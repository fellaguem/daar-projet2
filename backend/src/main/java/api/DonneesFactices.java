package api;

import java.util.List;

// Données bidon conformes au contrat, en attendant le vrai moteur
final class DonneesFactices {

    private DonneesFactices() {}

    static final List<Dto.Resultat> RESULTATS = List.of(
        new Dto.Resultat(2701, "Moby Dick", "Herman Melville", 1226, 0.0031),
        new Dto.Resultat(15, "Moby Dick; Or, The Whale", "Herman Melville", 980, 0.0027),
        new Dto.Resultat(2489, "Typee", "Herman Melville", 41, 0.0019));

    static final List<Dto.Suggestion> SUGGESTIONS = List.of(
        new Dto.Suggestion(2694, "Redburn", "Herman Melville"),
        new Dto.Suggestion(10712, "The Cruise of the Cachalot", "Frank T. Bullen"));

    static Dto.ReponseRecherche reponse(String requete, String type) {
        return new Dto.ReponseRecherche(requete, type, 12, RESULTATS, SUGGESTIONS);
    }

    static Dto.Livre livre(int id) {
        return new Dto.Livre(id, "Moby Dick", "Herman Melville",
            "https://www.gutenberg.org/ebooks/" + id,
            "Call me Ishmael. Some years ago, never mind how long precisely…",
            SUGGESTIONS);
    }
}
