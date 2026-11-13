package graphe;

import index.Index;
import java.util.stream.IntStream;

// Distance de Jaccard entre les livres, calculée à partir de l'index (sans relire les textes).
//
//   d(A, B) = 1 - |A ∩ B| / |A ∪ B|     A, B = ensembles de mots des deux livres
//
// Mots pris en compte :
// - les mots vides sont retirés : un mot présent dans plus de partMax des livres (par défaut 50 %) ;
// - les mots présents dans un seul livre sont gardés, mais sans bit : ils ne peuvent jamais être
//   dans une intersection, il suffit de les compter dans la taille du livre. La distance est
//   exactement celle des ensembles complets, avec des vecteurs de bits beaucoup plus petits ;
// - les autres mots ("partagés") ont chacun un bit.
//
// Chaque livre est un vecteur de bits (tableau de long) sur les mots partagés :
//   |A ∩ B| = nombre de bits à 1 dans (A ET B)     |A ∪ B| = |A| + |B| - |A ∩ B|
public final class Jaccard {

    public static final double PART_MAX_DEFAUT = 0.5;

    private final int nbLivres;
    private final long[][] bits;   // bits[livre] : un bit par mot partagé
    private final int[] tailles;   // |A| : nombre de mots gardés du livre (partagés + présents dans ce seul livre)
    private final int nbMotsPartages;
    private final int nbMotsUniques;
    private final int nbMotsVides;

    public Jaccard(Index index, double partMax) {
        nbLivres = index.nbLivres();

        // un mot est vide s'il est dans plus de dfMax livres
        int dfMax = (int) Math.floor(partMax * nbLivres);

        // numéro de bit de chaque mot : >= 0 pour un mot partagé, -1 présent dans un seul livre, -2 mot vide
        int[] numeroBit = new int[index.nbMots()];
        int partages = 0;
        int uniques = 0;
        int vides = 0;
        for (int m = 0; m < index.nbMots(); m++) {
            int nbLivresDuMot = index.fin(m) - index.debut(m);
            if (nbLivresDuMot > dfMax) {
                numeroBit[m] = -2;
                vides++;
            } else if (nbLivresDuMot == 1) {
                numeroBit[m] = -1;
                uniques++;
            } else {
                numeroBit[m] = partages++;
            }
        }
        nbMotsPartages = partages;
        nbMotsUniques = uniques;
        nbMotsVides = vides;

        tailles = new int[nbLivres];
        bits = new long[nbLivres][(partages + 63) / 64];
        for (int m = 0; m < index.nbMots(); m++) {
            int b = numeroBit[m];
            if (b == -2) {
                continue;
            }
            for (int p = index.debut(m); p < index.fin(m); p++) {
                int livre = index.livreDuPosting(p);
                tailles[livre]++;
                if (b >= 0) {
                    bits[livre][b >>> 6] |= 1L << (b & 63);
                }
            }
        }
    }

    // distance entre les livres a et b (numéros dans l'index)
    public double distance(int a, int b) {
        long[] x = bits[a];
        long[] y = bits[b];
        int intersection = 0;
        for (int k = 0; k < x.length; k++) {
            intersection += Long.bitCount(x[k] & y[k]);
        }
        int union = tailles[a] + tailles[b] - intersection;
        // deux livres sans aucun mot gardé : aucune information, on les considère comme éloignés
        return union == 0 ? 1.0 : 1.0 - (double) intersection / union;
    }

    // Toutes les distances d(i, j) pour i < j, rangées ligne par ligne dans un tableau
    // (place de la paire : indicePaire). Les lignes sont calculées en parallèle.
    public float[] toutesLesDistances() {
        int n = nbLivres;
        float[] d = new float[(int) ((long) n * (n - 1) / 2)];
        IntStream.range(0, n).parallel().forEach(i -> {
            int k = i + 1 < n ? indicePaire(i, i + 1, n) : 0;
            for (int j = i + 1; j < n; j++) {
                d[k++] = (float) distance(i, j);
            }
        });
        return d;
    }

    // place de la paire (i, j), i < j, dans le tableau de toutesLesDistances
    public static int indicePaire(int i, int j, int n) {
        return (int) ((long) i * (2L * n - i - 1) / 2 + (j - i - 1));
    }

    public int nbLivres() {
        return nbLivres;
    }

    public int taille(int livre) {
        return tailles[livre];
    }

    public int nbMotsPartages() {
        return nbMotsPartages;
    }

    public int nbMotsUniques() {
        return nbMotsUniques;
    }

    public int nbMotsVides() {
        return nbMotsVides;
    }

    // taille mémoire des vecteurs de bits, en octets
    public long tailleBits() {
        return nbLivres == 0 ? 0 : (long) nbLivres * bits[0].length * 8L;
    }
}
