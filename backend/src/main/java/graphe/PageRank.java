package graphe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

// PageRank sur le graphe de Jaccard, par itération de la puissance (version classique, non pondérée).
//
// À chaque itération, pour chaque livre i :
//   PR'(i) = (1 - a) / n  +  a * ( somme sur les voisins j de i de PR(j) / degre(j)  +  M / n )
// a = facteur d'amortissement (0,85) ; M = somme des PR des livres isolés (sans voisin) :
// un livre isolé n'a personne à qui donner son score, on le redistribue à tous les livres
// (sinon la somme des scores ne vaudrait plus 1). Le graphe étant non orienté, chaque arête
// compte dans les deux sens.
// On s'arrête quand la somme des |PR' - PR| est sous la tolérance, ou après iterationsMax.
public final class PageRank {

    public static final double AMORTISSEMENT_DEFAUT = 0.85;
    public static final double TOLERANCE_DEFAUT = 1e-10;
    public static final int ITERATIONS_MAX_DEFAUT = 100;

    private static final int MAGIE = 0x50475231; // "PGR1"

    private PageRank() {}

    // scores[i] = PageRank du sommet i ; ecart = somme des |PR' - PR| à la dernière itération
    public record Resultat(double[] scores, int iterations, double ecart) {}

    public static Resultat calculer(Graphe g) {
        return calculer(g, AMORTISSEMENT_DEFAUT, TOLERANCE_DEFAUT, ITERATIONS_MAX_DEFAUT);
    }

    public static Resultat calculer(Graphe g, double amortissement, double tolerance, int iterationsMax) {
        int n = g.nbSommets();
        if (n == 0) {
            return new Resultat(new double[0], 0, 0);
        }

        double[] pr = new double[n];
        double[] nouveau = new double[n];
        Arrays.fill(pr, 1.0 / n);

        int iterations = 0;
        double ecart = Double.MAX_VALUE;
        while (iterations < iterationsMax && ecart >= tolerance) {
            iterations++;

            // score des livres isolés, redistribué à tous
            double isoles = 0;
            for (int i = 0; i < n; i++) {
                if (g.degre(i) == 0) {
                    isoles += pr[i];
                }
            }
            Arrays.fill(nouveau, (1 - amortissement) / n + amortissement * isoles / n);

            // chaque livre donne son score à parts égales à ses voisins
            for (int j = 0; j < n; j++) {
                int degre = g.degre(j);
                if (degre > 0) {
                    double part = amortissement * pr[j] / degre;
                    for (int p = g.debut(j); p < g.fin(j); p++) {
                        nouveau[g.voisin(p)] += part;
                    }
                }
            }

            ecart = 0;
            for (int i = 0; i < n; i++) {
                ecart += Math.abs(nouveau[i] - pr[i]);
            }
            double[] t = pr;
            pr = nouveau;
            nouveau = t;
        }
        return new Resultat(pr, iterations, ecart);
    }

    // ---------- fichier ----------

    public record Scores(int[] idsLivres, double[] scores) {}

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
            double[] scores = new double[n];
            for (int i = 0; i < n; i++) {
                ids[i] = in.readInt();
                scores[i] = in.readDouble();
            }
            return new Scores(ids, scores);
        }
    }
}
