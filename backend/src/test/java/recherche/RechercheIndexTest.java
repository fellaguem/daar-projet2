package recherche;

import static org.junit.jupiter.api.Assertions.assertEquals;

import index.Index;
import java.util.List;
import org.junit.jupiter.api.Test;

class RechercheIndexTest {

    // livre 0 : whale x2, white x1, sea x1 ; livre 1 : whale x1, white x3 ; livre 2 : sea x2
    private static RechercheIndex moteur() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(100, "whale white whale sea");
        c.ajouterLivre(200, "white white white whale");
        c.ajouterLivre(300, "sea sea");
        return new RechercheIndex(c.construire());
    }

    @Test
    void unMotTrieParOccurrences() {
        assertEquals(List.of(new Resultat(0, 2), new Resultat(1, 1)), moteur().motCle("whale"));
        assertEquals(List.of(new Resultat(2, 2), new Resultat(0, 1)), moteur().motCle("sea"));
    }

    @Test
    void plusieursMotsEstUnET() {
        // "white" est dans les livres 0 et 1, "sea" dans 0 et 2 : seul le livre 0 a les deux
        assertEquals(List.of(new Resultat(0, 2)), moteur().motCle("white sea"));
    }

    @Test
    void scoreEgalSommeDesOccurrences() {
        // livre 1 : white 3 + whale 1 = 4 ; livre 0 : white 1 + whale 2 = 3
        assertEquals(List.of(new Resultat(1, 4), new Resultat(0, 3)), moteur().motCle("white whale"));
    }

    @Test
    void ordreEtMajusculesSansEffet() {
        assertEquals(moteur().motCle("white whale"), moteur().motCle("WHALE, White"));
    }

    @Test
    void motRepeteCompteUneFois() {
        assertEquals(moteur().motCle("whale"), moteur().motCle("whale whale"));
    }

    @Test
    void motAbsentOuRequeteSansMot() {
        assertEquals(List.of(), moteur().motCle("dolphin"));
        assertEquals(List.of(), moteur().motCle("whale dolphin")); // un mot absent : ET impossible
        assertEquals(List.of(), moteur().motCle(""));
        assertEquals(List.of(), moteur().motCle("a")); // mot d'une lettre : rien à chercher
    }

    @Test
    void motsSansLivreCommunDonnentRienET() {
        // "sea" (livres 0 et 2) et un mot qui n'est que dans le livre 1 : aucun livre commun
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(1, "alpha beta");
        c.ajouterLivre(2, "gamma delta");
        assertEquals(List.of(), new RechercheIndex(c.construire()).motCle("alpha gamma"));
    }
}
