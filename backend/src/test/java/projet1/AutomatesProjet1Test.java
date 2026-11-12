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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import parser.Parser;

// Reprise de TestAutomates du projet 1 : le naïf et l'une passe doivent
// trouver exactement les mêmes lignes.
class AutomatesProjet1Test {

    @Test
    void naifEtUnePasseTrouventLesMemesLignes() throws Exception {
        List<String> lignes = new ArrayList<>();
        for (Path fich : Fixtures.textes()) {
            lignes.addAll(Files.readAllLines(fich, StandardCharsets.UTF_8));
        }

        long divergences = 0;
        long comparaisons = 0;

        for (String regex : Fixtures.regexs()) {
            AstI ast = new Parser(regex).parse();
            Matcher naif = new MatcherNaif(new ConstructionAutomate(ast).getAfdMinimal());
            Matcher uneP = new MatcherUnePasse(
                new ConstructionAutomate(MatcherUnePasse.envelopper(ast)).getAfdMinimal());

            for (String s : lignes) {
                if (naif.contientMotif(s) != uneP.contientMotif(s)) {
                    divergences++;
                }
                comparaisons++;
            }
        }

        System.out.println("AutomatesProjet1Test : " + comparaisons + " comparaisons, " + divergences + " divergences");
        assertEquals(0, divergences);
    }
}
