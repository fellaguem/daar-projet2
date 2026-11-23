package index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

// Construit l'index des livres, une seule fois avant de lancer le serveur.
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

        long t0 = System.nanoTime();
        Index ix = depuisDossier(livres);
        long tBuild = (System.nanoTime() - t0) / 1_000_000;

        ix.sauver(sortie);
        long tAll = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("Livres : " + ix.nbLivres());
        System.out.println("Mots différents : " + ix.nbMots());
        System.out.println("Postings (couples mot-livre) : " + ix.nbPostings());
        System.out.println("Construction : " + tBuild + " ms, avec sauvegarde : " + tAll + " ms");
        System.out.println("Fichier " + sortie + " : " + Files.size(sortie) / 1024 / 1024 + " Mo");

        if (chercher != null) {
            afficher(ix, Tokeniseur.mots(chercher));
        }
    }

    // On garde les livres dans l'ordre de leur ID.
    public static Index depuisDossier(Path dossier) throws IOException {
        List<Path> docs;
        try (Stream<Path> flux = Files.list(dossier)) {
            docs = flux.filter(p -> p.getFileName().toString().matches("\\d+\\.txt"))
                    .sorted((a, b) -> Integer.compare(id(a), id(b)))
                    .toList();
        }

        Index.Constructeur c = new Index.Constructeur();
        for (Path f : docs) {
            c.ajouterLivre(id(f), Files.readString(f, StandardCharsets.UTF_8));
        }
        return c.construire();
    }

    private static int id(Path f) {
        String n = f.getFileName().toString();
        return Integer.parseInt(n.substring(0, n.length() - ".txt".length()));
    }

    // controle de la ou les livres où le mot revient le plus.
    private static void afficher(Index ix, List<String> mots) {
        if (mots.isEmpty())
            return;

        String mot = mots.get(0);
        int m = ix.trouver(mot);
        if (m < 0) {
            System.out.println("« " + mot + " » : absent de l'index");
            return;
        }
        System.out.println("« " + mot + " » : " + (ix.fin(m) - ix.debut(m)) + " livres");

        Integer[] pos = new Integer[ix.fin(m) - ix.debut(m)];
        for (int j = 0; j < pos.length; j++) {
            pos[j] = ix.debut(m) + j;
        }
        java.util.Arrays.sort(pos,
                (a, b) -> Integer.compare(ix.occurrencesDuPosting(b), ix.occurrencesDuPosting(a)));

        for (int j = 0; j < Math.min(10, pos.length); j++) {
            int p = pos[j]; // posting du livre
            System.out.println("  livre " + ix.idLivre(ix.livreDuPosting(p))
                    + " : " + ix.occurrencesDuPosting(p) + " occurrences");
        }
    }
}
