package graphe;

import index.Index;
import java.util.Arrays;
import java.util.stream.IntStream;

// Similarité entre livres, sans relire les textes, Jaccard : 1 - intersection / union des mots gardés on vire les mots trop courants, puis on garde tous les mots ou seulement les K plus fréquents.
// Les mots partagés ont des bits ; les mots uniques comptent juste dans la taille, ca évite de stocker un énorme tableau pour chaque livre.

public final class Jaccard {

    public static final double PART_MAX_DEFAUT = 0.5;
    public static final int TOUS_LES_MOTS = 0;

    private final int nbLivres;
    private final long[][] bits; // tableau par livre
    private final int[] tailles; // combien de mots gardés
    private final int nbMotsPartages;
    private final int nbMotsUniques;
    private final int nbMotsVides;

    public Jaccard(Index ix, double partMax) {
        this(ix, partMax, TOUS_LES_MOTS);
    }

    public Jaccard(Index ix, double partMax, int motsParLivre) {
        if (motsParLivre < 0) {
            throw new IllegalArgumentException("motsParLivre doit être positif ou nul : " + motsParLivre);
        }
        nbLivres = ix.nbLivres();
        int voc = ix.nbMots();

        // mots trop fréquents, on les ignore
        int limite = (int) Math.floor(partMax * nbLivres);
        boolean[] stop = new boolean[voc];
        int nbStop = 0;
        int[] freq = new int[nbLivres]; // mots non vides par livre
        for (int m = 0; m < voc; m++) {
            if (ix.fin(m) - ix.debut(m) > limite) {
                stop[m] = true;
                nbStop++;
                continue;
            }
            for (int p = ix.debut(m); p < ix.fin(m); p++) {
                freq[ix.livreDuPosting(p)]++;
            }
        }
        nbMotsVides = nbStop;

        // le tri se fait par fréquence, puis par numéro de mot
        int[] db = new int[nbLivres + 1];
        for (int i = 0; i < nbLivres; i++) {
            db[i + 1] = db[i] + freq[i];
        }
        long[] keys = new long[db[nbLivres]];
        int[] pos = Arrays.copyOf(db, nbLivres);
        for (int m = 0; m < voc; m++) {
            if (stop[m]) {
                continue;
            }
            for (int p = ix.debut(m); p < ix.fin(m); p++) {
                int livre = ix.livreDuPosting(p);
                long nbOcc = ix.occurrencesDuPosting(p);
                keys[pos[livre]++] = ((Integer.MAX_VALUE - nbOcc) << 32) | m;
            }
        }

        // on prend tous les mots, ou les K premiers
        tailles = new int[nbLivres];
        IntStream.range(0, nbLivres).parallel().forEach(i -> {
            if (motsParLivre > 0) {
                Arrays.sort(keys, db[i], db[i + 1]); // les plus fréquents d’abord
                tailles[i] = Math.min(motsParLivre, freq[i]);
            } else {
                tailles[i] = freq[i];
            }
        });

        // seuls les mots partagés ont un bit
        int[] compte = new int[voc];
        for (int i = 0; i < nbLivres; i++) {
            for (int k = db[i]; k < db[i] + tailles[i]; k++) {
                compte[mot(keys[k])]++;
            }
        }
        int[] rangBit = new int[voc];
        int communs = 0;
        int solos = 0;
        for (int m = 0; m < voc; m++) {
            if (compte[m] >= 2) {
                rangBit[m] = communs++;
            } else {
                rangBit[m] = -1;
                if (compte[m] == 1) {
                    solos++;
                }
            }
        }
        nbMotsPartages = communs;
        nbMotsUniques = solos;

        // on remplit les bits des livres
        bits = new long[nbLivres][(communs + 63) / 64];
        for (int i = 0; i < nbLivres; i++) {
            for (int k = db[i]; k < db[i] + tailles[i]; k++) {
                int b = rangBit[mot(keys[k])];
                if (b >= 0) {
                    bits[i][b >>> 6] |= 1L << (b & 63);
                }
            }
        }
    }

    // le numéro est dans les 32 bits du bas
    private static int mot(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    // distance entre deux livres
    public double distance(int a, int b) {
        long[] x = bits[a];
        long[] y = bits[b];
        int commun = 0;
        for (int k = 0; k < x.length; k++) {
            commun += Long.bitCount(x[k] & y[k]);
        }
        int fusion = tailles[a] + tailles[b] - commun;
        // rien à comparer, donc distance maximale
        return fusion == 0 ? 1.0 : 1.0 - (double) commun / fusion; // cas vide
    }

    // toutes les paires, en parallèle
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

    // où se trouve la paire i,j
    public static int indicePaire(int i, int j, int n) {
        return (int) ((long) i * (2L * n - i - 1) / 2 + (j - i - 1));
    }

    public int nbLivres() {
        return nbLivres;
    }

    public int taille(int livre) {
        return tailles[livre];
    }

    // mots présents dans plusieurs livres
    public int nbMotsPartages() {
        return nbMotsPartages;
    }

    // présents dans un seul livre
    public int nbMotsUniques() {
        return nbMotsUniques;
    }

    public int nbMotsVides() {
        return nbMotsVides;
    }

    // octets utilisés par les bits
    public long tailleBits() {
        return nbLivres == 0 ? 0 : (long) nbLivres * bits[0].length * 8L;
    }
}
