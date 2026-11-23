package corpus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


// les infos des livres, récupérées depuis metadonnees.tsv
// id, titre, auteur, langue, lien et nombre de mots
public final class Metadonnees {

  public record Livre(int id, String titre, String auteur, String langue, String urlSource, int nbMots) {}

    private final Map<Integer, Livre> livres;

  private Metadonnees(Map<Integer, Livre> livres) {
       this.livres = livres;
  }

    public static Metadonnees charger(Path fichier) throws IOException {

      Map<Integer,Livre> tbl = new HashMap<>();

        for(String ln : Files.readAllLines(fichier,StandardCharsets.UTF_8)) {

          String[] c=ln.split("\t",-1);
          if(c.length < 6) continue;  // ligne incomplète

            try {
              int n = Integer.parseInt(c[0].trim());

              tbl.put(n,new Livre(n,c[1], c[2],c[3], c[4],Integer.parseInt(c[5].trim())));
            }
            catch(NumberFormatException ex) {
                // valeur illisible, on passe
            }
        }

      return new Metadonnees(tbl);
    }

   // null si le livre n'existe pas
    public Livre livre(int id) {
        return livres.get(id);
    }

  public int taille() {
      return livres.size();
  }
}
