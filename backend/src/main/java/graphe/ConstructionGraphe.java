package graphe;

import corpus.Metadonnees;
import index.Index;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.PriorityQueue;

// Construit le graphe de Jaccard et le PageRank (programme lancé une fois, hors ligne du serveur).
//
//   java -cp backend/target/classes graphe.ConstructionGraphe --dossier data
//        [--index DOSSIER/index.bin] [--metadonnees DOSSIER/metadonnees.tsv]
//        [--part-max 0.5] [--mots-par-livre 250] [--degre-moyen 10 | --seuil 0.8] [--amortissement 0.85]
//
// --mots-par-livre K : chaque livre est représenté par ses K mots non vides les plus fréquents
// (0 = tous ses mots). Voir Jaccard et docs/notes.md (biais de longueur).
//
// Fichiers écrits dans le dossier :
//   graphe.bin, pagerank.bin                  lus par le serveur
//   aretes.csv, pagerank.csv                  liste d'arêtes et scores, lisibles (rapport)
//   mesures/distances_histogramme.csv         histogramme des distances (classes de 0,05)
public class ConstructionGraphe {

    // choisi sur le vrai corpus le 13/11/2026 (docs/notes.md) : 250 mots les plus fréquents par livre
    public static final int MOTS_PAR_LIVRE_DEFAUT = 250;

