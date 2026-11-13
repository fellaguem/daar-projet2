package api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

// Les objets JSON du contrat d'API (voir docs/api.md)
public final class Dto {

    private Dto() {}

    public record Resultat(int id, String titre, String auteur, int occurrences, double pagerank) {}

    public record Suggestion(int id, String titre, String auteur) {}

    // nbMotsTrouves et motsTrouves n'existent que pour une recherche par regex (type "regex") :
    // pour un mot-clef ils valent null et ne sont pas écrits dans le JSON
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReponseRecherche(String requete, String type, long tempsMs,
                                   List<Resultat> resultats, List<Suggestion> suggestions,
                                   Integer nbMotsTrouves, List<String> motsTrouves) {}

    // erreur renvoyée avec le code 400 (regex invalide, trop longue, etc.)
    public record Erreur(String erreur) {}

    public record Livre(int id, String titre, String auteur, String urlSource,
                        String extrait, List<Suggestion> voisins) {}
}
