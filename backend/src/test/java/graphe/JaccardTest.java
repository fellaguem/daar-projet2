package graphe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import index.Index;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import projet1.Fixtures;

class JaccardTest {

    // livre 0 : alpha beta gamma ; 1 : beta gamma delta ; 2 : alpha beta gamma (comme 0) ; 3 : omega sigma
    // nombre de livres par mot : alpha 2, beta 3, gamma 3, delta 1, omega 1, sigma 1
    private static Index petitIndex() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(10, "alpha beta gamma");
        c.ajouterLivre(11, "beta gamma delta");
        c.ajouterLivre(12, "alpha beta gamma");
        c.ajouterLivre(13, "omega sigma");
        return c.construire();
    }

    @Test
    void distancesConnuesSansMotsVides() {
        Jaccard j = new Jaccard(petitIndex(), 1.0); // 1.0 : aucun mot vide retiré

        // {alpha,beta,gamma} et {beta,gamma,delta} : intersection 2, union 4 ("delta" compte dans l'union)
        assertEquals(0.5, j.distance(0, 1), 1e-12);
        assertEquals(0.0, j.distance(0, 2), 1e-12); // mêmes mots
        assertEquals(1.0, j.distance(0, 3), 1e-12); // aucun mot commun
        assertEquals(j.distance(1, 0), j.distance(0, 1), 1e-12); // symétrique
    }

    @Test
    void motsPresentsDansUnSeulLivreComptesDansLaTaille() {
        Jaccard j = new Jaccard(petitIndex(), 1.0);
        assertEquals(3, j.nbMotsUniques());   // delta, omega, sigma : pas de bit
        assertEquals(3, j.nbMotsPartages());  // alpha, beta, gamma : un bit chacun
        assertEquals(0, j.nbMotsVides());
        assertEquals(3, j.taille(1));         // beta, gamma, delta
        assertEquals(2, j.taille(3));         // omega, sigma
    }

    @Test
    void motsVidesRetires() {
        // 4 livres, 50 % : un mot présent dans plus de 2 livres est vide -> beta et gamma retirés
        Jaccard j = new Jaccard(petitIndex(), 0.5);
        assertEquals(2, j.nbMotsVides());
        assertEquals(1, j.taille(0));               // il ne reste que alpha
        assertEquals(1.0, j.distance(0, 1), 1e-12); // {alpha} et {delta}
        assertEquals(0.0, j.distance(0, 2), 1e-12); // {alpha} et {alpha}
    }

    @Test
    void livresSansAucunMotGardeSontEloignes() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(1, "beta gamma");
        c.ajouterLivre(2, "beta gamma");
        c.ajouterLivre(3, "zeta");
        Jaccard j = new Jaccard(c.construire(), 0.5); // beta et gamma (2 livres sur 3) sont vides
        assertEquals(0, j.taille(0));
        assertEquals(1.0, j.distance(0, 1), 1e-12);
    }

    @Test
    void placeDesPaires() {
        int n = 5;
        int attendu = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                assertEquals(attendu++, Jaccard.indicePaire(i, j, n));
            }
        }
    }

    @Test
    void toutesLesDistancesRangeesCommeIndicePaire() {
        Jaccard j = new Jaccard(petitIndex(), 1.0);
        float[] d = j.toutesLesDistances();
        assertEquals(6, d.length);
        for (int a = 0; a < 4; a++) {
            for (int b = a + 1; b < 4; b++) {
                assertEquals(j.distance(a, b), d[Jaccard.indicePaire(a, b, 4)], 1e-6);
            }
        }
    }

    // ---------- comparaison avec le calcul sur les ensembles complets ----------

    // ensembles de mots complets de chaque livre (mots vides retirés), construits sans vecteur de bits
    private static List<Set<String>> ensembles(Index index, double partMax) {
        int dfMax = (int) Math.floor(partMax * index.nbLivres());
        List<Set<String>> e = new ArrayList<>();
        for (int i = 0; i < index.nbLivres(); i++) {
            e.add(new HashSet<>());
        }
        for (int m = 0; m < index.nbMots(); m++) {
            if (index.fin(m) - index.debut(m) > dfMax) {
                continue;
            }
            for (int p = index.debut(m); p < index.fin(m); p++) {
                e.get(index.livreDuPosting(p)).add(index.mot(m));
            }
        }
        return e;
    }

    private static double distanceEnsembles(Set<String> a, Set<String> b) {
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 1.0 : 1.0 - (double) inter.size() / union.size();
    }

    private static void comparer(Index index, double partMax) {
        Jaccard j = new Jaccard(index, partMax);
        List<Set<String>> e = ensembles(index, partMax);
        float[] toutes = j.toutesLesDistances();
        int n = index.nbLivres();
        for (int a = 0; a < n; a++) {
            assertEquals(e.get(a).size(), j.taille(a), "taille du livre " + a);
            for (int b = a + 1; b < n; b++) {
                double attendu = distanceEnsembles(e.get(a), e.get(b));
                assertEquals(attendu, j.distance(a, b), 1e-12, "livres " + a + " et " + b);
                assertEquals(attendu, toutes[Jaccard.indicePaire(a, b, n)], 1e-6, "livres " + a + " et " + b);
            }
        }
    }

    @Test
    void memeDistanceQueLesEnsemblesCompletsSurDeVraisTextes() throws Exception {
        // les 14 textes de test du projet 1 : 91 paires, des milliers de mots par texte
        Index.Constructeur c = new Index.Constructeur();
        int id = 1;
        for (Path fichier : Fixtures.textes()) {
            c.ajouterLivre(id++, Files.readString(fichier, StandardCharsets.UTF_8));
        }
        Index index = c.construire();
        comparer(index, 0.5);
        comparer(index, 1.0);
        Jaccard j = new Jaccard(index, 0.5);
        assertTrue(j.nbMotsUniques() > 0 && j.nbMotsVides() > 0 && j.nbMotsPartages() > 0);
    }

    @Test
    void memeDistanceQueLesEnsemblesCompletsSurDesLivresAleatoires() {
        Random hasard = new Random(42);
        for (int essai = 0; essai < 20; essai++) {
            Index.Constructeur c = new Index.Constructeur();
            int nbLivres = 2 + hasard.nextInt(15);
            for (int l = 0; l < nbLivres; l++) {
                // mots de deux lettres tirés dans un petit vocabulaire (plus de 64 mots : plusieurs long)
                StringBuilder texte = new StringBuilder();
                int nbMots = hasard.nextInt(200);
                for (int k = 0; k < nbMots; k++) {
                    texte.append((char) ('a' + hasard.nextInt(12))).append((char) ('a' + hasard.nextInt(12))).append(' ');
                }
                c.ajouterLivre(l, texte);
            }
            Index index = c.construire();
            comparer(index, 0.5);
            comparer(index, 1.0);
            comparer(index, 0.2);
        }
    }
}
