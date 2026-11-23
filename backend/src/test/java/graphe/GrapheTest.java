package graphe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import index.Index;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

// distance de Jaccard et PageRank sur des petits cas qu'on sait calculer a la main
class GrapheTest {

    @Test
    void distanceDeJaccard() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(10, "alpha beta gamma");
        c.ajouterLivre(11, "beta gamma delta");
        c.ajouterLivre(12, "omega sigma");
        Jaccard j = new Jaccard(c.construire(), 1.0); // 1.0 = on garde tous les mots

        // 2 mots en commun sur 4 au total -> 1 - 2/4 = 0.5
        assertEquals(0.5, j.distance(0, 1), 1e-9);
        // rien en commun -> distance max
        assertEquals(1.0, j.distance(0, 2), 1e-9);
    }

    // construit un graphe de n sommets : on met une distance de 0.5 sur les aretes voulues
    // et 1 partout ailleurs, puis on coupe a 0.75
    private Graphe graphe(int n, int[][] aretes) {
        float[] dist = new float[n * (n - 1) / 2];
        Arrays.fill(dist, 1f);
        for (int[] a : aretes) {
            dist[Jaccard.indicePaire(Math.min(a[0], a[1]), Math.max(a[0], a[1]), n)] = 0.5f;
        }
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = i;
        }
        return Graphe.depuisDistances(ids, dist, 0.75);
    }

    @Test
    void pageRankCentreDeLEtoile() {
        // etoile : le sommet 0 est relie a tous les autres
        double[] pr = PageRank.calculer(graphe(5, new int[][] {{0, 1}, {0, 2}, {0, 3}, {0, 4}})).scores();

        assertEquals(1.0, Arrays.stream(pr).sum(), 1e-9); // c'est une distribution
        for (int i = 1; i < 5; i++) {
            assertTrue(pr[0] > pr[i], "le centre doit etre le plus important");
        }
    }

    @Test
    void pageRankCycle() {
        // dans un cycle personne n'est plus central qu'un autre
        double[] pr = PageRank.calculer(graphe(4, new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 0}})).scores();
        for (double s : pr) {
            assertEquals(0.25, s, 1e-9);
        }
    }
}
