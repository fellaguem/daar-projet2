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

/*
 * Telecharge les livres depuis Gutenberg, une fois avant le serveur.
 * On prend les textes anglais, on nettoie l'entete et on garde ceux d'au moins 10 000 mots. */
public class TelechargeCorpus {

    static final String URL_CATALOGUE = "https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv";
    static final String MIROIR = "https://gutenberg.pglaf.org";
    static final String OFFICIEL = "https://www.gutenberg.org";
    static final int MINIMUM = 10_000;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20)).build();

    private final Path dossier;
    private final int cible;
    private final long pause;

    TelechargeCorpus(Path dossier, int cible, long pause) {
        this.dossier = dossier;
        this.cible = cible;
        this.pause = pause;
    }

    public static void main(String[] args) throws Exception {
        int cap = 1730; // on garde un peu de marge
        long delai = 1500;
        Path base = Path.of("data");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--max" -> cap = Integer.parseInt(args[++i]);
                case "--pause" -> delai = Long.parseLong(args[++i]);
                case "--dossier" -> base = Path.of(args[++i]);
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    return;
                }
            }
        }

        new TelechargeCorpus(base, cap, delai).lancer();
    }

    void lancer() throws Exception {
        Files.createDirectories(dossier.resolve("catalogue"));
        Files.createDirectories(dossier.resolve("livres"));

        Path cat = dossier.resolve("catalogue/pg_catalog.csv");
        if (!Files.exists(cat)) {
            System.out.println("Telechargement du catalogue officiel ...");
            telecharger(URL_CATALOGUE, cat);
        }

        List<Catalogue.Entree> liste;
        try (BufferedReader rd = Files.newBufferedReader(cat, StandardCharsets.UTF_8)) {
            liste = Catalogue.lire(rd, "en");
        }
        System.out.println("Livres de type Text en anglais dans le catalogue : " + liste.size());

        Path md = dossier.resolve("metadonnees.tsv");
        Path rej = dossier.resolve("rejetes.tsv");

        Set<Integer> vus = new HashSet<>();
        int ok = lireIds(md, vus);
        lireIds(rej, vus);
        System.out.println("Deja traites : " + vus.size() + " (et " + ok + " gardes)");

        Filtres.Doublons dbl = new Filtres.Doublons();
        enregistrerGardes(md, dbl); // pour eviter les memes livres

        int ess = 0;
        int hors = 0;
        int dup = 0;

        for (Catalogue.Entree e : liste) {
            if (ok >= cible) { // atteint
                break;
            }
            if (vus.contains(e.id()))
                continue;

            if (Filtres.titreExclu(e.titre())) {
                hors++;
                continue;
            }
            if (dbl.estDoublon(e.titre(), e.auteur())) {
                dup++; // deja le meme titre + auteur
                continue;
            }

            ess++;
            try {
                String raw = telechargerTexte(e.id());
                Thread.sleep(pause); // ne pas bombarder le serveur

                String txt = Nettoyage.retirerEnteteEtLicence(raw);
                if (txt == null) {
                    ecrire(rej, e.id() + "\t pas d'en-tete Gutenberg reconnu");
                    continue;
                }

                int nb = Tokeniseur.compter(txt);
                if (nb < MINIMUM) {
                    ecrire(rej, e.id() + "\ttrop court (" + nb + " mots)");
                    continue;
                }

                Files.writeString(dossier.resolve("livres/" + e.id() + ".txt"), txt, StandardCharsets.UTF_8);

                ecrire(md, e.id() + "\t" + e.titre() + "\t" + e.auteur()
                        + "\t" + e.langue() + "\t" + OFFICIEL + "/ebooks/" + e.id() + "\t" + nb);

                dbl.enregistrer(e.titre(), e.auteur());
                ok++;
                System.out.printf("[%d/%d] %d  %s (%d mots)%n", ok, cible, e.id(), e.titre(), nb);

            } catch (Absent ex) {
                ecrire(rej, e.id() + "\tfichier texte absent (404)");
                Thread.sleep(pause);
            } catch (IOException ex) {
                // on le retentera au prochain lancement
                System.err.println("Echec reseau pour " + e.id() + " : " + ex.getMessage());
                Thread.sleep(pause);
            }
        }

        System.out.println("Termine : " + ok + " livres gardes, " + ess + " telechargements essayes.");
        System.out.println("Ecartes avant telechargement : " + hors + " titres exclus, " + dup + " doublons.");
        if (ok < cible) {
            System.out.println("Cible non atteinte (" + cible + "). Relancer pour reessayer les echecs reseau.");
        }
    }

    // 404 / 403 / 410 : fichier indisponible
    static class Absent extends IOException {
        Absent(String message) {
            super(message);
        }
    }

    // miroir, puis officiel si le premier echoue
    private String telechargerTexte(int id) throws IOException, InterruptedException {
        String chemin = "/cache/epub/" + id + "/pg" + id + ".txt";
        try {
            return obtenir(MIROIR + chemin);
        } catch (IOException premiere) {
            return obtenir(OFFICIEL + chemin);
        }
    }

    // quelques essais si ca bloque (avec l'aide de Claude Code)
    private String obtenir(String url) throws IOException, InterruptedException {
        IOException erreur = null;
        for (int tour = 0; tour < 3; tour++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "daar-projet2-etudiant")
                    .timeout(Duration.ofSeconds(60))
                    .GET().build();

            try {
                HttpResponse<InputStream> rep = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream flux = rep.body()) {
                    int status = rep.statusCode();
                    if (status == 200) {
                        return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    if (status == 404 || status == 403 || status == 410) {
                        throw new Absent("HTTP " + status + " pour " + url);
                    }
                    erreur = new IOException("HTTP " + status + " pour " + url);
                }
            } catch (Absent ex) {
                throw ex;
            } catch (IOException ex) {
                erreur = ex;
            }
            Thread.sleep(5000L * (tour + 1));
        }
        throw erreur;
    }

    private void telecharger(String url, Path destination) throws IOException, InterruptedException {
        Files.writeString(destination, obtenir(url), StandardCharsets.UTF_8);
    }

    // ajoute une ligne, cree le fichier si besoin
    private static void ecrire(Path fichier, String ligne) throws IOException {
        Files.writeString(fichier, ligne + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // recharge les doublons deja gardes
    private static void enregistrerGardes(Path metadonnees, Filtres.Doublons doublons) throws IOException {
        if (!Files.exists(metadonnees))
            return;

        for (String ln : Files.readAllLines(metadonnees, StandardCharsets.UTF_8)) {
            String[] cc = ln.split("\t", -1);
            if (cc.length >= 3)
                doublons.enregistrer(cc[1], cc[2]);
        }
    }

    // ids dans la premiere colonne
    private static int lireIds(Path fichier, Set<Integer> ids) throws IOException {
        if (!Files.exists(fichier))
            return 0;

        int n = 0;
        for (String ln : Files.readAllLines(fichier, StandardCharsets.UTF_8)) {
            int pos = ln.indexOf('\t');
            if (pos > 0) {
                ids.add(Integer.parseInt(ln.substring(0, pos)));
                n++;
            }
        }
        return n;
    }
}
