package parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ast.AstChar;
import ast.AstConcat;
import ast.AstOr;
import ast.AstRepeat;
import ast.AstVide;
import ast.interfaces.AstI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// Tests de la syntaxe ajoutée au projet 1 : + ? et classes [..]
// (la justesse sur des textes est vérifiée par input7.txt dans les tests du projet 1)
class ParserTest {

    private static AstI arbre(String regex) {
        return new Parser(regex).parse();
    }

    // les lettres d'une alternative a|b|c, dans l'ordre de l'arbre
    private static void lettres(AstI noeud, List<Character> sortie) {
        if (noeud instanceof AstChar c) {
            sortie.add(c.getValeur());
        } else {
            assertInstanceOf(AstOr.class, noeud);
            lettres(noeud.getFilsGauche(), sortie);
            lettres(noeud.getFilsDroit(), sortie);
        }
    }

    @Test
    void plusEstXSuiviDeXEtoile() {
        AstI a = arbre("a+");
        assertInstanceOf(AstConcat.class, a);
        assertInstanceOf(AstChar.class, a.getFilsGauche());
        assertInstanceOf(AstRepeat.class, a.getFilsDroit());
        assertInstanceOf(AstChar.class, a.getFilsDroit().getFilsGauche());
    }

    @Test
    void pointInterrogationEstXOuVide() {
        AstI a = arbre("a?");
        assertInstanceOf(AstOr.class, a);
        assertInstanceOf(AstChar.class, a.getFilsGauche());
        assertInstanceOf(AstVide.class, a.getFilsDroit());
    }

    @Test
    void operateursSAppliquentAuGroupe() {
        assertInstanceOf(AstConcat.class, arbre("(ab)+"));
        assertInstanceOf(AstOr.class, arbre("(ab)?"));
        // ab+ : seul le b est répété
        AstI a = arbre("ab+");
        assertInstanceOf(AstConcat.class, a);
        assertInstanceOf(AstChar.class, a.getFilsGauche());
        assertInstanceOf(AstConcat.class, a.getFilsDroit());
    }

    @Test
    void classeDeLettres() {
        List<Character> l = new ArrayList<>();
        lettres(arbre("[cab]"), l);
        assertEquals(List.of('a', 'b', 'c'), l); // triées, sans doublon
        l.clear();
        lettres(arbre("[aab]"), l);
        assertEquals(List.of('a', 'b'), l);
    }

    @Test
    void classeAUneSeuleLettreEstUneLettre() {
        assertInstanceOf(AstChar.class, arbre("[a]"));
    }

    @Test
    void plageEtMelange() {
        List<Character> l = new ArrayList<>();
        lettres(arbre("[a-d]"), l);
        assertEquals(List.of('a', 'b', 'c', 'd'), l);
        l.clear();
        lettres(arbre("[a-cx-z]"), l);
        assertEquals(List.of('a', 'b', 'c', 'x', 'y', 'z'), l);
        l.clear();
        lettres(arbre("[è-ë]"), l);
        assertEquals(List.of('è', 'é', 'ê', 'ë'), l); // les lettres accentuées marchent aussi
    }

    @Test
    void tiretAuDebutOuALaFinEstUneLettre() {
        List<Character> l = new ArrayList<>();
        lettres(arbre("[-a]"), l);
        assertEquals(List.of('-', 'a'), l);
        l.clear();
        lettres(arbre("[a-]"), l);
        assertEquals(List.of('-', 'a'), l);
    }

    @Test
    void crochetFermantSeulResteUneLettre() {
        assertInstanceOf(AstConcat.class, arbre("a]"));
    }

    @Test
    void erreursDeSyntaxe() {
        for (String regex : List.of("[]", "[abc", "[", "[a-", "[z-a]", "[^a]", "+a", "?a", "a|+", "(+a)", "(?a)")) {
            RuntimeException e = assertThrows(RuntimeException.class, () -> arbre(regex), regex);
            assertTrue(e.getMessage() != null && !e.getMessage().isBlank(), regex);
        }
    }

    @Test
    void classeTropGrandeRefusee() {
        assertThrows(RuntimeException.class, () -> arbre("[\u0000-￿]"));
    }

    @Test
    void lAncienneSyntaxeNeChangePas() {
        assertInstanceOf(AstConcat.class, arbre("ab"));
        assertInstanceOf(AstOr.class, arbre("a|b"));
        assertInstanceOf(AstRepeat.class, arbre("a*"));
        assertInstanceOf(AstRepeat.class, arbre("a**"));
        assertInstanceOf(AstVide.class, arbre(""));
    }
}
