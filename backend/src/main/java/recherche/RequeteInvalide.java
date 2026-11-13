package recherche;

// Requête refusée (mot-clef vide, regex invalide, tri ou mode inconnu) : le message est montré à
// l'utilisateur. L'API répond avec le code 400.
public class RequeteInvalide extends RuntimeException {

    public RequeteInvalide(String message) {
        super(message);
    }
}
