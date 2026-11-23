package index;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// L'unique classe qui découpe un texte en mots
// elle sert à compter les mots du corpus, à construire l'index et à lire les requêtes
// il faut la même règle partout, sinon un mot de l'index ne serait pas retrouvé
public final class Tokeniseur {

    private Tokeniseur() {}

    // appelle action pour chaque mot du texte, dans l'ordre, sans construire de liste
    public static void pourChaqueMot(CharSequence texte, Consumer<String> action) {
        StringBuilder mot = new StringBuilder();
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (Character.isLetter(c)) {
                mot.append(Character.toLowerCase(c));
            } else if (mot.length() > 0) {
                action.accept(mot.toString());
                mot.setLength(0);
            }
        }
        if (mot.length() > 0) {
            action.accept(mot.toString());
        }
    }

    public static List<String> mots(CharSequence texte) {
        List<String> liste = new ArrayList<>();
        pourChaqueMot(texte, liste::add);
        return liste;
    }

    public static int compter(CharSequence texte) {
        int[] n = {0};
        pourChaqueMot(texte, m -> n[0]++);
        return n[0];
    }
}
