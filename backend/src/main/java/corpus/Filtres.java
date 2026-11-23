package corpus;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

// Filtres avant de telecharger les livres
// on retire certains titres et les doublons
public final class Filtres {

  private Filtres() { }

    // pas vraiment des livres
    private static final List<String> MOTS_EXCLUS = List.of(
      "factbook", "census", "thesaurus", "dictionary", "gazetteer");

   // variations qu'on ignore entre editions
   private static final Set<String> MOTS_NEUTRES =
        Set.of("the", "a", "an", "of", "and", "complete", "illustrated", "annotated", "unabridged", "edition");


    public static boolean titreExclu(String titre) {
       String txt = titre.toLowerCase(Locale.ROOT);

        for (String w : MOTS_EXCLUS) {
            if(txt.contains(w)) return true;
        }

       return false;
    }

   // titre + auteur : meme cle, meme livre
    public static String cleDoublon(String titre, String auteur) {
       return mots(titre,true) + "|" + mots(auteur, false);
    }


    private static String mots(String texte, boolean estTitre) {
        Set<String> tri = new TreeSet<>(); // les mots seront tries directement

       String sep = estTitre ? "[^\\p{L}\\p{N}]+" : "[^\\p{L}]+";

        for(String w : texte.toLowerCase(Locale.ROOT).split(sep)) {

          if (!w.isEmpty() && !(estTitre && MOTS_NEUTRES.contains(w)))
             tri.add(w);
        }

        return String.join(" ",tri);
    }

    // garde une trace de ce qui est deja passe
    public static class Doublons {

       private final Set<String> cles = new HashSet<>();

        public boolean estDoublon(String titre, String auteur) {
           return cles.contains(cleDoublon(titre,auteur));
        }


        public void enregistrer(String titre, String auteur) {
            cles.add(cleDoublon(titre, auteur)); // on le garde pour la suite
        }
    }
}
