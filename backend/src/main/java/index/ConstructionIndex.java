package index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

// Construit l'index depuis les livres téléchargés (programme lancé une fois, hors ligne du serveur).
//
//   java -cp backend/target/classes index.ConstructionIndex [--livres data/livres] [--sortie data/index.bin]
//                                                           [--chercher mot]
//
// Les livres sont les fichiers ID.txt du dossier. L'option --chercher affiche les 10 livres
// qui contiennent le mot le plus souvent (pour vérifier à la main).
public class ConstructionIndex {

    public static void main(String[] args) throws IOException {
        Path livres = Path.of("data/livres");
        Path sortie = Path.of("data/index.bin");
        String chercher = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--livres" -> livres = Path.of(args[++i]);
                case "--sortie" -> sortie = Path.of(args[++i]);
                case "--chercher" -> chercher = args[++i];
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    return;
                }
            }
        }

        long debut = System.nanoTime();
        Index index = depuisDossier(livres);
        long msConstruction = (System.nanoTime() - debut) / 1_000_000;

        index.sauver(sortie);
        long ms = (System.nanoTime() - debut) / 1_000_000;

        System.out.println("Livres : " + index.nbLivres());
        System.out.println("Mots différents : " + index.nbMots());
        System.out.println("Postings (couples mot-livre) : " + index.nbPostings());
        System.out.println("Construction : " + msConstruction + " ms, avec sauvegarde : " + ms + " ms");
        System.out.println("Fichier " + sortie + " : " + Files.size(sortie) / 1024 / 1024 + " Mo");

        if (chercher != null) {
            afficher(index, Tokeniseur.mots(chercher));
        }
    }

    // lit tous les fichiers ID.txt du dossier, par identifiant croissant
    public static Index depuisDossier(Path dossier) throws IOException {
        List<Path> fichiers;
        try (Stream<Path> stream = Files.list(dossier)) {
            fichiers = stream.filter(p -> p.getFileName().toString().matches("\\d+\\.txt"))
                .sorted((a, b) -> Integer.compare(id(a), id(b)))
                .toList();
        }

        Index.Constructeur constructeur = new Index.Constructeur();
        for (Path fichier : fichiers) {
            constructeur.ajouterLivre(id(fichier), Files.readString(fichier, StandardCharsets.UTF_8));
        }
        return constructeur.construire();
    }

    private static int id(Path fichier) {
        String nom = fichier.getFileName().toString();
        return Integer.parseInt(nom.substring(0, nom.length() - ".txt".length()));
    }

    private static void afficher(Index index, List<String> mots) {
        if (mots.isEmpty()) {
            return;
        }
        String mot = mots.get(0);
        int m = index.trouver(mot);
        if (m < 0) {
            System.out.println("« " + mot + " » : absent de l'index");
            return;
        }
        System.out.println("« " + mot + " » : " + (index.fin(m) - index.debut(m)) + " livres");

        Integer[] postings = new Integer[index.fin(m) - index.debut(m)];
        for (int i = 0; i < postings.length; i++) {
            postings[i] = index.debut(m) + i;
        }
        java.util.Arrays.sort(postings,
            (a, b) -> Integer.compare(index.occurrencesDuPosting(b), index.occurrencesDuPosting(a)));
        for (int i = 0; i < Math.min(10, postings.length); i++) {
            int p = postings[i];
            System.out.println("  livre " + index.idLivre(index.livreDuPosting(p))
                + " : " + index.occurrencesDuPosting(p) + " occurrences");
        }
    }
}
