package api;

import java.util.List;

// Les objets JSON du contrat d'API (voir docs/api.md)
public final class Dto {

    private Dto() {}

    public record Resultat(int id, String titre, String auteur, int occurrences, double pagerank) {}

    public record Suggestion(int id, String titre, String auteur) {}

    public record ReponseRecherche(String requete, String type, long tempsMs,
                                   List<Resultat> resultats, List<Suggestion> suggestions) {}

    public record Livre(int id, String titre, String auteur, String urlSource,
                        String extrait, List<Suggestion> voisins) {}
}
