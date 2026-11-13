package graphe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrapheTest {

    private static final int[] IDS = {10, 20, 30, 40};

    // paires dans l'ordre (0,1) (0,2) (0,3) (1,2) (1,3) (2,3)
    private static final float[] DIST = {0.1f, 0.5f, 0.9f, 0.2f, 0.95f, 0.3f};

    @Test
    void aretesStrictementSousLeSeuil() {
        Graphe g = Graphe.depuisDistances(IDS, DIST, 0.4);
        // arêtes : 0-1 (0,1), 1-2 (0,2), 2-3 (0,3) : un chemin
        assertEquals(3, g.nbAretes());
        assertArrayEquals(new int[] {1, 2, 2, 1}, new int[] {g.degre(0), g.degre(1), g.degre(2), g.degre(3)});

        // seuil égal à une distance : l'arête n'est pas prise (strictement sous le seuil)
        assertEquals(2, Graphe.depuisDistances(IDS, DIST, 0.3).nbAretes());
    }

    @Test
    void grapheSymetriqueSansBoucleEtVoisinsTriesParDistance() {
        Graphe g = Graphe.depuisDistances(IDS, DIST, 0.6);
        for (int i = 0; i < g.nbSommets(); i++) {
            for (int p = g.debut(i); p < g.fin(i); p++) {
                int v = g.voisin(p);
                assertTrue(v != i, "pas de boucle");
                boolean retour = false;
                for (int q = g.debut(v); q < g.fin(v); q++) {
                    retour |= g.voisin(q) == i && g.distance(q) == g.distance(p);
                }
                assertTrue(retour, "arête " + i + "-" + v + " dans les deux sens");
                if (p > g.debut(i)) {
                    assertTrue(g.distance(p - 1) <= g.distance(p), "voisins triés");
                }
            }
        }
        // voisins du sommet 1 : 0 (0,1) puis 2 (0,2)
        assertEquals(0, g.voisin(g.debut(1)));
        assertEquals(2, g.voisin(g.debut(1) + 1));
        // voisins du sommet 0 : 1 (0,1) puis 2 (0,5)
        assertEquals(1, g.voisin(g.debut(0)));
        assertEquals(0.5f, g.distance(g.debut(0) + 1));
    }

    @Test
    void isolesEtComposantes() {
        Graphe g = Graphe.depuisDistances(IDS, DIST, 0.15); // seule l'arête 0-1
        assertEquals(1, g.nbAretes());
        assertEquals(2, g.nbIsoles());
        assertArrayEquals(new int[] {2, 1, 1}, g.composantes());

        Graphe chemin = Graphe.depuisDistances(IDS, DIST, 0.4);
        assertArrayEquals(new int[] {4}, chemin.composantes());
        assertEquals(0, chemin.nbIsoles());
    }

    @Test
    void seuilPourUnDegreMoyen() {
        // degré moyen 1,5 sur 4 livres : 3 arêtes -> les 3 plus petites distances (0,1 0,2 0,3)
        double seuil = Graphe.seuilPourDegreMoyen(DIST, 4, 1.5);
        assertEquals(0.5f, (float) seuil);
        assertEquals(3, Graphe.depuisDistances(IDS, DIST, seuil).nbAretes());

        assertEquals(0, Graphe.depuisDistances(IDS, DIST, Graphe.seuilPourDegreMoyen(DIST, 4, 0)).nbAretes());
        assertEquals(6, Graphe.depuisDistances(IDS, DIST, Graphe.seuilPourDegreMoyen(DIST, 4, 100)).nbAretes());
    }

    @Test
    void sauvegardePuisChargement(@TempDir Path dossier) throws IOException {
        Graphe avant = Graphe.depuisDistances(IDS, DIST, 0.6);
        Path fichier = dossier.resolve("graphe.bin");
        avant.sauver(fichier);
        Graphe apres = Graphe.charger(fichier);

        assertEquals(avant.nbSommets(), apres.nbSommets());
        assertEquals(avant.nbAretes(), apres.nbAretes());
        assertEquals(avant.seuil(), apres.seuil());
        for (int i = 0; i < avant.nbSommets(); i++) {
            assertEquals(avant.idLivre(i), apres.idLivre(i));
            assertEquals(avant.debut(i), apres.debut(i));
            assertEquals(avant.fin(i), apres.fin(i));
        }
        for (int p = 0; p < 2 * avant.nbAretes(); p++) {
            assertEquals(avant.voisin(p), apres.voisin(p));
            assertEquals(avant.distance(p), apres.distance(p));
        }

        Path autre = dossier.resolve("autre.bin");
        Files.write(autre, new byte[] {0, 0, 0, 0, 0, 0, 0, 0});
        assertThrows(IOException.class, () -> Graphe.charger(autre));
    }

    @Test
    void listeDAretesLisible(@TempDir Path dossier) throws IOException {
        Path csv = dossier.resolve("aretes.csv");
        Graphe.depuisDistances(IDS, DIST, 0.4).ecrireAretesCsv(csv);
        assertEquals(List.of("id1,id2,distance", "10,20,0.100000", "20,30,0.200000", "30,40,0.300000"),
            Files.readAllLines(csv, StandardCharsets.UTF_8));
    }

    @Test
    void grapheVideEtUnSeulLivre() {
        Graphe vide = Graphe.depuisDistances(new int[0], new float[0], 0.5);
        assertEquals(0, vide.nbSommets());
        assertEquals(0, vide.composantes().length);

        Graphe seul = Graphe.depuisDistances(new int[] {7}, new float[0], 0.5);
        assertEquals(1, seul.nbIsoles());
    }
}
