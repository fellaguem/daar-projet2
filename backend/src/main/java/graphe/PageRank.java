package graphe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

// PageRank sur le graphe de livres, sans poids sur les arêtes.
// Chaque livre partage son score entre ses voisins.
public final class PageRank {

    public static final double AMORTISSEMENT_DEFAUT = 0.85;
    public static final double TOLERANCE_DEFAUT = 1e-10;
    public static final int ITERATIONS_MAX_DEFAUT = 200;

    private static final int MAGIE = 0x50475231; // PGR1

    private PageRank() {
    }

    public record Resultat(double[] scores, int iterations, double ecart) {
    }

    public static Resultat calculer(Graphe g) {
        return calculer(g, AMORTISSEMENT_DEFAUT, TOLERANCE_DEFAUT, ITERATIONS_MAX_DEFAUT);
    }

    public static Resultat calculer(Graphe g, double amortissement, double tolerance, int iterationsMax) {
        int n = g.nbSommets();
        if (n == 0) {
            return new Resultat(new double[0], 0, 0);
        }

        double[] pr = new double[n];
        double[] nx = new double[n];
        Arrays.fill(pr, 1.0 / n);

        int tour = 0;
        double delta = Double.MAX_VALUE;

        while (tour < iterationsMax && delta >= tolerance) {
            tour++;

            double perdu = 0; // score des livres sans voisin
            for (int i = 0; i < n; i++) {
                if (g.degre(i) == 0)
                    perdu += pr[i];
            }

            Arrays.fill(nx, (1 - amortissement) / n + amortissement * perdu / n);

            // redistribuer aux voisins
            for (int j = 0; j < n; j++) {
                int dg = g.degre(j);
                if (dg > 0) {
                    double val = amortissement * pr[j] / dg;
                    for (int p = g.debut(j); p < g.fin(j); p++) {
                        nx[g.voisin(p)] += val;
                    }
                }
            }

            delta = 0;
            for (int i = 0; i < n; i++) {
                delta += Math.abs(nx[i] - pr[i]);
            }
            double[] tmp = pr;
            pr = nx;
            nx = tmp;
        }

        return new Resultat(pr, tour, delta);
    }

    // Ecriture et lecture des scores
    public record Scores(int[] idsLivres, double[] scores) {
    }

    public static void sauver(Path fichier, int[] idsLivres, double[] scores) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(fichier), 1 << 16))) {
            out.writeInt(MAGIE);
            out.writeInt(idsLivres.length);

            for (int i = 0; i < idsLivres.length; i++) {
                out.writeInt(idsLivres[i]);
                out.writeDouble(scores[i]);
            }
        }
    }

    public static Scores charger(Path fichier) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(fichier), 1 << 16))) {
            if (in.readInt() != MAGIE) {
                throw new IOException("Ce fichier n'est pas un PageRank : " + fichier);
            }

            int n = in.readInt();
            int[] ids = new int[n];
            double[] vals = new double[n];
            for (int i = 0; i < n; i++) {
                ids[i] = in.readInt();
                vals[i] = in.readDouble();
            }
            return new Scores(ids, vals);
        }
    }
}
