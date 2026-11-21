package corpus;

import graphe.Jaccard;
import index.Index;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/*
 * Retrait des doublons a titres differents (deux editions du meme livre), que Filtres.Doublons
 * laisse passer (lance une fois, apres TelechargeCorpus).
 *
 *       java -cp backend/target/classes corpus.RetraitDoublons [--dossier data] [--seuil 0.13] [--appliquer]
 *
 * distance de Jaccard entre chaque paire de livres, sur tous leurs mots (sans empreinte)
 * -> paire a distance < seuil : doublon
 * -> on garde le plus petit id (en general la premiere mise en ligne)
 *
 * sans --appliquer : affiche seulement les livres a retirer, rien n'est modifie
 * avec --appliquer, pour chaque livre retire :
 * -> livres/ID.txt deplace dans doublons/ID.txt (rien n'est supprime)
 * -> sa ligne retiree de metadonnees.tsv
 * -> ajoute dans rejetes.tsv (TelechargeCorpus ne le retelecharge pas)
 *
 * ensuite : reconstruire l'index (ConstructionIndex) puis le graphe (ConstructionGraphe)
 */


public class RetraitDoublons {


    public static final double SEUIL_DEFAUT = 0.13;

    // livre retire, livre garde auquel il ressemble, distance entre les deux
    public record Retrait(int idRetire, int idGarde, double distance) {}

    public static void main(String[] args) throws IOException {

        Path dossier = Path.of("data");
        double seuil = SEUIL_DEFAUT;
        boolean appliquer = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {

                case "--dossier" -> dossier = Path.of(args[++i]);
                case "--seuil" -> seuil = Double.parseDouble(args[++i]);
                case "--appliquer" -> appliquer = true;
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    return;
                }
            }
        }

        Index index = Index.charger(dossier.resolve("index.bin"));
        Path fichierMeta = dossier.resolve("metadonnees.tsv");

        Metadonnees meta = Files.exists(fichierMeta) ? Metadonnees.charger(fichierMeta) : null;

        List<Retrait> retraits = chercher(index, seuil);
        System.out.printf(Locale.ROOT, "%d livres à retirer (distance de Jaccard < %.2f) :%n", retraits.size(), seuil);

        for (Retrait r : retraits) {
            System.out.printf(Locale.ROOT, "  %.4f  retiré %s  |  gardé %s%n",
                r.distance(), nom(r.idRetire(), meta), nom(r.idGarde(), meta));
        }

        if (!appliquer) {
            System.out.println("Rien n'est modifié (ajouter --appliquer).");
            return;
        }

        appliquer(dossier, retraits);
        System.out.println("Fait. Reconstruire maintenant l'index, puis le graphe.");
    }

    // livres a retirer, de la paire la plus proche a la moins proche (un livre n'apparait qu'une fois)
    public static List<Retrait> chercher(Index index, double seuil) {

        int n = index.nbLivres();
        float[] dist = new Jaccard(index, Jaccard.PART_MAX_DEFAUT).toutesLesDistances();

        // paires sous le seuil : {i, j} avec i < j (numeros dans l'index, ranges par id croissant)
        List<int[]> paires = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++, k++) {
                if (dist[k] < seuil) {
                    paires.add(new int[] {i, j, k});
                }
            }
        }
        paires.sort(Comparator.comparingDouble((int[] p) -> dist[p[2]]));

        List<Retrait> retraits = new ArrayList<>();
        Set<Integer> dejaRetires = new HashSet<>();

        for (int[] p : paires) {
            int petit = Math.min(index.idLivre(p[0]), index.idLivre(p[1]));
            int grand = Math.max(index.idLivre(p[0]), index.idLivre(p[1]));
            if (dejaRetires.add(grand)) {
                retraits.add(new Retrait(grand, petit, dist[p[2]]));
            }
        }
        return retraits;
    }

    public static void appliquer(Path dossier, List<Retrait> retraits) throws IOException {

        if (retraits.isEmpty()) {
            return;
        }

        Path doublons = dossier.resolve("doublons");
        Files.createDirectories(doublons);

        Set<Integer> retires = new HashSet<>();
        StringBuilder rejetes = new StringBuilder();
        for (Retrait r : retraits) {

            retires.add(r.idRetire());
            Path source = dossier.resolve("livres").resolve(r.idRetire() + ".txt");
            if (Files.exists(source)) {
                Files.move(source, doublons.resolve(r.idRetire() + ".txt"));
            }

            rejetes.append(String.format(Locale.ROOT, "%d\tdoublon de %d (distance de Jaccard %.4f)%n",
                r.idRetire(), r.idGarde(), r.distance()));
        }

        Path fichierMeta = dossier.resolve("metadonnees.tsv");
        if (Files.exists(fichierMeta)) {
            List<String> gardees = new ArrayList<>();
            for (String ligne : Files.readAllLines(fichierMeta, StandardCharsets.UTF_8)) {
                if (!retires.contains(id(ligne))) {
                    gardees.add(ligne);
                }
            }
            Files.write(fichierMeta, gardees, StandardCharsets.UTF_8);
        }

        Files.writeString(dossier.resolve("rejetes.tsv"), rejetes, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // id en debut de ligne (-1 si illisible)
    private static int id(String ligne) {
        int tab = ligne.indexOf('\t');

        try {
            return Integer.parseInt((tab < 0 ? ligne : ligne.substring(0, tab)).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String nom(int id, Metadonnees meta) {
        Metadonnees.Livre l = meta == null ? null : meta.livre(id);
        return l == null ? "livre " + id : id + " " + l.titre();
    }
}
