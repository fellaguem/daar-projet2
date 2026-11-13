package graphe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import index.Index;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    // ---------- empreinte : les K mots les plus fréquents de chaque livre ----------

    // occurrences : livre 0 whale 3, sea 2, captain 1, ship 1
    //               livre 1 sea 3, ocean 2, whale 2, ship 1
    //               livre 2 garden 2, rose 1
    private static Index indexAvecOccurrences() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(1, "whale whale whale sea sea ship captain");
        c.ajouterLivre(2, "whale whale sea sea sea ocean ocean ship");
        c.ajouterLivre(3, "garden garden rose");
        return c.construire();
    }

    @Test
    void empreinteDesMotsLesPlusFrequents() {
        Index index = indexAvecOccurrences();

        // K = 1 : {whale} et {sea}
        assertEquals(1.0, new Jaccard(index, 1.0, 1).distance(0, 1), 1e-12);

        // K = 2 : {whale, sea} et {sea, ocean} (ocean et whale à égalité : ordre alphabétique)
        Jaccard deux = new Jaccard(index, 1.0, 2);
        assertEquals(1 - 1.0 / 3, deux.distance(0, 1), 1e-12);
        assertEquals(2, deux.taille(0));
        assertEquals(2, deux.taille(2));

        // K = 3 : {whale, sea, captain} (captain avant ship) et {sea, ocean, whale}
        assertEquals(0.5, new Jaccard(index, 1.0, 3).distance(0, 1), 1e-12);
    }

    @Test
    void empreintePlusGrandeQueLeVocabulaire() {
        Index index = indexAvecOccurrences();
        Jaccard tous = new Jaccard(index, 1.0);
        Jaccard grand = new Jaccard(index, 1.0, 100);
        assertEquals(2, grand.taille(2)); // garden, rose : le livre a moins de 100 mots
        for (int a = 0; a < 3; a++) {
            assertEquals(tous.taille(a), grand.taille(a));
            for (int b = a + 1; b < 3; b++) {
                assertEquals(tous.distance(a, b), grand.distance(a, b), 1e-12);
            }
        }
    }

    @Test
    void empreinteSansMotsVides() {
        // 3 livres, 50 % : un mot dans plus d'un livre est vide -> whale, sea, ship retirés
        // livre 0 : captain ; livre 1 : ocean ; K = 1
        Jaccard j = new Jaccard(indexAvecOccurrences(), 0.5, 1);
        assertEquals(3, j.nbMotsVides());
        assertEquals(1, j.taille(0));
        assertEquals(1.0, j.distance(0, 1), 1e-12);
    }

    @Test
    void motsParLivreNegatifRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new Jaccard(petitIndex(), 0.5, -1));
    }

    // ---------- comparaison avec le calcul sur les ensembles complets ----------

    // ensembles de mots de chaque livre (mots vides retirés), construits sans vecteur de bits ;
    // k > 0 : seulement les k mots les plus fréquents du livre, à égalité par ordre alphabétique
    private static List<Set<String>> ensembles(Index index, double partMax, int k) {
        int dfMax = (int) Math.floor(partMax * index.nbLivres());
        List<List<String>> mots = new ArrayList<>();
        List<Map<String, Integer>> occ = new ArrayList<>();
        for (int i = 0; i < index.nbLivres(); i++) {
            mots.add(new ArrayList<>());
            occ.add(new HashMap<>());
        }
        for (int m = 0; m < index.nbMots(); m++) {
            if (index.fin(m) - index.debut(m) > dfMax) {
                continue;
            }
            for (int p = index.debut(m); p < index.fin(m); p++) {
                int livre = index.livreDuPosting(p);
                mots.get(livre).add(index.mot(m));
                occ.get(livre).put(index.mot(m), index.occurrencesDuPosting(p));
            }
        }
        List<Set<String>> e = new ArrayList<>();
        for (int i = 0; i < index.nbLivres(); i++) {
            Map<String, Integer> o = occ.get(i);
            List<String> l = mots.get(i);
            l.sort(Comparator.comparing((String s) -> -o.get(s)).thenComparing(Comparator.naturalOrder()));
            e.add(new HashSet<>(k > 0 && l.size() > k ? l.subList(0, k) : l));
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
        comparer(index, partMax, Jaccard.TOUS_LES_MOTS);
    }

    private static void comparer(Index index, double partMax, int k) {
        Jaccard j = new Jaccard(index, partMax, k);
        List<Set<String>> e = ensembles(index, partMax, k);
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
        comparer(index, 0.5, 50);
        comparer(index, 0.5, 300);
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
            comparer(index, 0.5, 1 + hasard.nextInt(30));
            comparer(index, 1.0, 1 + hasard.nextInt(30));
        }
    }
}
