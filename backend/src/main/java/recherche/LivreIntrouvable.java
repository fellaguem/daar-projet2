package recherche;

// Identifiant de livre absent de la bibliothèque. L'API répond avec le code 404.
public class LivreIntrouvable extends RuntimeException {

    public LivreIntrouvable(int id) {
        super("Livre introuvable : " + id);
    }
}
