package api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import recherche.Moteur;

// Les objets JSON du contrat d'API (voir docs/api.md), et leur construction à partir du moteur
public final class Dto {

    private Dto() {}

    // score : (occurrences des mots non vides / nombre de mots du livre) × pagerank
    public record Resultat(int id, String titre, String auteur, int occurrences, double pagerank, double score) {}

    public record Suggestion(int id, String titre, String auteur) {}

    // nbMotsTrouves et motsTrouves n'existent que pour une recherche par regex (type "regex") :
    // pour un mot-clef ils valent null et ne sont pas écrits dans le JSON.
    // tempsMs : temps du moteur (recherche, classement, suggestions), sans le réseau ni le JSON.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReponseRecherche(String requete, String type, double tempsMs,
                                   List<Resultat> resultats, List<Suggestion> suggestions,
                                   Integer nbMotsTrouves, List<String> motsTrouves) {}

    // erreur renvoyée avec le code 400 (requête refusée) ou 404 (livre introuvable)
    public record Erreur(String erreur) {}

    public record Livre(int id, String titre, String auteur, String urlSource,
                        String extrait, List<Suggestion> voisins) {}

    // ---------- conversion depuis le moteur ----------

    static ReponseRecherche depuis(Moteur.Reponse r) {
        List<Resultat> resultats = r.resultats().stream()
            .map(t -> new Resultat(t.id(), t.titre(), t.auteur(), t.occurrences(), t.pagerank(), t.score()))
            .toList();
        return new ReponseRecherche(r.requete(), r.type(), r.tempsMs(), resultats,
            suggestions(r.suggestions()), r.nbMotsTrouves(), r.motsTrouves());
    }

    static Livre depuis(Moteur.Fiche f) {
        return new Livre(f.id(), f.titre(), f.auteur(), f.urlSource(), f.extrait(), suggestions(f.voisins()));
    }

    private static List<Suggestion> suggestions(List<Moteur.LivreCourt> livres) {
        return livres.stream().map(l -> new Suggestion(l.id(), l.titre(), l.auteur())).toList();
    }
}
