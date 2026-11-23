package recherche;

// Identifiant de livre absent de la bibliothèque.
public class LivreIntrouvable extends RuntimeException {

    public LivreIntrouvable(int id) {
        super("Livre introuvable : " + id);
    }
}
