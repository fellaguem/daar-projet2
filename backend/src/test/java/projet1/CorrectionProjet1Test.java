package projet1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ast.interfaces.AstI;
import automate.ConstructionAutomate;
import experimentation.Matcher;
import experimentation.MatcherNaif;
import experimentation.MatcherUnePasse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import kmp.Kmp;
import org.junit.jupiter.api.Test;
import parser.Parser;

// Reprise de TestCorrection du projet 1 : le naïf, l'une passe et KMP
// doivent donner le même résultat que java.util.regex, ligne par ligne.
class CorrectionProjet1Test {

    @Test
    void automatesEtKmpIdentiquesAJavaRegex() throws Exception {
        long total = 0;
        StringBuilder echecs = new StringBuilder();
        long nbEchecs = 0;

        for (String regex : Fixtures.regexs()) {
            AstI ast = new Parser(regex).parse();
            Pattern ref = Pattern.compile(regex);

            Matcher naif = new MatcherNaif(new ConstructionAutomate(ast).getAfdMinimal());
            Matcher uneP = new MatcherUnePasse(
                new ConstructionAutomate(MatcherUnePasse.envelopper(ast)).getAfdMinimal());
            String mot = Kmp.motifLitteral(ast);
            Matcher kmp = mot == null ? null : new Kmp(mot);

            for (Path fich : Fixtures.textes()) {
                List<String> lignes = Files.readAllLines(fich, StandardCharsets.UTF_8);
                for (int i = 0; i < lignes.size(); i++) {
                    String s = lignes.get(i);
                    boolean attendu = ref.matcher(s).find();
                    total += kmp == null ? 2 : 3;
                    if (naif.contientMotif(s) != attendu) { nbEchecs++; noter(echecs, "naif", regex, fich, i); }
                    if (uneP.contientMotif(s) != attendu) { nbEchecs++; noter(echecs, "unePasse", regex, fich, i); }
                    if (kmp != null && kmp.contientMotif(s) != attendu) { nbEchecs++; noter(echecs, "kmp", regex, fich, i); }
                }
            }
        }

        System.out.println("CorrectionProjet1Test : " + total + " comparaisons, " + nbEchecs + " échecs");
        assertEquals(0, nbEchecs, "Échecs (les 20 premiers) :\n" + echecs);
    }

    // on ne garde que les 20 premiers échecs pour que le message reste lisible
    private static void noter(StringBuilder sb, String algo, String regex, Path fich, int ligne) {
        if (sb.chars().filter(c -> c == '\n').count() < 20) {
            sb.append(algo).append(" | ").append(regex).append(" | ")
              .append(fich.getFileName()).append(" | ligne ").append(ligne + 1).append('\n');
        }
    }
}
