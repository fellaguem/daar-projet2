package recherche;

import automate.Afd;
import automate.Etat;
import experimentation.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// On met l'AFD dans des tableaux, c'est plus rapide pour tester les mots.
// Pas besoin de parcourir les maps à chaque caractère.
public final class MatcherTable implements Matcher {

    private static final int MORT = -1; // aucune transition

    private final int[] col;
    private final int nc;
    private final int[] tab;
    private final boolean[] fin;

    public MatcherTable(Afd afd) {
        List<Character> abc = new ArrayList<>();

        for (String s : afd.getAlphabet()) {
            if (s.length() == 1)
                abc.add(s.charAt(0));
        }

        int hors = abc.size(); // tout ce qui n'est pas dans l'alphabet
        nc = hors + 1;

        col = new int[Character.MAX_VALUE + 1];
        Arrays.fill(col, hors);
        for (int k = 0; k < abc.size(); k++) {
            col[abc.get(k)] = k;
        }

        // On numérote les états en partant du premier
        Map<Etat, Integer> nums = new HashMap<>();
        List<Etat> pile = new ArrayList<>();
        List<int[]> rangs = new ArrayList<>();

        nums.put(afd.getInitial(), 0);
        pile.add(afd.getInitial());

        for (int i = 0; i < pile.size(); i++) {
            int[] rang = new int[nc];

            for (int k = 0; k < nc; k++) {
                String sym = (k == hors) ? Afd.AUTRE : String.valueOf(abc.get(k));
                Etat dest = afd.transition(pile.get(i), sym);

                if (dest == null) {
                    rang[k] = MORT;
                } else {
                    Integer no = nums.get(dest);
                    if (no == null) {
                        no = pile.size();
                        nums.put(dest, no);
                        pile.add(dest);
                    }
                    rang[k] = no;
                }
            }
            rangs.add(rang);
        }

        tab = new int[pile.size() * nc];
        fin = new boolean[pile.size()];

        for (int i = 0; i < pile.size(); i++) {
            System.arraycopy(rangs.get(i), 0, tab, i * nc, nc);
            fin[i] = afd.getFinaux().contains(pile.get(i));
        }
    }

    @Override
    public boolean contientMotif(String ligne) {
        if (fin[0])
            return true;

        int e = 0;
        for (int i = 0; i < ligne.length(); i++) {
            e = tab[e * nc + col[ligne.charAt(i)]];
            if (e == MORT)
                return false; // plus de chemin

            if (fin[e])
                return true;
        }
        return false;
    }

    // Ici, il faut que le mot entier passe
    public boolean reconnait(String mot) {
        int e = 0;
        for (int i = 0; i < mot.length(); i++) {
            e = tab[e * nc + col[mot.charAt(i)]];
            if (e == MORT)
                return false;
        }

        return fin[e];
    }

    public int nbEtats() {
        return fin.length;
    }
}
