package experimentation;

import java.io.BufferedReader;
import java.io.IOException;

public class FileSearcher {

    private final Matcher matcher;

    public FileSearcher(Matcher matcher) {
        this.matcher = matcher;
    }

    // on lit le texte ligne par ligne et on garde celles qui contienent le motif
    public ResultatRecherche rechercher(BufferedReader lect)
            throws IOException {

        ResultatRecherche res = new ResultatRecherche();

        String ligne;

        // readLine enleve deja le retour a la ligne, donc le matcher voit la ligne sans son \n
        
        while ((ligne = lect.readLine()) != null) {
            res.ajouterLigneLue(ligne, matcher.contientMotif(ligne));
        }

        return res;
    }
}
