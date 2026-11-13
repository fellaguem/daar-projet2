package graphe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PageRankTest {

    // graphe à n sommets avec les arêtes données (distance 0,5 pour une arête, 1 sinon, seuil 0,75)
    private static Graphe graphe(int n, int[][] aretes) {
        float[] dist = new float[n * (n - 1) / 2];
        Arrays.fill(dist, 1f);
        for (int[] a : aretes) {
            int i = Math.min(a[0], a[1]);
            int j = Math.max(a[0], a[1]);
            dist[Jaccard.indicePaire(i, j, n)] = 0.5f;
        }
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = 100 + i;
        }
        return Graphe.depuisDistances(ids, dist, 0.75);
    }

    private static double somme(double[] x) {
        return Arrays.stream(x).sum();
    }

    @Test
    void cycleDonneDesScoresEgaux() {
        double[] pr = PageRank.calculer(graphe(4, new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 0}})).scores();
        for (double s : pr) {
            assertEquals(0.25, s, 1e-9);
        }
    }

    @Test
    void centreDeLEtoileLePlusHaut() {
        double[] pr = PageRank.calculer(graphe(5, new int[][] {{0, 1}, {0, 2}, {0, 3}, {0, 4}})).scores();
        assertEquals(1.0, somme(pr), 1e-9);
        for (int f = 1; f < 5; f++) {
            assertTrue(pr[0] > pr[f]);
            assertEquals(pr[1], pr[f], 1e-12); // les feuilles sont équivalentes
        }
    }

    @Test
    void livreIsoleRedistribueSonScore() {
        // arête 0-1 et sommet 2 isolé. Par symétrie PR(0) = PR(1) = x, PR(2) = y.
        // Le sommet 2 ne reçoit que la part commune : y = (1-a)/3 + a*y/3, donc y = (1-a) / (3-a)
        double a = 0.85;
        double[] pr = PageRank.calculer(graphe(3, new int[][] {{0, 1}}), a, 1e-14, 1000).scores();
        double y = (1 - a) / (3 - a);
        assertEquals(y, pr[2], 1e-10);
        assertEquals((1 - y) / 2, pr[0], 1e-10);
        assertEquals(pr[0], pr[1], 1e-12);
        assertEquals(1.0, somme(pr), 1e-12);
    }

    @Test
    void sansAucuneAreteScoresUniformes() {
        double[] pr = PageRank.calculer(graphe(4, new int[0][])).scores();
        for (double s : pr) {
            assertEquals(0.25, s, 1e-12);
        }
    }

    @Test
    void grapheVide() {
        PageRank.Resultat r = PageRank.calculer(graphe(0, new int[0][]));
        assertEquals(0, r.scores().length);
    }

    @Test
    void convergeEtRespecteLaTolerance() {
        PageRank.Resultat r = PageRank.calculer(graphe(5, new int[][] {{0, 1}, {1, 2}, {2, 0}, {3, 4}}));
        assertTrue(r.iterations() < PageRank.ITERATIONS_MAX_DEFAUT);
        assertTrue(r.ecart() < PageRank.TOLERANCE_DEFAUT);
    }

    // calcul de référence avec une matrice pleine : G[i][j] = probabilité d'aller de j vers i
    private static double[] pageRankMatriciel(Graphe g, double a) {
        int n = g.nbSommets();
        double[][] m = new double[n][n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                m[i][j] = (1 - a) / n;
            }
            if (g.degre(j) == 0) {
                for (int i = 0; i < n; i++) {
                    m[i][j] += a / n;
                }
            } else {
                for (int p = g.debut(j); p < g.fin(j); p++) {
                    m[g.voisin(p)][j] += a / g.degre(j);
                }
            }
        }
        double[] x = new double[n];
        Arrays.fill(x, 1.0 / n);
        for (int it = 0; it < 2000; it++) {
            double[] y = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    y[i] += m[i][j] * x[j];
                }
            }
            x = y;
        }
        return x;
    }

    @Test
    void memeResultatQueLeCalculMatricielSurDesGraphesAleatoires() {
        Random hasard = new Random(7);
        for (int essai = 0; essai < 30; essai++) {
            int n = 2 + hasard.nextInt(12);
            float[] dist = new float[n * (n - 1) / 2];
            for (int k = 0; k < dist.length; k++) {
                dist[k] = hasard.nextFloat();
            }
            int[] ids = new int[n];
            Graphe g = Graphe.depuisDistances(ids, dist, 0.3); // graphe peu dense : souvent des isolés
            double[] attendu = pageRankMatriciel(g, 0.85);
            double[] obtenu = PageRank.calculer(g, 0.85, 1e-14, 2000).scores();
            assertArrayEquals(attendu, obtenu, 1e-10, "essai " + essai);
        }
    }

    @Test
    void sauvegardePuisChargement(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve("pagerank.bin");
        PageRank.sauver(fichier, new int[] {5, 9}, new double[] {0.25, 0.75});
        PageRank.Scores s = PageRank.charger(fichier);
        assertArrayEquals(new int[] {5, 9}, s.idsLivres());
        assertArrayEquals(new double[] {0.25, 0.75}, s.scores());
    }
}
