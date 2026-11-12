
package experimentation;

import ast.AstFactory;
import ast.interfaces.AstI;
import automate.Afd;
import automate.Etat;

// Cette classe permet de chercher le motif en parcourant la ligne une seule fois
public class MatcherUnePasse implements Matcher {

    private final Afd automate;

    public MatcherUnePasse(Afd automate) {
        this.automate = automate;
    }

    // on transforme notre regex R en .*R pour pouvoir chercher le motif n'importe ou
    public static AstI envelopper(AstI r) {
        return AstFactory.createConcat(
            AstFactory.createRepeat(AstFactory.createDot()),
            r);
    }

    // on lit les caracteres un par un sans recommencer depuis chaque position
    @Override
    public boolean contientMotif(String ligne) {

        Etat e = automate.getInitial();

        // si l'initial est final alors le mot vide est accepté
        if (automate.getFinaux().contains(e)) {
            return true;
        }

        int n = ligne.length();

        for (int i = 0; i < n; i++) {

            e = automate.suivant(e, ligne.charAt(i));

            // si aucune transition existe on arrete la recherche
            if (e == null) {
                return false;
            }

            // un etat final veut dire qu'on a trouvé notre motif
            if (automate.getFinaux().contains(e)) {
                return true;
            }
        }

        return false;
    }
}
