package recherche;

// Requête refusée (mot-clef vide, regex invalide, tri ou mode inconnu)
public class RequeteInvalide extends RuntimeException {

    public RequeteInvalide(String message) {
        super(message);
    }
}
