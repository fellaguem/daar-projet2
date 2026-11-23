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

// Un sommet = un livre. On relie les deux quand la distance passe sous le seuil.
// Les arêtes vont dans les deux sens, et les voisins sont triés par distance.
public final class Graphe {

    private static final int MAGIE = 0x47524631; // GRF1

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

    // dist suit l'ordre des paires i < j de Jaccard
    public static Graphe depuisDistances(int[] idsLivres, float[] dist, double seuil) {
        int n = idsLivres.length;

        int[] deg = new int[n]; // compter les voisins d'abord
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (dist[k++] < seuil) {
                    deg[i]++;
                    deg[j]++;
                }
            }
        }

        int[] deb = new int[n + 1];
        for (int i = 0; i < n; i++)
            deb[i + 1] = deb[i] + deg[i];

        // chaque lien est ajouté 2 fois
        long[] keys = new long[deb[n]];
        int[] pos = Arrays.copyOf(deb, n);
        k = 0;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float d = dist[k++];
                if (d < seuil) {
                    keys[pos[i]++] = cle(d, j);
                    keys[pos[j]++] = cle(d, i);
                }
            }
        }

        int[] vois = new int[keys.length];
        float[] dd = new float[keys.length];

        for (int i = 0; i < n; i++) {
            Arrays.sort(keys, deb[i], deb[i + 1]);
            for (int p = deb[i]; p < deb[i + 1]; p++) {
                vois[p] = (int) keys[p];
                dd[p] = Float.intBitsToFloat((int) (keys[p] >>> 32));
            }
        }
        return new Graphe(idsLivres.clone(), deb, vois, dd, seuil);
    }

    private static long cle(float distance, int voisin) {
        return ((long) Float.floatToIntBits(distance) << 32) | voisin;
    }

    // on prend les m plus petites distances
    public static double seuilPourDegreMoyen(float[] dist, int n, double degreMoyen) {
        long m = Math.round(degreMoyen * n / 2.0);
        if (m <= 0)
            return 0.0;
        if (m >= dist.length)
            return 2.0; // toutes les arêtes

        float[] tri = dist.clone();
        Arrays.sort(tri);
        return tri[(int) m];
    }

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
        int nb = 0;
        for (int i = 0; i < nbSommets(); i++) {
            if (degre(i) == 0)
                nb++;
        }
        return nb;
    }

    // composantes connexes, avec une file
    public int[] composantes() {
        int n = nbSommets();
        boolean[] seen = new boolean[n];
        int[] tailles = new int[n];
        int nb = 0;

        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int s = 0; s < n; s++) {
            if (seen[s])
                continue;

            int t = 0;
            seen[s] = true;
            q.add(s);
            while (!q.isEmpty()) {
                int u = q.poll();
                t++;
                for (int p = debut[u]; p < debut[u + 1]; p++) {
                    if (!seen[voisins[p]]) {
                        seen[voisins[p]] = true;
                        q.add(voisins[p]);
                    }
                }
            }
            tailles[nb++] = t;
        }

        int[] res = Arrays.copyOf(tailles, nb);
        Arrays.sort(res);
        for (int i = 0; i < nb / 2; i++) {
            int tmp = res[i];
            res[i] = res[nb - 1 - i];
            res[nb - 1 - i] = tmp;
        }
        return res;
    }

    // une ligne CSV par arête, sans doublon
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

            for (int id : idsLivres)
                out.writeInt(id);
            for (int d : debut)
                out.writeInt(d);

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
            int nEnt = in.readInt();
            double s = in.readDouble();

            int[] ids = new int[n];
            for (int i = 0; i < n; i++)
                ids[i] = in.readInt();

            int[] deb = new int[n + 1];
            for (int i = 0; i <= n; i++)
                deb[i] = in.readInt();

            int[] vois = new int[nEnt];
            float[] dd = new float[nEnt];
            for (int i = 0; i < nEnt; i++) {
                vois[i] = in.readInt();
                dd[i] = in.readFloat();
            }
            return new Graphe(ids, deb, vois, dd, s);
        }
    }
}
