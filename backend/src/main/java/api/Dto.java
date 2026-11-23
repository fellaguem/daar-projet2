package api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import recherche.Moteur;

// objets renvoyés à l'API
public final class Dto {

    private Dto() {
    }

    // score calculé côté moteur
    public record Resultat(int id, String titre, String auteur, int occurrences,
            double pagerank, double score) {
    }

    public record Suggestion(int id, String titre, String auteur) {
    }

    // ces trois champs restent absents pour une recherche classique
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReponseRecherche(String requete, String type, double tempsMs,
            List<Resultat> resultats, List<Suggestion> suggestions,
            Integer nbMotsTrouves, List<String> motsTrouves, Boolean motEntier) {
    }

    public record Erreur(String erreur) {
    } // 400 ou 404

    public record Livre(int id, String titre, String auteur, String urlSource,
            String extrait, List<Suggestion> voisins) {
    }

    // résultat du moteur -> résultat API
    static ReponseRecherche depuis(Moteur.Reponse r) {
        List<Resultat> res = r.resultats().stream()
                .map(x -> new Resultat(x.id(), x.titre(), x.auteur(), x.occurrences(),
                        x.pagerank(), x.score()))
                .toList();

        return new ReponseRecherche(r.requete(), r.type(), r.tempsMs(), res,
                suggestions(r.suggestions()), r.nbMotsTrouves(),
                r.motsTrouves(), r.motEntier());
    }

    static Livre depuis(Moteur.Fiche f) {
        return new Livre(f.id(), f.titre(), f.auteur(), f.urlSource(),
                f.extrait(), suggestions(f.voisins()));
    }

    private static List<Suggestion> suggestions(List<Moteur.LivreCourt> docs) {
        return docs.stream()
                .map(b -> new Suggestion(b.id(), b.titre(), b.auteur())).toList();
    }
}
