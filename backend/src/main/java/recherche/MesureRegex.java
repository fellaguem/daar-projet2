package recherche;

import index.Index;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

// Mesure le temps de la recherche par regex dans l'index (moyenne et écart type), pour les courbes du rapport
public class MesureRegex {

    private static final List<String> REGEXS = List.of(
        "whale", "wh(a|e)le", "s(a|g|r)*on", "a.*b", ".*ing", "(sh|wh)(a|o)(l|r)(e|k)", "q.*z", ".*");

    public static void main(String[] args) throws IOException {
        Path fichierIndex = Path.of("data/index.bin");
        Path sortie = Path.of("data/mesures/regex_index.csv");
        int essais = 10;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--index" -> fichierIndex = Path.of(args[++i]);
                case "--sortie" -> sortie = Path.of(args[++i]);
                case "--essais" -> essais = Integer.parseInt(args[++i]);
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    return;
                }
            }
        }

        Index index = Index.charger(fichierIndex);
        RechercheRegex moteur = new RechercheRegex(index);
        System.out.println("Index : " + index.nbLivres() + " livres, " + index.nbMots() + " mots, "
            + index.nbPostings() + " postings, " + essais + " essais par regex");

        if (sortie.getParent() != null) {
            Files.createDirectories(sortie.getParent());
        }
        try (PrintStream csv = new PrintStream(Files.newOutputStream(sortie), true, StandardCharsets.UTF_8)) {
            csv.println("regex,mot_entier,essais,moyenne_ms,ecart_type_ms,mots_trouves,livres_trouves");

            for (boolean motEntier : new boolean[] {true, false}) {
              for (String regex : REGEXS) {
                if (!motEntier && regex.equals(".*")) {
                    continue;
                }
                // 3 appels pour chauffer la JVM, non comptés
                RechercheRegex.Reponse reponse = null;
                for (int i = 0; i < 3; i++) {
                    reponse = moteur.chercher(regex, motEntier);
                }

                double[] ms = new double[essais];
                for (int i = 0; i < essais; i++) {
                    long debut = System.nanoTime();
                    reponse = moteur.chercher(regex, motEntier);
                    ms[i] = (System.nanoTime() - debut) / 1e6;
                }

                double moyenne = 0;
                for (double t : ms) {
                    moyenne += t;
                }
                moyenne /= essais;
                double variance = 0;
                for (double t : ms) {
                    variance += (t - moyenne) * (t - moyenne);
                }
                double ecartType = Math.sqrt(variance / essais);

                System.out.printf(Locale.ROOT, "%-11s %-26s %8.2f ms ± %6.2f   %6d mots, %4d livres%n",
                    motEntier ? "mot entier" : "sous-chaîne", regex, moyenne, ecartType,
                    reponse.nbMotsTrouves(), reponse.resultats().size());
                csv.printf(Locale.ROOT, "\"%s\",%b,%d,%.3f,%.3f,%d,%d%n", regex, motEntier, essais, moyenne, ecartType,
                    reponse.nbMotsTrouves(), reponse.resultats().size());
              }
            }
        }
        System.out.println("Résultats : " + sortie);
    }
}
