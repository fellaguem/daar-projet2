package recherche;

import static org.junit.jupiter.api.Assertions.assertEquals;

import index.Index;
import java.util.List;
import org.junit.jupiter.api.Test;

// recherche par mot-clef et par regex sur un mini corpus
class RechercheTest {

    private Index corpus() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(100, "whale whaleboat whaler sea white");
        c.ajouterLivre(200, "whale whale white white white");
        c.ajouterLivre(300, "sea sailor sea");
        return c.construire();
    }

    @Test
    void motCleTrieParNombreOccurrences() {
        RechercheIndex r = new RechercheIndex(corpus());
        // le livre 1 a "whale" 2 fois, il passe devant le livre 0
        assertEquals(List.of(new Resultat(1, 2), new Resultat(0, 1)), r.motCle("whale"));
    }

    @Test
    void plusieursMotsCestUnET() {
        RechercheIndex r = new RechercheIndex(corpus());
        // seul le livre 0 contient a la fois "white" et "sea"
        assertEquals(List.of(new Resultat(0, 2)), r.motCle("white sea"));
        // si un des mots n'existe pas, il n'y a aucun resultat
        assertEquals(List.of(), r.motCle("whale dolphin"));
    }

    @Test
    void regexDansLesMotsDeLIndex() {
        RechercheRegex.Reponse rep = new RechercheRegex(corpus()).chercher("whal");

        // whale, whaleboat et whaler contiennent "whal"
        assertEquals(List.of("whale", "whaleboat", "whaler"), rep.motsTrouves());
        assertEquals(List.of(new Resultat(0, 3), new Resultat(1, 2)), rep.resultats());
    }

    @Test
    void regexEnMotEntier() {
        // en mot entier, "whale" ne doit plus attraper whaleboat ni whaler
        RechercheRegex.Reponse rep = new RechercheRegex(corpus()).chercher("whale", true);
        assertEquals(List.of("whale"), rep.motsTrouves());
    }
}
