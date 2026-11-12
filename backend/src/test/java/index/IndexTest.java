package index;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexTest {

    // trois petits livres dont on connaît les comptes à la main
    private static Index petitIndex() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(100, "The whale, the WHALE and the sea.");   // 7 mots : the x3, whale x2, and, sea
        c.ajouterLivre(200, "A white whale.");                       // 2 mots indexés ("a" est ignoré)
        c.ajouterLivre(300, "Nothing about sea creatures here sea"); // 6 mots : sea x2
        return c.construire();
    }

    @Test
    void compteLesLivresEtLesLongueurs() {
        Index index = petitIndex();
        assertEquals(3, index.nbLivres());
        assertEquals(100, index.idLivre(0));
        assertEquals(300, index.idLivre(2));
        assertEquals(7, index.longueurLivre(0));
        assertEquals(2, index.longueurLivre(1));
        assertEquals(6, index.longueurLivre(2));
    }

    @Test
    void motsD_une_seuleLettreNonIndexes() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(1, "Whale's tale: I don't know, a b c.");
        Index index = c.construire();

        // "whale's" -> "whale" + "s" ; "don't" -> "don" + "t" : les lettres isolées disparaissent
        assertEquals(java.util.List.of("don", "know", "tale", "whale"),
            java.util.stream.IntStream.range(0, index.nbMots()).mapToObj(index::mot).toList());
        assertEquals(4, index.longueurLivre(0));
        assertEquals(-1, index.trouver("a"));
        assertEquals(-1, index.trouver("s"));
    }

    @Test
    void requeteLueAvecLaMemeRegleQueLesLivres() {
        assertEquals(java.util.List.of("white", "whale"), Index.motsDeRequete("A White-Whale's"));
        assertEquals(java.util.List.of(), Index.motsDeRequete("a"));
        assertEquals(java.util.List.of(), Index.motsDeRequete(" ... "));
    }

    @Test
    void occurrencesParLivre() {
        Index index = petitIndex();
        int whale = index.trouver("whale");
        assertEquals(2, index.occurrences(whale, 0));
        assertEquals(1, index.occurrences(whale, 1));
        assertEquals(0, index.occurrences(whale, 2)); // pas dans le troisième livre

        int the = index.trouver("the");
        assertEquals(3, index.occurrences(the, 0));
    }

    @Test
    void postingsTriesParLivre() {
        Index index = petitIndex();
        int sea = index.trouver("sea");
        assertEquals(2, index.fin(sea) - index.debut(sea)); // livres 0 et 2
        assertEquals(0, index.livreDuPosting(index.debut(sea)));
        assertEquals(2, index.livreDuPosting(index.debut(sea) + 1));
    }

    @Test
    void motAbsentEtVocabulaireTrie() {
        Index index = petitIndex();
        assertEquals(-1, index.trouver("dolphin"));
        assertEquals(-1, index.trouver("Whale")); // l'index est en minuscules : la requête doit l'être aussi
        for (int m = 1; m < index.nbMots(); m++) {
            assertEquals(true, index.mot(m - 1).compareTo(index.mot(m)) < 0);
        }
    }

    @Test
    void sommeDesOccurrencesEgaleLaLongueurDesLivres() {
        Index index = petitIndex();
        int[] somme = new int[index.nbLivres()];
        for (int p = 0; p < index.nbPostings(); p++) {
            somme[index.livreDuPosting(p)] += index.occurrencesDuPosting(p);
        }
        for (int l = 0; l < index.nbLivres(); l++) {
            assertEquals(index.longueurLivre(l), somme[l]);
        }
    }

    @Test
    void sauvegardePuisChargementDonneLeMemeIndex(@TempDir Path dossier) throws IOException {
        Index avant = petitIndex();
        Path fichier = dossier.resolve("index.bin");
        avant.sauver(fichier);
        Index apres = Index.charger(fichier);

        assertEquals(avant.nbLivres(), apres.nbLivres());
        assertEquals(avant.nbMots(), apres.nbMots());
        assertEquals(avant.nbPostings(), apres.nbPostings());
        for (int l = 0; l < avant.nbLivres(); l++) {
            assertEquals(avant.idLivre(l), apres.idLivre(l));
            assertEquals(avant.longueurLivre(l), apres.longueurLivre(l));
        }
        for (int m = 0; m < avant.nbMots(); m++) {
            assertEquals(avant.mot(m), apres.mot(m));
            assertEquals(avant.debut(m), apres.debut(m));
        }
        for (int p = 0; p < avant.nbPostings(); p++) {
            assertEquals(avant.livreDuPosting(p), apres.livreDuPosting(p));
            assertEquals(avant.occurrencesDuPosting(p), apres.occurrencesDuPosting(p));
        }
    }

    @Test
    void refuseUnFichierQuiNEstPasUnIndex(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve("autre.bin");
        Files.write(fichier, new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
        assertThrows(IOException.class, () -> Index.charger(fichier));
    }

    @Test
    void indexVide() {
        Index index = new Index.Constructeur().construire();
        assertEquals(0, index.nbLivres());
        assertEquals(0, index.nbMots());
        assertEquals(-1, index.trouver("a"));
    }

    @Test
    void construitDepuisUnDossierDeLivres(@TempDir Path dossier) throws IOException {
        Files.writeString(dossier.resolve("12.txt"), "alpha beta beta");
        Files.writeString(dossier.resolve("3.txt"), "beta gamma");
        Files.writeString(dossier.resolve("notes.md"), "ignoré");

        Index index = ConstructionIndex.depuisDossier(dossier);

        assertEquals(2, index.nbLivres());
        assertArrayEquals(new int[] {3, 12}, new int[] {index.idLivre(0), index.idLivre(1)}); // tri par id
        assertEquals(2, index.occurrences(index.trouver("beta"), 1));
        assertEquals(1, index.occurrences(index.trouver("beta"), 0));
    }
}
