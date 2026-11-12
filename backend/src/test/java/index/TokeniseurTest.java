package index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TokeniseurTest {

    @Test
    void minusculesEtPonctuationRetiree() {
        assertEquals(List.of("call", "me", "ishmael"), Tokeniseur.mots("Call me, ISHMAEL!"));
    }

    @Test
    void apostropheEtTiretSeparentLesMots() {
        assertEquals(List.of("don", "t", "well", "known"), Tokeniseur.mots("don't well-known"));
    }

    @Test
    void chiffresSontIgnores() {
        assertEquals(List.of("chapter", "the", "whale"), Tokeniseur.mots("Chapter 12: the 3 whale"));
    }

    @Test
    void accentsConservesEtMisEnMinuscules() {
        assertEquals(List.of("été", "à", "l", "école"), Tokeniseur.mots("Été à l'École"));
    }

    @Test
    void sauts_de_ligneEtTexteVide() {
        assertEquals(List.of("a", "b"), Tokeniseur.mots("a\n\r\n  b\t"));
        assertEquals(0, Tokeniseur.compter(""));
        assertEquals(0, Tokeniseur.compter(" ... 123 --- "));
    }

    @Test
    void compterEgalLaTailleDeLaListe() {
        String texte = "It was the best of times, it was the worst of times.";
        assertEquals(Tokeniseur.mots(texte).size(), Tokeniseur.compter(texte));
        assertEquals(12, Tokeniseur.compter(texte));
    }
}
