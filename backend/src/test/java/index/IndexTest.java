package index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class IndexTest {

    // 3 livres assez courts pour compter les mots a la main
    private Index petitIndex() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(100, "The whale, the WHALE and the sea.");
        c.ajouterLivre(200, "A white whale.");
        c.ajouterLivre(300, "Nothing about sea creatures here sea");
        return c.construire();
    }

    @Test
    void decoupageEnMots() {
        // tout en minuscules, la ponctuation saute
        assertEquals(List.of("call", "me", "ishmael"), Tokeniseur.mots("Call me, ISHMAEL!"));
        // les chiffres ne sont pas des mots
        assertEquals(List.of("chapter", "the", "whale"), Tokeniseur.mots("Chapter 12: the 3 whale"));
    }

    @Test
    void occurrencesDansChaqueLivre() {
        Index index = petitIndex();
        int whale = index.trouver("whale");

        assertEquals(2, index.occurrences(whale, 0)); // "whale" et "WHALE"
        assertEquals(1, index.occurrences(whale, 1));
        assertEquals(0, index.occurrences(whale, 2));
    }

    @Test
    void motsAbsentsOuTropCourts() {
        Index index = petitIndex();
        assertEquals(-1, index.trouver("dolphin"));
        // on n'indexe pas les mots d'une seule lettre, donc "a" n'y est pas
        assertEquals(-1, index.trouver("a"));
    }
}
