package corpus;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

// Lecture du catalogue officiel de Project Gutenberg (pg_catalog.csv)
public final class Catalogue {

    private Catalogue() {}

    // une ligne du catalogue : les champs dont on a besoin
    public record Entree(int id, String titre, String auteur, String langue) {}

    // Lit le CSV et garde les livres de type "Text" dont la langue est exactement langue ("en").
    // Un livre en plusieurs langues ("en; fr") est écarté.
    public static List<Entree> lire(Reader lecteur, String langue) throws IOException {
        List<Entree> entrees = new ArrayList<>();
        List<String> ligne = lireLigne(lecteur);

        // la première ligne donne le nom des colonnes
        int cId = ligne.indexOf("Text#");
        int cType = ligne.indexOf("Type");
        int cTitre = ligne.indexOf("Title");
        int cLangue = ligne.indexOf("Language");
        int cAuteur = ligne.indexOf("Authors");
        if (cId < 0 || cType < 0 || cTitre < 0 || cLangue < 0 || cAuteur < 0) {
            throw new IOException("En-tête du catalogue inattendu : " + ligne);
        }

        while ((ligne = lireLigne(lecteur)) != null) {
            if (ligne.size() <= Math.max(Math.max(cId, cType), Math.max(cTitre, Math.max(cLangue, cAuteur)))) {
                continue; // ligne incomplète
            }
            if (!"Text".equals(ligne.get(cType)) || !langue.equals(ligne.get(cLangue).trim())) {
                continue;
            }
            try {
                int id = Integer.parseInt(ligne.get(cId).trim());
                entrees.add(new Entree(id, nettoyer(ligne.get(cTitre)), nettoyer(ligne.get(cAuteur)),
                    ligne.get(cLangue).trim()));
            } catch (NumberFormatException e) {
                // identifiant illisible : on ignore la ligne
            }
        }
        return entrees;
    }

    // les titres peuvent contenir des retours à la ligne et des tabulations
    private static String nettoyer(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    // Lit une ligne de CSV (norme RFC 4180) : champs entre guillemets, "" pour un guillemet,
    // et retours à la ligne possibles à l'intérieur d'un champ entre guillemets.
    // Renvoie null à la fin du fichier.
    static List<String> lireLigne(Reader lecteur) throws IOException {
        List<String> champs = new ArrayList<>();
        StringBuilder champ = new StringBuilder();
        boolean entreGuillemets = false;
        boolean quelqueChose = false;
        int c;

        while ((c = lecteur.read()) != -1) {
            quelqueChose = true;
            if (entreGuillemets) {
                if (c == '"') {
                    lecteur.mark(1);
                    int suivant = lecteur.read();
                    if (suivant == '"') {
                        champ.append('"');
                    } else {
                        entreGuillemets = false;
                        if (suivant != -1) {
                            lecteur.reset();
                        }
                    }
                } else {
                    champ.append((char) c);
                }
            } else if (c == '"') {
                entreGuillemets = true;
            } else if (c == ',') {
                champs.add(champ.toString());
                champ.setLength(0);
            } else if (c == '\n') {
                champs.add(champ.toString());
                return champs;
            } else if (c != '\r') {
                champ.append((char) c);
            }
        }

        if (!quelqueChose) {
            return null;
        }
        champs.add(champ.toString());
        return champs;
    }
}
