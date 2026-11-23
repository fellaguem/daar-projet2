package corpus;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

// Lecture du catalogue Gutenberg, on garde juste les champs utiles.
public final class Catalogue {

    private Catalogue() {
    }

    public record Entree(int id, String titre, String auteur, String langue) {
    }

    // seuls les textes dans la langue demandée
    public static List<Entree> lire(Reader lecteur, String langue) throws IOException {

        List<Entree> ent = new ArrayList<>();
        List<String> lg = lireLigne(lecteur);

        // repérer les colonnes
        int ci = lg.indexOf("Text#");
        int ct = lg.indexOf("Type");
        int cTi = lg.indexOf("Title");
        int cl = lg.indexOf("Language");
        int ca = lg.indexOf("Authors");

        if (ci < 0 || ct < 0 || cTi < 0 || cl < 0 || ca < 0) {
            throw new IOException("En-tête du catalogue inattendu : " + lg);
        }

        while ((lg = lireLigne(lecteur)) != null) {

            if (lg.size() <= Math.max(Math.max(ci, ct), Math.max(cTi, Math.max(cl, ca)))) {
                continue; // pas assez de champs
            }

            if (!"Text".equals(lg.get(ct)) || !langue.equals(lg.get(cl).trim()))
                continue;

            try {
                int id = Integer.parseInt(lg.get(ci).trim());
                ent.add(new Entree(id, nettoyer(lg.get(cTi)), nettoyer(lg.get(ca)),
                        lg.get(cl).trim()));
            } catch (NumberFormatException e) {
                // identifiant illisible, tant pis
            }
        }

        return ent;
    }

    private static String nettoyer(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    // Lit un enregistrement CSV, même si un champ passe sur plusieurs lignes.
    static List<String> lireLigne(Reader lecteur) throws IOException {
        List<String> cols = new ArrayList<>();
        StringBuilder buf = new StringBuilder();

        boolean quote = false;
        boolean vu = false;
        int c;

        while ((c = lecteur.read()) != -1) {
            vu = true;

            if (quote) {
                if (c == '"') {
                    lecteur.mark(1);
                    int ap = lecteur.read();

                    if (ap == '"')
                        buf.append('"');
                    else {
                        quote = false;
                        if (ap != -1)
                            lecteur.reset();
                    }
                } else
                    buf.append((char) c);
            } else if (c == '"') {
                quote = true;
            } else if (c == ',') {
                cols.add(buf.toString());
                buf.setLength(0);
            } else if (c == '\n') {
                cols.add(buf.toString());
                return cols;
            } else if (c != '\r')
                buf.append((char) c);
        }

        if (!vu)
            return null;

        cols.add(buf.toString());
        return cols;
    }
}
