package recherche;

import graphe.Graphe;
import graphe.Jaccard;
import graphe.PageRank;
import index.ConstructionIndex;
import index.Index;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// Petit corpus de 6 livres, construit comme le vrai (index, graphe, PageRank, métadonnées, livres/),
// pour tester le moteur et l'API.
//
// "the" est dans les 6 livres : c'est un mot vide (plus de 50 %).
// Mots gardés pour Jaccard (sans "the") :
//   1 {whale, ship, sea}   2 {whale, ship, sea, ocean}   3 {whale, ship, sea, ocean, captain}
//   4 {garden, flower, rose, tulip}   5 {garden, flower, rose, daisy}   6 {castle, king, queen, knight}
// Distances : 1-2 0,25  1-3 0,4  2-3 0,2  4-5 0,4, toutes les autres 1.
// Seuil 0,5 : un triangle 1-2-3, une arête 4-5, le livre 6 isolé.
public final class CorpusDeTest {

    public static final double SEUIL = 0.5;

    private CorpusDeTest() {}

    public static Path creer(Path dossier) throws IOException {
        Path livres = dossier.resolve("livres");
        Files.createDirectories(livres);
        ecrire(livres, 1, "The the the the the the the the whale ship sea.");      // 11 mots, whale 1
        ecrire(livres, 2, "The whale whale whale, ship sea ocean.");                // 7 mots, whale 3
        ecrire(livres, 3, "The whale ship sea ocean captain.");                     // 6 mots, whale 1
        ecrire(livres, 4, "The garden flower rose tulip.");
        ecrire(livres, 5, "The garden flower rose daisy.");
        ecrire(livres, 6, "The castle king queen knight.");

        StringBuilder meta = new StringBuilder();
        String[] titres = {"Baleine courte", "Baleine longue", "Capitaine", "Jardin tulipe", "Jardin marguerite", "Chateau"};
        for (int id = 1; id <= 6; id++) {
            meta.append(id).append('\t').append(titres[id - 1]).append('\t').append("Auteur ").append(id)
                .append("\ten\thttps://www.gutenberg.org/ebooks/").append(id).append("\t10000\n");
        }
        Files.writeString(dossier.resolve("metadonnees.tsv"), meta, StandardCharsets.UTF_8);

        Index index = ConstructionIndex.depuisDossier(livres);
        index.sauver(dossier.resolve("index.bin"));

        int[] ids = new int[index.nbLivres()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = index.idLivre(i);
        }
        Graphe graphe = Graphe.depuisDistances(ids, new Jaccard(index, Jaccard.PART_MAX_DEFAUT).toutesLesDistances(), SEUIL);
        graphe.sauver(dossier.resolve("graphe.bin"));
        PageRank.sauver(dossier.resolve("pagerank.bin"), ids, PageRank.calculer(graphe).scores());
        return dossier;
    }

    private static void ecrire(Path livres, int id, String texte) throws IOException {
        Files.writeString(livres.resolve(id + ".txt"), texte, StandardCharsets.UTF_8);
    }
}