    public static void main(String[] args) throws IOException {
        Path dossier = Path.of("data");
        Path fichierIndex = null;
        Path fichierMeta = null;
        double partMax = Jaccard.PART_MAX_DEFAUT;
        int motsParLivre = MOTS_PAR_LIVRE_DEFAUT;
        double degreMoyen = 10;
        Double seuilFixe = null;
        double amortissement = PageRank.AMORTISSEMENT_DEFAUT;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dossier" -> dossier = Path.of(args[++i]);
                case "--index" -> fichierIndex = Path.of(args[++i]);
                case "--metadonnees" -> fichierMeta = Path.of(args[++i]);
                case "--part-max" -> partMax = Double.parseDouble(args[++i]);
                case "--mots-par-livre" -> motsParLivre = Integer.parseInt(args[++i]);
                case "--degre-moyen" -> degreMoyen = Double.parseDouble(args[++i]);
                case "--seuil" -> seuilFixe = Double.parseDouble(args[++i]);
                case "--amortissement" -> amortissement = Double.parseDouble(args[++i]);
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    return;
                }
            }
        }
        if (fichierIndex == null) {
            fichierIndex = dossier.resolve("index.bin");
        }
        if (fichierMeta == null) {
            fichierMeta = dossier.resolve("metadonnees.tsv");
        }
        Metadonnees meta = Files.exists(fichierMeta) ? Metadonnees.charger(fichierMeta) : null;

        // ---------- distances ----------
        Index index = Index.charger(fichierIndex);
        int n = index.nbLivres();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = index.idLivre(i);
        }

        long t0 = System.nanoTime();
        Jaccard jaccard = new Jaccard(index, partMax, motsParLivre);
        long t1 = System.nanoTime();
        float[] dist = jaccard.toutesLesDistances();
        long t2 = System.nanoTime();

        System.out.printf(Locale.ROOT, "Livres : %d, paires : %d%n", n, dist.length);
        int tailleMin = Integer.MAX_VALUE;
        int tailleMax = 0;
        int incomplets = 0;
        for (int i = 0; i < n; i++) {
            tailleMin = Math.min(tailleMin, jaccard.taille(i));
            tailleMax = Math.max(tailleMax, jaccard.taille(i));
            if (motsParLivre > 0 && jaccard.taille(i) < motsParLivre) {
                incomplets++;
            }
        }
        System.out.printf(Locale.ROOT, "Ensembles : %s, tailles de %d à %d mots%s%n",
            motsParLivre > 0 ? "les " + motsParLivre + " mots les plus fréquents de chaque livre" : "tous les mots",
            n == 0 ? 0 : tailleMin, tailleMax,
            incomplets > 0 ? " (" + incomplets + " livres ont moins de " + motsParLivre + " mots)" : "");
        System.out.printf(Locale.ROOT, "Mots : %d partagés (un bit chacun), %d dans l'ensemble d'un seul livre, "
            + "%d mots vides retirés (dans plus de %.0f %% des livres)%n",
            jaccard.nbMotsPartages(), jaccard.nbMotsUniques(), jaccard.nbMotsVides(), partMax * 100);
        System.out.printf(Locale.ROOT, "Vecteurs de bits : %.1f Mo, construits en %d ms%n",
            jaccard.tailleBits() / 1e6, (t1 - t0) / 1_000_000);
        System.out.printf(Locale.ROOT, "Distances : %d ms (%d coeurs)%n",
            (t2 - t1) / 1_000_000, Runtime.getRuntime().availableProcessors());

        Files.createDirectories(dossier.resolve("mesures"));
        afficherDistribution(dist);
        ecrireHistogramme(dist, dossier.resolve("mesures/distances_histogramme.csv"));
        afficherPairesProches(dist, n, ids, meta, 10);

        // ---------- graphe ----------
        double seuil = seuilFixe != null ? seuilFixe : Graphe.seuilPourDegreMoyen(dist, n, degreMoyen);
        long t3 = System.nanoTime();
        Graphe graphe = Graphe.depuisDistances(ids, dist, seuil);
        long t4 = System.nanoTime();
        int[] composantes = graphe.composantes();
        System.out.printf(Locale.ROOT, "%nGraphe : seuil %.4f%s, %d arêtes, degré moyen %.2f, %d livres isolés, "
            + "%d composantes (la plus grande : %d livres), construit en %d ms%n",
            seuil, seuilFixe != null ? " (fixé)" : " (pour un degré moyen de " + degreMoyen + ")",
            graphe.nbAretes(), n == 0 ? 0 : 2.0 * graphe.nbAretes() / n, graphe.nbIsoles(),
            composantes.length, composantes.length == 0 ? 0 : composantes[0], (t4 - t3) / 1_000_000);

        // ---------- PageRank ----------
        long t5 = System.nanoTime();
        PageRank.Resultat pr = PageRank.calculer(graphe, amortissement,
            PageRank.TOLERANCE_DEFAUT, PageRank.ITERATIONS_MAX_DEFAUT);
        long t6 = System.nanoTime();
        System.out.printf(Locale.ROOT, "PageRank : %d itérations, écart final %.2e, %d ms%n",
            pr.iterations(), pr.ecart(), (t6 - t5) / 1_000_000);
        afficherMeilleurs(graphe, pr.scores(), meta, 10);

        // corrélation avec le degré : sur un graphe non orienté, PageRank est souvent proche du degré
        // et avec la longueur du livre : mesure le biais de longueur du graphe
        double[] degres = new double[n];
        double[] longueurs = new double[n];
        for (int i = 0; i < n; i++) {
            degres[i] = graphe.degre(i);
            longueurs[i] = index.longueurLivre(i);
        }
        System.out.printf(Locale.ROOT, "Corrélation (Pearson) entre PageRank et degré : %.4f%n",
            pearson(pr.scores(), degres));
        System.out.printf(Locale.ROOT, "Corrélation (Pearson) entre PageRank et longueur du livre : %.4f%n",
            pearson(pr.scores(), longueurs));
        afficherIsolesParLongueur(graphe, longueurs);

        // ---------- fichiers ----------
        graphe.sauver(dossier.resolve("graphe.bin"));
        graphe.ecrireAretesCsv(dossier.resolve("aretes.csv"));
        PageRank.sauver(dossier.resolve("pagerank.bin"), ids, pr.scores());
        ecrirePageRankCsv(graphe, pr.scores(), meta, dossier.resolve("pagerank.csv"));
        System.out.println("Fichiers écrits dans " + dossier);
    }

    // quelques repères de la distribution des distances
    private static void afficherDistribution(float[] dist) {
        if (dist.length == 0) {
            return;
        }
        float[] tri = dist.clone();
        Arrays.sort(tri);
        double somme = 0;
        for (float d : tri) {
            somme += d;
        }
        System.out.printf(Locale.ROOT, "Distances : min %.4f | 1 %% %.4f | 5 %% %.4f | 25 %% %.4f | médiane %.4f "
            + "| 75 %% %.4f | 95 %% %.4f | max %.4f | moyenne %.4f%n",
            tri[0], quantile(tri, 0.01), quantile(tri, 0.05), quantile(tri, 0.25), quantile(tri, 0.5),
            quantile(tri, 0.75), quantile(tri, 0.95), tri[tri.length - 1], somme / tri.length);
    }

    private static float quantile(float[] tri, double q) {
        return tri[(int) Math.min(tri.length - 1, Math.floor(q * tri.length))];
    }

    // 20 classes de largeur 0,05 entre 0 et 1 (la distance 1 va dans la dernière)
    private static void ecrireHistogramme(float[] dist, Path fichier) throws IOException {
        long[] classes = new long[20];
        for (float d : dist) {
            classes[Math.min(19, (int) (d * 20))]++;
        }
        try (PrintStream out = new PrintStream(Files.newOutputStream(fichier), false, StandardCharsets.UTF_8)) {
            out.println("debut,fin,nb_paires");
            for (int c = 0; c < 20; c++) {
                out.printf(Locale.ROOT, "%.2f,%.2f,%d%n", c * 0.05, (c + 1) * 0.05, classes[c]);
            }
        }
    }

    // les paires les plus proches : doublons possibles que le filtre par titre a manqués
    private static void afficherPairesProches(float[] dist, int n, int[] ids, Metadonnees meta, int combien) {
        // tas des "combien" plus petites distances (le plus grand en tête, pour le remplacer)
        PriorityQueue<long[]> tas = new PriorityQueue<>(
            Comparator.comparingDouble((long[] x) -> dist[(int) x[2]]).reversed());
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++, k++) {
                if (tas.size() < combien) {
                    tas.add(new long[] {i, j, k});
                } else if (dist[k] < dist[(int) tas.peek()[2]]) {
                    tas.poll();
                    tas.add(new long[] {i, j, k});
                }
            }
        }
        long[][] paires = tas.toArray(new long[0][]);
        Arrays.sort(paires, Comparator.comparingDouble((long[] x) -> dist[(int) x[2]]));
        System.out.println("Paires les plus proches :");
        for (long[] p : paires) {
            System.out.printf(Locale.ROOT, "  %.4f  %s  |  %s%n", dist[(int) p[2]],
                nom(ids[(int) p[0]], meta), nom(ids[(int) p[1]], meta));
        }
    }

    private static void afficherMeilleurs(Graphe g, double[] scores, Metadonnees meta, int combien) {
        Integer[] ordre = new Integer[g.nbSommets()];
        for (int i = 0; i < ordre.length; i++) {
            ordre[i] = i;
        }
        Arrays.sort(ordre, (a, b) -> Double.compare(scores[b], scores[a]));
        System.out.println("PageRank le plus élevé :");
        for (int r = 0; r < Math.min(combien, ordre.length); r++) {
            int i = ordre[r];
            System.out.printf(Locale.ROOT, "  %.6f  degré %3d  %s%n", scores[i], g.degre(i), nom(g.idLivre(i), meta));
        }
    }

    private static void ecrirePageRankCsv(Graphe g, double[] scores, Metadonnees meta, Path fichier)
            throws IOException {
        try (PrintStream out = new PrintStream(Files.newOutputStream(fichier), false, StandardCharsets.UTF_8)) {
            out.println("id,degre,pagerank,titre");
            for (int i = 0; i < g.nbSommets(); i++) {
                Metadonnees.Livre l = meta == null ? null : meta.livre(g.idLivre(i));
                String titre = l == null ? "" : l.titre().replace("\"", "\"\"");
                out.printf(Locale.ROOT, "%d,%d,%.10f,\"%s\"%n", g.idLivre(i), g.degre(i), scores[i], titre);
            }
        }
    }

    // part de livres isolés et degré moyen par tranche de longueur (en mots indexés)
    private static void afficherIsolesParLongueur(Graphe g, double[] longueurs) {
        int[] bornes = {0, 25_000, 50_000, 100_000, 200_000, Integer.MAX_VALUE};
        System.out.println("Par longueur (mots indexés) :");
        for (int t = 0; t + 1 < bornes.length; t++) {
            int nb = 0;
            int isoles = 0;
            long degres = 0;
            for (int i = 0; i < g.nbSommets(); i++) {
                if (longueurs[i] >= bornes[t] && longueurs[i] < bornes[t + 1]) {
                    nb++;
                    degres += g.degre(i);
                    if (g.degre(i) == 0) {
                        isoles++;
                    }
                }
            }
            if (nb > 0) {
                System.out.printf(Locale.ROOT, "  %7d à %-10s : %4d livres, %5.1f %% isolés, degré moyen %.1f%n",
                    bornes[t], bornes[t + 1] == Integer.MAX_VALUE ? "plus" : String.valueOf(bornes[t + 1]),
                    nb, 100.0 * isoles / nb, (double) degres / nb);
            }
        }
    }

    private static String nom(int id, Metadonnees meta) {
        Metadonnees.Livre l = meta == null ? null : meta.livre(id);
        if (l == null) {
            return "livre " + id;
        }
        String titre = l.titre().length() > 50 ? l.titre().substring(0, 50) + "…" : l.titre();
        return id + " " + titre;
    }

    static double pearson(double[] x, double[] y) {
        int n = x.length;
        if (n < 2) {
            return Double.NaN;
        }
        double mx = 0;
        double my = 0;
        for (int i = 0; i < n; i++) {
            mx += x[i];
            my += y[i];
        }
        mx /= n;
        my /= n;
        double sxy = 0;
        double sxx = 0;
        double syy = 0;
        for (int i = 0; i < n; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
        }
        return sxy / Math.sqrt(sxx * syy);
    }
}
