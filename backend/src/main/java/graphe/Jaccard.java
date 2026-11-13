package graphe;

import index.Index;
import java.util.Arrays;
import java.util.stream.IntStream;

// Distance de Jaccard entre les livres, calculée à partir de l'index (sans relire les textes).
//
//   d(A, B) = 1 - |A ∩ B| / |A ∪ B|     A, B = ensembles de mots des deux livres
//
// Ensemble de mots d'un livre :
// - les mots vides sont retirés : un mot présent dans plus de partMax des livres (par défaut 50 %) ;
// - motsParLivre = 0 : tous les autres mots du livre ;
// - motsParLivre = K > 0 : seulement les K mots les plus fréquents du livre (son "empreinte"),
//   à égalité par ordre alphabétique. Tous les livres ont alors des ensembles de même taille
//   (K, ou moins pour un livre au vocabulaire plus petit). Sans cela, la distance dépend surtout
//   de la longueur : deux livres longs ont de grands vocabulaires qui se recoupent beaucoup, alors
//   qu'un livre court face à un long a une petite intersection et une grande union (voir notes.md).
//
// Représentation :
// - un mot qui n'est dans l'ensemble que d'un seul livre est gardé, mais sans bit : il ne peut jamais
//   être dans une intersection, il suffit de le compter dans la taille du livre. La distance est
//   exactement celle des ensembles complets, avec des vecteurs de bits beaucoup plus petits ;
// - les autres mots ("partagés") ont chacun un bit.
//
// Chaque livre est un vecteur de bits (tableau de long) sur les mots partagés :
//   |A ∩ B| = nombre de bits à 1 dans (A ET B)     |A ∪ B| = |A| + |B| - |A ∩ B|
public final class Jaccard {

    public static final double PART_MAX_DEFAUT = 0.5;
    public static final int TOUS_LES_MOTS = 0;

    private final int nbLivres;
    private final long[][] bits;   // bits[livre] : un bit par mot partagé
    private final int[] tailles;   // |A| : nombre de mots dans l'ensemble du livre
    private final int nbMotsPartages;
    private final int nbMotsUniques;
    private final int nbMotsVides;

    public Jaccard(Index index, double partMax) {
        this(index, partMax, TOUS_LES_MOTS);
    }

    public Jaccard(Index index, double partMax, int motsParLivre) {
        if (motsParLivre < 0) {
            throw new IllegalArgumentException("motsParLivre doit être positif ou nul : " + motsParLivre);
        }
        nbLivres = index.nbLivres();
        int nbMots = index.nbMots();

        // 1. mots vides : présents dans plus de dfMax livres
        int dfMax = (int) Math.floor(partMax * nbLivres);
        boolean[] vide = new boolean[nbMots];
        int vides = 0;
        int[] nbParLivre = new int[nbLivres]; // nombre de mots non vides de chaque livre
        for (int m = 0; m < nbMots; m++) {
            if (index.fin(m) - index.debut(m) > dfMax) {
                vide[m] = true;
                vides++;
                continue;
            }
            for (int p = index.debut(m); p < index.fin(m); p++) {
                nbParLivre[index.livreDuPosting(p)]++;
            }
        }
        nbMotsVides = vides;

        // 2. mots non vides de chaque livre, rangés livre par livre : livre i dans cles[debut[i]..debut[i+1][.
        //    Une clé = (Integer.MAX_VALUE - occurrences) sur 32 bits hauts, numéro du mot sur 32 bits bas :
        //    trier les clés par ordre croissant range les mots du plus fréquent au moins fréquent,
        //    puis par ordre alphabétique (les mots de l'index sont triés).
        int[] debut = new int[nbLivres + 1];
        for (int i = 0; i < nbLivres; i++) {
            debut[i + 1] = debut[i] + nbParLivre[i];
        }
        long[] cles = new long[debut[nbLivres]];
        int[] place = Arrays.copyOf(debut, nbLivres);
        for (int m = 0; m < nbMots; m++) {
            if (vide[m]) {
                continue;
            }
            for (int p = index.debut(m); p < index.fin(m); p++) {
                int livre = index.livreDuPosting(p);
                long occ = index.occurrencesDuPosting(p);
                cles[place[livre]++] = ((Integer.MAX_VALUE - occ) << 32) | m;
            }
        }

        // 3. ensemble de chaque livre : tous ses mots, ou les K premiers après tri
        tailles = new int[nbLivres];
        IntStream.range(0, nbLivres).parallel().forEach(i -> {
            if (motsParLivre > 0) {
                Arrays.sort(cles, debut[i], debut[i + 1]);
                tailles[i] = Math.min(motsParLivre, nbParLivre[i]);
            } else {
                tailles[i] = nbParLivre[i];
            }
        });

        // 4. un bit pour chaque mot présent dans au moins deux ensembles
        int[] nbEnsembles = new int[nbMots];
        for (int i = 0; i < nbLivres; i++) {
            for (int k = debut[i]; k < debut[i] + tailles[i]; k++) {
                nbEnsembles[mot(cles[k])]++;
            }
        }
        int[] numeroBit = new int[nbMots];
        int partages = 0;
        int uniques = 0;
        for (int m = 0; m < nbMots; m++) {
            if (nbEnsembles[m] >= 2) {
                numeroBit[m] = partages++;
            } else {
                numeroBit[m] = -1;
                if (nbEnsembles[m] == 1) {
                    uniques++;
                }
            }
        }
        nbMotsPartages = partages;
        nbMotsUniques = uniques;

        // 5. vecteurs de bits
        bits = new long[nbLivres][(partages + 63) / 64];
        for (int i = 0; i < nbLivres; i++) {
            for (int k = debut[i]; k < debut[i] + tailles[i]; k++) {
                int b = numeroBit[mot(cles[k])];
                if (b >= 0) {
                    bits[i][b >>> 6] |= 1L << (b & 63);
                }
            }
        }
    }

    // numéro du mot rangé dans une clé (32 bits bas)
    private static int mot(long cle) {
        return (int) (cle & 0xFFFFFFFFL);
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

    // mots qui ont un bit (dans au moins deux ensembles)
    public int nbMotsPartages() {
        return nbMotsPartages;
    }

    // mots qui ne sont que dans l'ensemble d'un seul livre (comptés dans sa taille, sans bit)
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
