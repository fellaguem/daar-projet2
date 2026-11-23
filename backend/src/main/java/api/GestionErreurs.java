package api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import recherche.LivreIntrouvable;
import recherche.RequeteInvalide;

// renvoie les erreurs en JSON, avec le bon statut
@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(RequeteInvalide.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Dto.Erreur requeteInvalide(RequeteInvalide e) {
        return new Dto.Erreur(e.getMessage()); // requête pas valide
    }

    @ExceptionHandler(LivreIntrouvable.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Dto.Erreur livreIntrouvable(LivreIntrouvable e) {
        return new Dto.Erreur(e.getMessage());
    }

    // paramètre oublié dans l'URL
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Dto.Erreur parametreManquant(MissingServletRequestParameterException ex) {
        return new Dto.Erreur("Paramètre manquant : " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Dto.Erreur parametreInvalide(MethodArgumentTypeMismatchException err) {
        return new Dto.Erreur("Paramètre invalide : " + err.getName()); // ex : id non numérique
    }
}
