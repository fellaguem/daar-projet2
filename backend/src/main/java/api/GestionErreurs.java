package api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import recherche.LivreIntrouvable;
import recherche.RequeteInvalide;

// Transforme les erreurs en réponses JSON { "erreur": "..." } avec le bon code HTTP
@RestControllerAdvice
public class GestionErreurs {

    // mot-clef vide, regex invalide, tri ou mode inconnu
    @ExceptionHandler(RequeteInvalide.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Dto.Erreur requeteInvalide(RequeteInvalide e) {
        return new Dto.Erreur(e.getMessage());
    }

    @ExceptionHandler(LivreIntrouvable.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Dto.Erreur livreIntrouvable(LivreIntrouvable e) {
        return new Dto.Erreur(e.getMessage());
    }

    // ?q= ou ?regex= absent
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Dto.Erreur parametreManquant(MissingServletRequestParameterException e) {
        return new Dto.Erreur("Paramètre manquant : " + e.getParameterName());
    }

    // /api/books/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Dto.Erreur parametreInvalide(MethodArgumentTypeMismatchException e) {
        return new Dto.Erreur("Paramètre invalide : " + e.getName());
    }
}
