package corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import index.ConstructionIndex;
import index.Index;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RetraitDoublonsTest {

    // 7 livres : 1, 3 et 5 ont le même texte, les autres n'ont aucun mot en commun.
    // Avec 7 livres, un mot est vide s'il est dans plus de 3 livres : les mots de 1, 3, 5 restent.
    private static final String[] TEXTES = {
        "whale ship sea ocean captain harpoon",
        "garden flower rose tulip",
        "whale ship sea ocean captain harpoon",
        "castle king queen knight",
        "whale ship sea ocean captain harpoon",
        "mountain river forest valley",
        "piano violin music concert",
    };

    private static Path creer(Path dossier) throws IOException {
        Path livres = dossier.resolve("livres");
        Files.createDirectories(livres);
        StringBuilder meta = new StringBuilder();
        for (int id = 1; id <= TEXTES.length; id++) {
            Files.writeString(livres.resolve(id + ".txt"), TEXTES[id - 1], StandardCharsets.UTF_8);
            meta.append(id).append("\tTitre ").append(id).append("\tAuteur\ten\thttps://www.gutenberg.org/ebooks/")
                .append(id).append("\t10000\n");
        }
        Files.writeString(dossier.resolve("metadonnees.tsv"), meta, StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("rejetes.tsv"), "99\ttrop court (12 mots)\n", StandardCharsets.UTF_8);
        return dossier;
    }

    @Test
    void gardeLePlusPetitIdentifiant(@TempDir Path d) throws IOException {
        creer(d);
        Index index = ConstructionIndex.depuisDossier(d.resolve("livres"));
        List<RetraitDoublons.Retrait> r = RetraitDoublons.chercher(index, RetraitDoublons.SEUIL_DEFAUT);
        // 3 paires (1-3, 1-5, 3-5) mais deux livres retirés, chacun une seule fois
        assertEquals(2, r.size());
        assertEquals(new RetraitDoublons.Retrait(3, 1, 0.0), r.get(0));
        assertEquals(new RetraitDoublons.Retrait(5, 1, 0.0), r.get(1));
    }

    @Test
    void aucunDoublonSousUnSeuilNul(@TempDir Path d) throws IOException {
        creer(d);
        Index index = ConstructionIndex.depuisDossier(d.resolve("livres"));
        assertEquals(List.of(), RetraitDoublons.chercher(index, 0.0));
    }

    @Test
    void appliquerDeplaceEtNoteSansRienSupprimer(@TempDir Path d) throws IOException {
        creer(d);
        Index index = ConstructionIndex.depuisDossier(d.resolve("livres"));
        RetraitDoublons.appliquer(d, RetraitDoublons.chercher(index, RetraitDoublons.SEUIL_DEFAUT));

        assertFalse(Files.exists(d.resolve("livres/3.txt")));
        assertFalse(Files.exists(d.resolve("livres/5.txt")));
        assertEquals(TEXTES[2], Files.readString(d.resolve("doublons/3.txt"), StandardCharsets.UTF_8));
        assertTrue(Files.exists(d.resolve("doublons/5.txt")));
        assertTrue(Files.exists(d.resolve("livres/1.txt")));

        Metadonnees meta = Metadonnees.charger(d.resolve("metadonnees.tsv"));
        assertEquals(5, meta.taille());
        assertEquals(null, meta.livre(3));
        assertEquals("Titre 1", meta.livre(1).titre());

        List<String> rejetes = Files.readAllLines(d.resolve("rejetes.tsv"), StandardCharsets.UTF_8);
        assertEquals("99\ttrop court (12 mots)", rejetes.get(0)); // l'ancien contenu est gardé
        assertEquals("3\tdoublon de 1 (distance de Jaccard 0.0000)", rejetes.get(1));
        assertEquals("5\tdoublon de 1 (distance de Jaccard 0.0000)", rejetes.get(2));

        // le nouvel index n'a plus les doublons
        assertEquals(5, ConstructionIndex.depuisDossier(d.resolve("livres")).nbLivres());
    }
}
