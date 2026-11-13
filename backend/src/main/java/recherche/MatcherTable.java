package recherche;

import automate.Afd;
import automate.Etat;
import experimentation.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// L'AFD minimal du projet 1, recopié dans un tableau (état, caractère) -> état suivant, pour tester
// vite des centaines de milliers de mots.
//
// Même résultat que experimentation.MatcherUnePasse (vérifié par MatcherTableTest). Mais pour chaque
// caractère, MatcherUnePasse crée une chaîne, cherche dans un TreeSet, puis dans deux HashMap et un
// HashSet : environ 150 ms pour les 416 000 mots de l'index. Ici, une case de tableau par caractère.
// La copie est faite une fois par requête (quelques états, quelques symboles : négligeable).
public final class MatcherTable implements Matcher {

    private static final int MORT = -1; // pas de transition : le mot est rejeté

    private final int[] colonne;     // colonne de chaque caractère ; AUTRE pour ceux hors de l'alphabet
    private final int nbColonnes;
    private final int[] suivant;     // suivant[etat * nbColonnes + colonne], MORT s'il n'y a pas de transition
    private final boolean[] finaux;  // l'état initial a le numéro 0

    public MatcherTable(Afd afd) {
        // une colonne par symbole d'un caractère de l'alphabet, la dernière pour AUTRE
        List<Character> symboles = new ArrayList<>();
        for (String s : afd.getAlphabet()) {
            if (s.length() == 1) {
                symboles.add(s.charAt(0));
            }
        }
        int autre = symboles.size();
        nbColonnes = autre + 1;
        colonne = new int[Character.MAX_VALUE + 1];
        Arrays.fill(colonne, autre);
        for (int k = 0; k < symboles.size(); k++) {
            colonne[symboles.get(k)] = k;
        }

        // numéros des états : parcours en largeur depuis l'état initial (numéro 0)
        Map<Etat, Integer> numero = new HashMap<>();
        List<Etat> etats = new ArrayList<>();
        List<int[]> lignes = new ArrayList<>();
        numero.put(afd.getInitial(), 0);
        etats.add(afd.getInitial());
        for (int i = 0; i < etats.size(); i++) {
            int[] ligne = new int[nbColonnes];
            for (int k = 0; k < nbColonnes; k++) {
                String symbole = k == autre ? Afd.AUTRE : String.valueOf(symboles.get(k));
                Etat destination = afd.transition(etats.get(i), symbole);
                if (destination == null) {
                    ligne[k] = MORT;
                } else {
                    Integer n = numero.get(destination);
                    if (n == null) {
                        n = etats.size();
                        numero.put(destination, n);
                        etats.add(destination);
                    }
                    ligne[k] = n;
                }
            }
            lignes.add(ligne);
        }

        suivant = new int[etats.size() * nbColonnes];
        finaux = new boolean[etats.size()];
        for (int i = 0; i < etats.size(); i++) {
            System.arraycopy(lignes.get(i), 0, suivant, i * nbColonnes, nbColonnes);
            finaux[i] = afd.getFinaux().contains(etats.get(i));
        }
    }

    // même parcours que MatcherUnePasse.contientMotif
    @Override
    public boolean contientMotif(String ligne) {
        if (finaux[0]) {
            return true;
        }
        int e = 0;
        for (int i = 0; i < ligne.length(); i++) {
            e = suivant[e * nbColonnes + colonne[ligne.charAt(i)]];
            if (e == MORT) {
                return false;
            }
            if (finaux[e]) {
                return true;
            }
        }
        return false;
    }

    public int nbEtats() {
        return finaux.length;
    }
}
