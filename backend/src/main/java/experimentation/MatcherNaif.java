
package experimentation;

import automate.Afd;
import automate.Etat;

// Cette classe permet de chercher un motif dans une ligne en essayant chaque position de depart
public class MatcherNaif implements Matcher {

    private final Afd automate;

    public MatcherNaif(Afd automate) {
        this.automate = automate;
    }

    // On parcours la ligne depuis chaque position jusqu'a trouver le motif
    @Override
    public boolean contientMotif(String ligne) {

        Etat initial = automate.getInitial();

        // si l'etat initial est final alors le mot vide est accepté
        if (automate.getFinaux().contains(initial)) {
            return true;
        }

        int n = ligne.length();

        for (int deb = 0; deb < n; deb++) {

            Etat e = initial;

            // essayer de lire les caracteres a partir de cette position
            for (int i = deb; i < n; i++) {

                e = automate.suivant(e, ligne.charAt(i));

                // si aucune transition existe on essaye la position suivante
                if (e == null) {
                    break;
                }

                // si on arrive à un etat final le motif est trouvé
                if (automate.getFinaux().contains(e)) {
                    return true;
                }
            }
        }
        return false;
    }
}
