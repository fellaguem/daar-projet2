package corpus;

import index.Tokeniseur;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Télécharge le corpus depuis Project Gutenberg (programme lancé une fois, hors ligne du serveur).
//
//   java -cp backend/target/classes corpus.TelechargeCorpus [--max 1750] [--pause 1500] [--dossier data]
//
// Étapes : catalogue officiel (pg_catalog.csv) -> livres de type Text en anglais ->
// téléchargement de chaque livre depuis un miroir (le site officiel en secours),
// avec une pause entre deux requêtes -> retrait de l'en-tête et de la licence ->
// on garde le livre s'il a au moins 10 000 mots.
//
// On peut interrompre et relancer : les livres déjà traités (gardés ou écartés) sont sautés.
//
// Fichiers produits dans le dossier :
//   catalogue/pg_catalog.csv   copie du catalogue officiel
//   livres/ID.txt              texte nettoyé
//   metadonnees.tsv            id, titre, auteur, langue, urlSource, nbMots (un livre par ligne)
//   rejetes.tsv                id, raison (livres écartés)
public class TelechargeCorpus {

    static final String URL_CATALOGUE = "https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv";
    static final String MIROIR = "https://gutenberg.pglaf.org";
    static final String OFFICIEL = "https://www.gutenberg.org";
    static final int MOTS_MINIMUM = 10_000;

    private final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    private final Path dossier;
    private final int cible;
    private final long pauseMs;

    TelechargeCorpus(Path dossier, int cible, long pauseMs) {
        this.dossier = dossier;
        this.cible = cible;
        this.pauseMs = pauseMs;
    }

