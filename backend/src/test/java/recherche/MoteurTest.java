package recherche;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphe.PageRank;
import index.Index;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import recherche.Moteur.LivreTrouve;

class MoteurTest {

    @TempDir
    static Path dossier;

    static Moteur moteur;
    static Index index;
    static Map<Integer, Double> pagerank = new HashMap<>();

    @BeforeAll
    static void preparer() throws IOException {
        CorpusDeTest.creer(dossier);
        moteur = Moteur.charger(dossier);
        index = Index.charger(dossier.resolve("index.bin"));
        PageRank.Scores s = PageRank.charger(dossier.resolve("pagerank.bin"));
        for (int i = 0; i < s.idsLivres().length; i++) {
            pagerank.put(s.idsLivres()[i], s.scores()[i]);
        }
    }

    private static List<Integer> ids(List<LivreTrouve> l) {
        return l.stream().map(LivreTrouve::id).toList();
    }

    private static List<Integer> ids(Moteur.Reponse r) {
        return ids(r.resultats());
    }

    // score attendu, calculé à la main à partir de l'index et du PageRank
    private static double score(int id, String... motsUtiles) {
        int livre = -1;
        for (int i = 0; i < index.nbLivres(); i++) {
            if (index.idLivre(i) == id) {
                livre = i;
            }
        }
        int occ = 0;
        for (String mot : motsUtiles) {
            occ += index.occurrences(index.trouver(mot), livre);
        }
        return (double) occ / index.longueurLivre(livre) * pagerank.get(id);
    }

    @Test
    void motVideDetecte() {
        assertTrue(moteur.estMotVide("the"));      // dans 6 livres sur 6
        assertTrue(!moteur.estMotVide("sea"));     // dans 3 livres sur 6 : pas plus de 50 %
        assertTrue(!moteur.estMotVide("whale"));
    }

    @Test
    void classementParProduit() {
        Moteur.Reponse r = moteur.chercherMotCle("whale", null);
        assertEquals("motcle", r.type());
        assertNull(r.nbMotsTrouves());
        for (LivreTrouve t : r.resultats()) {
            assertEquals(score(t.id(), "whale"), t.score(), 1e-12, "livre " + t.id());
        }
        // trié par score décroissant
        for (int i = 1; i < r.resultats().size(); i++) {
            assertTrue(r.resultats().get(i - 1).score() >= r.resultats().get(i).score());
        }
        assertEquals(List.of(1, 2, 3), ids(r).stream().sorted().toList());
    }

    @Test
    void motVideGardeDansLeETMaisPasDansLeScore() {
        Moteur.Reponse r = moteur.chercherMotCle("the whale", null);
        // ET : seuls les livres avec "whale" (tous ont "the")
        assertEquals(List.of(1, 2, 3), ids(r).stream().sorted().toList());
        for (LivreTrouve t : r.resultats()) {
            assertEquals(score(t.id(), "whale"), t.score(), 1e-12, "seul whale compte, livre " + t.id());
        }
        // les occurrences affichées comptent tous les mots : livre 1 = 8 "the" + 1 "whale"
        LivreTrouve un = r.resultats().stream().filter(t -> t.id() == 1).findFirst().orElseThrow();
        assertEquals(9, un.occurrences());
        // même ordre que pour "whale" seul
        assertEquals(ids(moteur.chercherMotCle("whale", null)), ids(r));
    }

    @Test
    void requeteDeMotsVidesSeulement() {
        Moteur.Reponse r = moteur.chercherMotCle("the", null);
        assertEquals(6, r.resultats().size());
        for (LivreTrouve t : r.resultats()) {
            assertEquals(score(t.id(), "the"), t.score(), 1e-12);
        }
    }

    @Test
    void triParOccurrencesEtParPagerank() {
        // "the whale" : occurrences totales 9 (livre 1), 4 (livre 2), 2 (livre 3)
        assertEquals(List.of(1, 2, 3), ids(moteur.chercherMotCle("the whale", "occurrences")));

        List<LivreTrouve> parPr = moteur.chercherMotCle("the", "pagerank").resultats();
        for (int i = 1; i < parPr.size(); i++) {
            assertTrue(parPr.get(i - 1).pagerank() >= parPr.get(i).pagerank());
        }
        // le score affiché reste le produit, quel que soit le tri
        for (LivreTrouve t : parPr) {
            assertEquals(score(t.id(), "the"), t.score(), 1e-12);
        }
        assertEquals(moteur.chercherMotCle("whale", null).resultats(),
            moteur.chercherMotCle("whale", "PRODUIT").resultats());
    }

    @Test
    void suggestionsVoisinsDesTroisPremiersSaufEuxMemes() {
        // "ship ocean" : livres 2 et 3 ; leur voisin commun est le livre 1 (distance 0,25 au livre 2)
        Moteur.Reponse r = moteur.chercherMotCle("ship ocean", null);
        assertEquals(List.of(2, 3), ids(r).stream().sorted().toList());
        assertEquals(List.of(1), r.suggestions().stream().map(Moteur.LivreCourt::id).toList());

        // "whale" : les 3 premiers sont 1, 2, 3, qui ne sont voisins qu'entre eux
        assertEquals(List.of(), moteur.chercherMotCle("whale", null).suggestions());

        // "tulip" : livre 4, son voisin est le livre 5
        assertEquals(List.of(5), moteur.chercherMotCle("tulip", null).suggestions().stream()
            .map(Moteur.LivreCourt::id).toList());

        // "castle" : le livre 6 est isolé, aucune suggestion
        assertEquals(List.of(), moteur.chercherMotCle("castle", null).suggestions());
    }

