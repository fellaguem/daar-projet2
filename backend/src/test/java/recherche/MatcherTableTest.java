package recherche;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import automate.Afd;
import automate.ConstructionAutomate;
import experimentation.MatcherUnePasse;
import index.Tokeniseur;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import parser.Parser;
import projet1.Fixtures;

// MatcherTable doit donner exactement les mêmes réponses que MatcherUnePasse (projet 1).
class MatcherTableTest {

    private static final List<String> REGEXS = List.of(
        "whale", "wh(a|e)le", "s(a|g|r)*on", "a.*b", ".*ing", "(sh|wh)(a|o)(l|r)(e|k)", "q.*z",
        "ab", "a|b", "(ab)*c", "a+b?c", "[a-c]x", "[aeiou][aeiou][aeiou]", "e.e.e", "...",
        "é", "caf(é|e)", "na.ve", "x(yz)+", "(a|b)*abb");

    private static Afd afd(String regex, boolean sousChaine) {
        var ast = new Parser(regex).parse();
        return new ConstructionAutomate(sousChaine ? MatcherUnePasse.envelopper(ast) : ast,
            RechercheRegex.ETATS_MAX).getAfdMinimal();
    }

    // mots des textes du projet 1, plus des mots au hasard (accents, majuscules, chiffres, ponctuation)
    private static List<String> mots() throws Exception {
        Set<String> mots = new LinkedHashSet<>();
        for (Path f : Fixtures.textes()) {
            mots.addAll(Tokeniseur.mots(Files.readString(f, StandardCharsets.UTF_8)));
        }
        Random hasard = new Random(7);
        String lettres = "abcehilnoqrswxyzéèçAB1'- ";
        for (int i = 0; i < 5000; i++) {
            StringBuilder s = new StringBuilder();
            int n = hasard.nextInt(12);
            for (int k = 0; k < n; k++) {
                s.append(lettres.charAt(hasard.nextInt(lettres.length())));
            }
            mots.add(s.toString());
        }
        return new ArrayList<>(mots);
    }

    @Test
    void memesReponsesQueMatcherUnePasse() throws Exception {
        List<String> mots = mots();
        assertTrue(mots.size() > 10_000);
        for (String regex : REGEXS) {
            for (boolean sousChaine : new boolean[] {true, false}) {
                Afd a = afd(regex, sousChaine);
                MatcherUnePasse reference = new MatcherUnePasse(a);
                MatcherTable table = new MatcherTable(a);
                for (String mot : mots) {
                    assertEquals(reference.contientMotif(mot), table.contientMotif(mot),
                        "regex " + regex + (sousChaine ? " (sous-chaîne)" : "") + ", mot « " + mot + " »");
                }
            }
        }
    }

    // mot entier : même réponse que java.util.regex (Pattern.matches) ; sous-chaîne : même réponse que find()
    @Test
    void memesReponsesQueJavaUtilRegex() throws Exception {
        List<String> mots = mots();
        for (String regex : REGEXS) {
            Pattern p = Pattern.compile(regex);
            MatcherTable entier = new MatcherTable(afd(regex, false));
            MatcherTable sousChaine = new MatcherTable(afd(regex, true));
            for (String mot : mots) {
                assertEquals(p.matcher(mot).matches(), entier.reconnait(mot), "regex " + regex + ", mot « " + mot + " »");
                assertEquals(p.matcher(mot).find(), sousChaine.contientMotif(mot), "regex " + regex + ", mot « " + mot + " »");
            }
        }
    }

    @Test
    void motEntier() {
        MatcherTable m = new MatcherTable(afd("wh(a|e)le", false));
        assertTrue(m.reconnait("whale"));
        assertTrue(m.reconnait("whele"));
        assertFalse(m.reconnait("narwhale"));
        assertFalse(m.reconnait("whales"));
        assertFalse(m.reconnait(""));

        MatcherTable tout = new MatcherTable(afd(".*", false));
        assertTrue(tout.reconnait(""));
        assertTrue(tout.reconnait("anything"));
    }

    @Test
    void exemples() {
        MatcherTable m = new MatcherTable(afd("wh(a|e)le", true));
        assertTrue(m.contientMotif("whale"));
        assertTrue(m.contientMotif("narwhale"));
        assertTrue(m.contientMotif("whelek"));
        assertFalse(m.contientMotif("whole"));
        assertFalse(m.contientMotif(""));

        // sans enveloppe : il faut lire la regex dès le début du mot
        MatcherTable debut = new MatcherTable(afd("ab", false));
        assertTrue(debut.contientMotif("abc"));
        assertFalse(debut.contientMotif("cab"));
    }
}
