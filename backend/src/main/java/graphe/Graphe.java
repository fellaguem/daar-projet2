package graphe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Locale;

// Graphe de Jaccard : un sommet par livre, une arête entre deux livres si leur distance est
// strictement sous le seuil. Graphe non orienté : chaque arête est rangée dans les deux sens.
//
// Rangement en tableaux (comme l'index) :
//   voisins[debut[i]] .. voisins[debut[i+1]-1]   les voisins du livre i, du plus proche au plus loin
//   distances[p]                                  la distance de l'arête p
// Les sommets sont les numéros de livres de l'index ; idLivre(i) donne l'identifiant Gutenberg.
public final class Graphe {

    private static final int MAGIE = 0x47524631; // "GRF1"

    private final int[] idsLivres;
    private final int[] debut;
    private final int[] voisins;
    private final float[] distances;
    private final double seuil;

    private Graphe(int[] idsLivres, int[] debut, int[] voisins, float[] distances, double seuil) {
        this.idsLivres = idsLivres;
        this.debut = debut;
        this.voisins = voisins;
        this.distances = distances;
        this.seuil = seuil;
    }

    // dist : distances rangées comme Jaccard.toutesLesDistances (paires i < j, ligne par ligne)
    public static Graphe depuisDistances(int[] idsLivres, float[] dist, double seuil) {
        int n = idsLivres.length;

        // 1. degré de chaque sommet
        int[] degre = new int[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (dist[k++] < seuil) {
                    degre[i]++;
                    degre[j]++;
                }
            }
        }
        int[] debut = new int[n + 1];
        for (int i = 0; i < n; i++) {
            debut[i + 1] = debut[i] + degre[i];
        }

        // 2. on range chaque arête dans les deux sens, sous forme d'une clé (distance, voisin) :
        // une distance positive garde le même ordre que ses bits, donc trier les clés trie par distance
        long[] cles = new long[debut[n]];
        int[] place = Arrays.copyOf(debut, n);
        k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float d = dist[k++];
                if (d < seuil) {
                    cles[place[i]++] = cle(d, j);
                    cles[place[j]++] = cle(d, i);
                }
            }
        }

        // 3. voisins de chaque sommet du plus proche au plus loin
        int[] voisins = new int[cles.length];
        float[] distances = new float[cles.length];
        for (int i = 0; i < n; i++) {
            Arrays.sort(cles, debut[i], debut[i + 1]);
            for (int p = debut[i]; p < debut[i + 1]; p++) {
                voisins[p] = (int) cles[p];
                distances[p] = Float.intBitsToFloat((int) (cles[p] >>> 32));
            }
        }
        return new Graphe(idsLivres.clone(), debut, voisins, distances, seuil);
    }

    private static long cle(float distance, int voisin) {
        return ((long) Float.floatToIntBits(distance) << 32) | voisin;
    }

    // Seuil qui donne environ degreMoyen voisins par livre : on garde les m = degreMoyen * n / 2
    // plus petites distances (moins si plusieurs paires ont exactement la même distance).
    public static double seuilPourDegreMoyen(float[] dist, int n, double degreMoyen) {
        long m = Math.round(degreMoyen * n / 2.0);
        if (m <= 0) {
            return 0.0; // aucune arête
        }
        if (m >= dist.length) {
            return 2.0; // toutes les arêtes (une distance vaut au plus 1)
        }
        float[] tri = dist.clone();
        Arrays.sort(tri);
        return tri[(int) m];
    }

    // ---------- accès ----------

    public int nbSommets() {
        return idsLivres.length;
    }

    public int idLivre(int sommet) {
        return idsLivres[sommet];
    }

    public int[] idsLivres() {
        return idsLivres.clone();
    }

    public double seuil() {
        return seuil;
    }

    // nombre d'arêtes (chaque arête comptée une fois)
    public int nbAretes() {
        return voisins.length / 2;
    }

    public int degre(int sommet) {
        return debut[sommet + 1] - debut[sommet];
    }

    public int debut(int sommet) {
        return debut[sommet];
    }

    public int fin(int sommet) {
        return debut[sommet + 1];
    }

    public int voisin(int p) {
        return voisins[p];
    }

    public float distance(int p) {
        return distances[p];
    }

    public int nbIsoles() {
        int n = 0;
        for (int i = 0; i < nbSommets(); i++) {
            if (degre(i) == 0) {
                n++;
            }
        }
        return n;
    }

    // taille de chaque composante connexe (parcours en largeur), de la plus grande à la plus petite
    public int[] composantes() {
        int n = nbSommets();
        boolean[] vu = new boolean[n];
        int[] tailles = new int[n];
        int nb = 0;
        ArrayDeque<Integer> file = new ArrayDeque<>();
        for (int s = 0; s < n; s++) {
            if (vu[s]) {
                continue;
            }
            int taille = 0;
            vu[s] = true;
            file.add(s);
            while (!file.isEmpty()) {
                int u = file.poll();
                taille++;
                for (int p = debut[u]; p < debut[u + 1]; p++) {
                    if (!vu[voisins[p]]) {
                        vu[voisins[p]] = true;
                        file.add(voisins[p]);
                    }
                }
            }
            tailles[nb++] = taille;
        }
        int[] resultat = Arrays.copyOf(tailles, nb);
        Arrays.sort(resultat);
        for (int i = 0; i < nb / 2; i++) {
            int t = resultat[i];
            resultat[i] = resultat[nb - 1 - i];
            resultat[nb - 1 - i] = t;
        }
        return resultat;
    }

    // ---------- fichiers ----------

    // liste d'arêtes lisible (une ligne par arête, identifiants Gutenberg), pour le rapport
    public void ecrireAretesCsv(Path fichier) throws IOException {
        try (PrintStream out = new PrintStream(Files.newOutputStream(fichier), false, StandardCharsets.UTF_8)) {
            out.println("id1,id2,distance");
            for (int i = 0; i < nbSommets(); i++) {
                for (int p = debut[i]; p < debut[i + 1]; p++) {
                    if (i < voisins[p]) {
                        out.printf(Locale.ROOT, "%d,%d,%.6f%n", idsLivres[i], idsLivres[voisins[p]], distances[p]);
                    }
                }
            }
        }
    }

    public void sauver(Path fichier) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(fichier), 1 << 16))) {
            out.writeInt(MAGIE);
            out.writeInt(idsLivres.length);
            out.writeInt(voisins.length);
            out.writeDouble(seuil);
            for (int id : idsLivres) {
                out.writeInt(id);
            }
            for (int d : debut) {
                out.writeInt(d);
            }
            for (int i = 0; i < voisins.length; i++) {
                out.writeInt(voisins[i]);
                out.writeFloat(distances[i]);
            }
        }
    }

    public static Graphe charger(Path fichier) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(fichier), 1 << 16))) {
            if (in.readInt() != MAGIE) {
                throw new IOException("Ce fichier n'est pas un graphe : " + fichier);
            }
            int n = in.readInt();
            int nbEntrees = in.readInt();
            double seuil = in.readDouble();
            int[] ids = new int[n];
            for (int i = 0; i < n; i++) {
                ids[i] = in.readInt();
            }
            int[] debut = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                debut[i] = in.readInt();
            }
            int[] voisins = new int[nbEntrees];
            float[] distances = new float[nbEntrees];
            for (int i = 0; i < nbEntrees; i++) {
                voisins[i] = in.readInt();
                distances[i] = in.readFloat();
            }
            return new Graphe(ids, debut, voisins, distances, seuil);
        }
    }
}
