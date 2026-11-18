package recherche;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import index.Index;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import projet1.Fixtures;
import recherche.RechercheRegex.RegexInvalide;

class MesureCorpusTest {

    // La recherche dans le texte complet (mesures du rapport) doit trouver exactement les mêmes livres,
    // avec les mêmes nombres d'occurrences et dans le même ordre, que la recherche dans l'index (mot entier).
    @Test
    void texteCompletEtIndexTrouventLesMemesLivres() throws Exception {
        List<String> textes = new ArrayList<>();
        Index.Constructeur c = new Index.Constructeur();
        int id = 1;
        for (Path fichier : Fixtures.textes()) {
            String texte = Files.readString(fichier, StandardCharsets.UTF_8);
            textes.add(texte);
            c.ajouterLivre(id++, texte);
        }
        RechercheRegex index = new RechercheRegex(c.construire());
        String[] tableau = textes.toArray(new String[0]);

        int regexTestees = 0;
        for (String regex : Fixtures.regexs()) {
            RechercheRegex.Reponse attendu;
            try {
                attendu = index.chercher(regex, true);
            } catch (RegexInvalide e) {
                continue; // refusée par l'application (automate trop grand, regex mal écrite)
            }
            assertEquals(attendu.resultats(), MesureCorpus.chercherDansTexte(tableau, regex), "regex " + regex);
            regexTestees++;
        }
        assertTrue(regexTestees > 20);
    }

    @Test
    void moyenneEtEcartTypeDEchantillon() {
        double[] t = {2, 4, 4, 4, 5, 5, 7, 9};
        assertEquals(5.0, MesureCorpus.moyenne(t), 1e-12);
        assertEquals(Math.sqrt(32.0 / 7), MesureCorpus.ecartType(t), 1e-12);
        assertEquals(0.0, MesureCorpus.ecartType(new double[] {3}), 1e-12);
    }
}
