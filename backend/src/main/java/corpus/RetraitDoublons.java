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

// Retire les doublons que le filtre par titre (Filtres.Doublons) a laissé passer : deux éditions du
// même livre sous des titres différents. On les repère par la distance de Jaccard calculée sur tous
// les mots des livres (sans empreinte). Pour chaque paire à distance < seuil, on garde le plus petit
// identifiant (en général la première mise en ligne sur Gutenberg).
//
//   java -cp backend/target/classes corpus.RetraitDoublons [--dossier data] [--seuil 0.13] [--appliquer]
//
// Sans --appliquer, le programme affiche seulement les livres qu'il retirerait. Avec --appliquer,
// pour chaque livre retiré :
//   - livres/ID.txt est déplacé dans doublons/ID.txt (rien n'est supprimé) ;
//   - sa ligne de metadonnees.tsv est retirée ;
//   - rejetes.tsv reçoit « ID  doublon de X (distance de Jaccard d) » : un nouveau lancement de
//     TelechargeCorpus ne le télécharge pas à nouveau.
// Il faut ensuite reconstruire l'index (ConstructionIndex), puis le graphe (ConstructionGraphe).
public class RetraitDoublons {

    public static final double SEUIL_DEFAUT = 0.13;

    // livre retiré, livre gardé auquel il ressemble, distance entre les deux
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

    // Les livres à retirer, de la paire la plus proche à la moins proche. Un livre n'apparaît qu'une fois.
    public static List<Retrait> chercher(Index index, double seuil) {
        int n = index.nbLivres();
        float[] dist = new Jaccard(index, Jaccard.PART_MAX_DEFAUT).toutesLesDistances();

        // paires sous le seuil : {i, j} avec i < j (numéros dans l'index, rangés par id croissant)
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

    // identifiant en tête de ligne (-1 si illisible)
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