    @Test
    void rechercheParRegex() {
        Moteur.Reponse r = moteur.chercherRegex("wha", "index", null);
        assertEquals("regex", r.type());
        assertEquals(1, r.nbMotsTrouves());
        assertEquals(List.of("whale"), r.motsTrouves());
        assertEquals(ids(moteur.chercherMotCle("whale", null)), ids(r));

        // "the" (vide) et "whale" trouvés : seul whale compte dans le score ;
        // les livres 4, 5, 6 n'ont que "the" : score 0, en dernier
        Moteur.Reponse r2 = moteur.chercherRegex("(the|whale)", null, null);
        assertEquals(6, r2.resultats().size());
        for (LivreTrouve t : r2.resultats()) {
            assertEquals(score(t.id(), "whale"), t.score(), 1e-12);
        }
        assertEquals(List.of(4, 5, 6), ids(r2).subList(3, 6).stream().sorted().toList());

        // seulement des mots vides trouvés : on les compte
        Moteur.Reponse r3 = moteur.chercherRegex("th", "index", null);
        for (LivreTrouve t : r3.resultats()) {
            assertEquals(score(t.id(), "the"), t.score(), 1e-12);
        }
    }

    @Test
    void requetesRefusees() {
        assertThrows(RequeteInvalide.class, () -> moteur.chercherMotCle("", null));
        assertThrows(RequeteInvalide.class, () -> moteur.chercherMotCle("a ... !", null)); // rien à chercher
        assertThrows(RequeteInvalide.class, () -> moteur.chercherMotCle(null, null));
        assertThrows(RequeteInvalide.class, () -> moteur.chercherMotCle("whale", "titre"));
        assertThrows(RequeteInvalide.class, () -> moteur.chercherRegex("(wha", "index", null));
        assertThrows(RequeteInvalide.class, () -> moteur.chercherRegex("a*", "index", null));
        assertThrows(RequeteInvalide.class, () -> moteur.chercherRegex("wha", "texte", null));
        assertThrows(RequeteInvalide.class, () -> moteur.chercherRegex("wha", "autre", null));
        assertThrows(RequeteInvalide.class, () -> moteur.chercherRegex("wha", "index", "titre"));
    }

    @Test
    void motAbsentAucunResultat() {
        Moteur.Reponse r = moteur.chercherMotCle("dolphin", null);
        assertEquals(List.of(), r.resultats());
        assertEquals(List.of(), r.suggestions());
    }

    @Test
    void pageDUnLivre() {
        Moteur.Fiche f = moteur.livre(2);
        assertEquals("Baleine longue", f.titre());
        assertEquals("Auteur 2", f.auteur());
        assertEquals("https://www.gutenberg.org/ebooks/2", f.urlSource());
        assertEquals("The whale whale whale, ship sea ocean.", f.extrait());
        // voisins du plus proche au plus loin : livre 3 (0,2) puis livre 1 (0,25)
        assertEquals(List.of(3, 1), f.voisins().stream().map(Moteur.LivreCourt::id).toList());

        assertEquals(List.of(), moteur.livre(6).voisins()); // isolé
        assertThrows(LivreIntrouvable.class, () -> moteur.livre(999));
    }

    @Test
    void tempsMesure() {
        assertTrue(moteur.chercherMotCle("whale", null).tempsMs() >= 0);
    }

    @Test
    void extraitCoupeSurUnMotAvecParagraphes(@TempDir Path d) throws IOException {
        Path f = d.resolve("1.txt");
        Files.writeString(f, "\r\n\r\nPremier paragraphe\r\nsur deux lignes.\r\n\r\n\r\nDeuxième.\r\n", StandardCharsets.UTF_8);
        assertEquals("Premier paragraphe sur deux lignes.\n\nDeuxième.", Moteur.extrait(f));

        Files.writeString(f, "mot ".repeat(1000), StandardCharsets.UTF_8);
        String e = Moteur.extrait(f);
        assertTrue(e.endsWith(" …"));
        assertTrue(e.length() <= Moteur.TAILLE_EXTRAIT + 2);
        assertTrue(e.substring(0, e.length() - 2).endsWith("mot"));

        assertEquals("", Moteur.extrait(d.resolve("absent.txt")));
    }

    @Test
    void refuseDesFichiersIncoherents(@TempDir Path d) throws IOException {
        CorpusDeTest.creer(d);
        // PageRank d'un autre corpus
        PageRank.sauver(d.resolve("pagerank.bin"), new int[] {1, 2, 3}, new double[] {0.3, 0.3, 0.4});
        IOException e = assertThrows(IOException.class, () -> Moteur.charger(d));
        assertTrue(e.getMessage().contains("mêmes livres"));

        Files.delete(d.resolve("graphe.bin"));
        assertThrows(IOException.class, () -> Moteur.charger(d));
    }
}
