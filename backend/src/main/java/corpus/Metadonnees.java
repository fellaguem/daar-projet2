package corpus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Métadonnées des livres, lues dans metadonnees.tsv (écrit par TelechargeCorpus) :
// une ligne par livre : id, titre, auteur, langue, urlSource, nbMots (séparés par des tabulations)
public final class Metadonnees {

    public record Livre(int id, String titre, String auteur, String langue, String urlSource, int nbMots) {}

    private final Map<Integer, Livre> livres;

    private Metadonnees(Map<Integer, Livre> livres) {
        this.livres = livres;
    }

    public static Metadonnees charger(Path fichier) throws IOException {
        Map<Integer, Livre> livres = new HashMap<>();
        for (String ligne : Files.readAllLines(fichier, StandardCharsets.UTF_8)) {
            String[] c = ligne.split("\t", -1);
            if (c.length < 6) {
                continue; // ligne incomplète
            }
            try {
                int id = Integer.parseInt(c[0].trim());
                livres.put(id, new Livre(id, c[1], c[2], c[3], c[4], Integer.parseInt(c[5].trim())));
            } catch (NumberFormatException e) {
                // ligne illisible : ignorée
            }
        }
        return new Metadonnees(livres);
    }

    // null si l'identifiant est inconnu
    public Livre livre(int id) {
        return livres.get(id);
    }

    public int taille() {
        return livres.size();
    }
}
