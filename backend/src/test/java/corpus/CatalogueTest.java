package corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogueTest {

    private static final String CSV =
        "Text#,Type,Issued,Title,Language,Authors,Subjects,LoCC,Bookshelves\r\n"
        + "1,Text,1971-12-01,The Declaration,en,\"Jefferson, Thomas, 1743-1826\",Politics,E201,\r\n"
        + "2,Text,1972-12-01,\"Titre avec \"\"guillemets\"\", virgule\",en,Auteur A,,PS,\r\n"
        + "3,Text,1973-01-01,\"Titre sur\ndeux lignes\",en,Auteur B,,PS,\r\n"
        + "4,Sound,1973-01-01,Un enregistrement,en,Auteur C,,PS,\r\n"
        + "5,Text,1973-01-01,Un livre français,fr,Auteur D,,PS,\r\n"
        + "6,Text,1973-01-01,Deux langues,en; fr,Auteur E,,PS,\r\n"
        + "7,Text,1973-01-01,Dernier,en,Auteur F,,PS,";

    @Test
    void garde_seulement_les_textes_en_anglais() throws Exception {
        List<Catalogue.Entree> entrees = Catalogue.lire(new StringReader(CSV), "en");

        assertEquals(4, entrees.size());
        assertEquals(List.of(1, 2, 3, 7), entrees.stream().map(Catalogue.Entree::id).toList());
    }

    @Test
    void lit_les_champs_entre_guillemets() throws Exception {
        List<Catalogue.Entree> entrees = Catalogue.lire(new StringReader(CSV), "en");

        assertEquals("Jefferson, Thomas, 1743-1826", entrees.get(0).auteur());
        assertEquals("Titre avec \"guillemets\", virgule", entrees.get(1).titre());
        assertEquals("Titre sur deux lignes", entrees.get(2).titre());
        assertEquals("Auteur F", entrees.get(3).auteur()); // dernière ligne sans retour à la ligne
    }
}