    public static void main(String[] args) throws Exception {
        int cible = 1750; // un peu plus que les 1 664 demandés, pour avoir de la marge
        long pause = 1500;
        Path dossier = Path.of("data");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--max" -> cible = Integer.parseInt(args[++i]);
                case "--pause" -> pause = Long.parseLong(args[++i]);
                case "--dossier" -> dossier = Path.of(args[++i]);
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    return;
                }
            }
        }
        new TelechargeCorpus(dossier, cible, pause).lancer();
    }

    void lancer() throws Exception {
        Files.createDirectories(dossier.resolve("catalogue"));
        Files.createDirectories(dossier.resolve("livres"));

        Path fichierCatalogue = dossier.resolve("catalogue/pg_catalog.csv");
        if (!Files.exists(fichierCatalogue)) {
            System.out.println("Téléchargement du catalogue officiel...");
            telecharger(URL_CATALOGUE, fichierCatalogue);
        }

        List<Catalogue.Entree> candidats;
        try (BufferedReader lecteur = Files.newBufferedReader(fichierCatalogue, StandardCharsets.UTF_8)) {
            candidats = Catalogue.lire(lecteur, "en");
        }
        System.out.println("Livres de type Text en anglais dans le catalogue : " + candidats.size());

        Path metadonnees = dossier.resolve("metadonnees.tsv");
        Path rejetes = dossier.resolve("rejetes.tsv");
        Set<Integer> dejaTraites = new HashSet<>();
        int gardes = lireIds(metadonnees, dejaTraites);
        lireIds(rejetes, dejaTraites);
        System.out.println("Déjà traités : " + dejaTraites.size() + " (dont " + gardes + " gardés)");

        // les livres déjà gardés servent à repérer les doublons (aussi après une reprise)
        Filtres.Doublons doublons = new Filtres.Doublons();
        enregistrerGardes(metadonnees, doublons);

        int tentatives = 0;
        int exclus = 0;
        int nbDoublons = 0;
        for (Catalogue.Entree e : candidats) {
            if (gardes >= cible) {
                break;
            }
            if (dejaTraites.contains(e.id())) {
                continue;
            }

            // Les filtres passent avant le téléchargement et avant le comptage de --max :
            // un livre écarté ne compte pas parmi les livres gardés.
            if (Filtres.titreExclu(e.titre())) {
                exclus++;
                continue;
            }
            if (doublons.estDoublon(e.titre(), e.auteur())) {
                nbDoublons++;
                continue;
            }

            tentatives++;
            try {
                String brut = telechargerTexte(e.id());
                Thread.sleep(pauseMs); // pause entre deux téléchargements

                String texte = Nettoyage.retirerEnteteEtLicence(brut);
                if (texte == null) {
                    ecrire(rejetes, e.id() + "\tpas d'en-tête Gutenberg reconnu");
                    continue;
                }
                int nbMots = Tokeniseur.compter(texte);
                if (nbMots < MOTS_MINIMUM) {
                    ecrire(rejetes, e.id() + "\ttrop court (" + nbMots + " mots)");
                    continue;
                }

                Files.writeString(dossier.resolve("livres/" + e.id() + ".txt"), texte, StandardCharsets.UTF_8);
                ecrire(metadonnees, e.id() + "\t" + e.titre() + "\t" + e.auteur() + "\t" + e.langue()
                    + "\t" + OFFICIEL + "/ebooks/" + e.id() + "\t" + nbMots);
                doublons.enregistrer(e.titre(), e.auteur());
                gardes++;
                System.out.printf("[%d/%d] %d  %s (%d mots)%n", gardes, cible, e.id(), e.titre(), nbMots);

            } catch (Absent ex) {
                ecrire(rejetes, e.id() + "\tfichier texte absent (404)");
                Thread.sleep(pauseMs);
            } catch (IOException ex) {
                // on n'écrit pas dans rejetes : une erreur réseau doit être réessayée au prochain lancement
                System.err.println("Échec réseau pour " + e.id() + " : " + ex.getMessage());
                Thread.sleep(pauseMs);
            }
        }

        System.out.println("Terminé : " + gardes + " livres gardés, " + tentatives + " téléchargements essayés.");
        System.out.println("Écartés avant téléchargement : " + exclus + " titres exclus, " + nbDoublons + " doublons.");
        if (gardes < cible) {
            System.out.println("Cible non atteinte (" + cible + "). Relancer pour réessayer les échecs réseau.");
        }
    }

    // le livre n'existe pas sur le serveur (404) : inutile de réessayer
    static class Absent extends IOException {
        Absent(String message) {
            super(message);
        }
    }

    // texte brut d'un livre : le miroir d'abord, le site officiel en secours.
    // Si les deux répondent "absent", le livre est écarté (Absent).
    private String telechargerTexte(int id) throws IOException, InterruptedException {
        String chemin = "/cache/epub/" + id + "/pg" + id + ".txt";
        try {
            return obtenir(MIROIR + chemin);
        } catch (IOException premiere) {
            return obtenir(OFFICIEL + chemin);
        }
    }

    // GET avec quelques essais si le serveur est occupé (429, 5xx)
    private String obtenir(String url) throws IOException, InterruptedException {
        IOException derniere = null;
        for (int essai = 0; essai < 3; essai++) {
            HttpRequest requete = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "daar-projet2-etudiant")
                .timeout(Duration.ofSeconds(60))
                .GET().build();
            try {
                HttpResponse<InputStream> rep = client.send(requete, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream in = rep.body()) {
                    int code = rep.statusCode();
                    if (code == 200) {
                        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    if (code == 404 || code == 403 || code == 410) {
                        throw new Absent("HTTP " + code + " pour " + url);
                    }
                    derniere = new IOException("HTTP " + code + " pour " + url);
                }
            } catch (Absent e) {
                throw e; // inutile de réessayer
            } catch (IOException e) {
                derniere = e;
            }
            Thread.sleep(5000L * (essai + 1));
        }
        throw derniere;
    }

    private void telecharger(String url, Path destination) throws IOException, InterruptedException {
        Files.writeString(destination, obtenir(url), StandardCharsets.UTF_8);
    }

    // ajoute une ligne à la fin du fichier (le crée au besoin)
    private static void ecrire(Path fichier, String ligne) throws IOException {
        Files.writeString(fichier, ligne + "\n", StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // relit le titre (colonne 2) et l'auteur (colonne 3) des livres déjà gardés
    private static void enregistrerGardes(Path metadonnees, Filtres.Doublons doublons) throws IOException {
        if (!Files.exists(metadonnees)) {
            return;
        }
        for (String ligne : Files.readAllLines(metadonnees, StandardCharsets.UTF_8)) {
            String[] colonnes = ligne.split("\t", -1);
            if (colonnes.length >= 3) {
                doublons.enregistrer(colonnes[1], colonnes[2]);
            }
        }
    }

    // lit la première colonne (l'id) de chaque ligne ; renvoie le nombre de lignes lues
    private static int lireIds(Path fichier, Set<Integer> ids) throws IOException {
        if (!Files.exists(fichier)) {
            return 0;
        }
        int n = 0;
        for (String ligne : Files.readAllLines(fichier, StandardCharsets.UTF_8)) {
            int tab = ligne.indexOf('\t');
            if (tab > 0) {
                ids.add(Integer.parseInt(ligne.substring(0, tab)));
                n++;
            }
        }
        return n;
    }
}
