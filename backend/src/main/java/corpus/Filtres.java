package corpus;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

// Filtres appliqués aux livres du catalogue avant le téléchargement
public final class Filtres {

    private Filtres() {}

    // Titres qui annoncent des listes de mots ou de chiffres, pas de vrais livres.
    // Les encyclopédies et la Bible ne sont pas écartées.
    private static final List<String> MOTS_EXCLUS =
        List.of("factbook", "census", "thesaurus", "dictionary", "gazetteer");

    // Mots qui changent d'une édition à l'autre sans changer le livre
    private static final Set<String> MOTS_NEUTRES =
        Set.of("the", "a", "an", "of", "and", "complete", "illustrated", "annotated", "unabridged", "edition");

    // Vrai si le titre contient l'un des mots exclus (sans tenir compte des majuscules)
    public static boolean titreExclu(String titre) {
        String bas = titre.toLowerCase(Locale.ROOT);
        for (String mot : MOTS_EXCLUS) {
            if (bas.contains(mot)) {
                return true;
            }
        }
        return false;
    }

    // Clé de comparaison de deux livres : même clé = même livre (doublon).
    // Titre : minuscules, ponctuation retirée, mots neutres retirés, ordre des mots ignoré.
    //   "The King James Version of the Bible" et "The Bible, King James Version, Complete"
    //   donnent la même clé.
    // Auteur : mêmes règles, et les chiffres (dates) sont ignorés.
    //   Un auteur vide ne ressemble qu'à un autre auteur vide.
    // Deux livres de même titre mais d'auteurs différents ne sont pas des doublons.
    public static String cleDoublon(String titre, String auteur) {
        return mots(titre, true) + "|" + mots(auteur, false);
    }

    // Pour un titre (estTitre = true), on garde les chiffres : "Volume 1" et "Volume 2" sont
    // deux livres différents. Pour un auteur, on les retire : ce sont des dates de naissance.
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

    // Garde en mémoire les livres gardés ; un nouveau livre est un doublon si son titre
    // et son auteur ressemblent à ceux d'un livre déjà gardé.
    // Comme on parcourt le catalogue dans l'ordre des identifiants, on garde le plus petit.
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
