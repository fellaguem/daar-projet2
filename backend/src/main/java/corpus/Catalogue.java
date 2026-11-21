package corpus;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


/*
 * Lecture du catalogue officiel de Project Gutenberg (pg_catalog.csv).
 *
 * pg_catalog.csv
 * -> livres de type Text dans une seule langue ("en")
 * -> une Entree par livre (id, titre, auteur, langue)
 */


public final class Catalogue {


    private Catalogue() {}

    // une ligne du catalogue (seulement les champs utiles)
    public record Entree(int id, String titre, String auteur, String langue) {}

    // garde les livres de type "Text" dont la langue est exactement langue ("en")
    // un livre en plusieurs langues ("en; fr") est ecarte
    public static List<Entree> lire(Reader lecteur, String langue) throws IOException {

        List<Entree> entrees = new ArrayList<>();
        List<String> ligne = lireLigne(lecteur);

        // la premiere ligne donne le nom des colonnes
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
                continue; // ligne incomplete
            }
            if (!"Text".equals(ligne.get(cType)) || !langue.equals(ligne.get(cLangue).trim())) {
                continue;
            }

            try {
                int id = Integer.parseInt(ligne.get(cId).trim());
                entrees.add(new Entree(id, nettoyer(ligne.get(cTitre)), nettoyer(ligne.get(cAuteur)),
                    ligne.get(cLangue).trim()));
            } catch (NumberFormatException e) {
                // id illisible : on ignore la ligne
            }
        }

        return entrees;
    }

    // les titres peuvent contenir des retours a la ligne et des tabulations
    private static String nettoyer(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    // lit une ligne de CSV (norme RFC 4180) : champs entre guillemets, "" pour un guillemet,
    // retours a la ligne possibles dans un champ entre guillemets
    // renvoie null a la fin du fichier
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
