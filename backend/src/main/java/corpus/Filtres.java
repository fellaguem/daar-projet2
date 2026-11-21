package corpus;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;


/*
 * Filtres appliques aux livres du catalogue, avant le telechargement 
 * titres exclus (listes de mots ou de chiffres, pas un vrai livre)
 * doublons (meme titre et meme auteur qu'un livre deja garde)
 */

public final class Filtres {


    private Filtres() {}

    // titres qui ne sont pas un vrai livre (listes de mots ou de chiffres)
    private static final List<String> MOTS_EXCLUS =
        List.of("factbook", "census", "thesaurus", "dictionary", "gazetteer");

    // mots qui changent d'une edition a l'autre sans changer le livre
    private static final Set<String> MOTS_NEUTRES =
        Set.of("the", "a", "an", "of", "and", "complete", "illustrated", "annotated", "unabridged", "edition");


    // vrai si le titre contient un mot exclu (majuscules ignorees)
    public static boolean titreExclu(String titre) {

        String bas = titre.toLowerCase(Locale.ROOT);

        for (String mot : MOTS_EXCLUS) {
            if (bas.contains(mot)) {
                return true;
            }
        }

        return false;
    }

    // cle d'un livre (titre + auteur) : meme cle = doublon
    // sans majuscules, ponctuation, mots neutres ni dates, ordre des mots ignore
    public static String cleDoublon(String titre, String auteur) {
        return mots(titre, true) + "|" + mots(auteur, false);
    }

    // titre : on garde les chiffres ("Volume 1" et "Volume 2" sont deux livres differents)
    private static String mots(String texte, boolean estTitre) {
        Set<String> mots = new TreeSet<>();
        String separateur = estTitre ? "[^\\p{L}\\p{N}]+" : "[^\\p{L}]+";

        for (String mot : texte.toLowerCase(Locale.ROOT).split(separateur)) {

            if (!mot.isEmpty() && !(estTitre && MOTS_NEUTRES.contains(mot))) {
                mots.add(mot);
            }
        }
        return String.join(" ", mots);
    }

    // carnet des livres deja gardes : un nouveau livre est un doublon s'il a la meme cle
    // le catalogue est lu par id croissant : on garde donc le plus petit id
    public static class Doublons {

        private final Set<String> cles = new HashSet<>();

        public boolean estDoublon(String titre, String auteur) {
            return cles.contains(cleDoublon(titre, auteur));
        }

        public void enregistrer(String titre, String auteur) {
            cles.add(cleDoublon(titre, auteur));
        }
    }
}
