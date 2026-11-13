package recherche;

// Un livre trouvé par une recherche (mot-clef ou regex) avec son nombre d'occurrences.
// livre = numéro du livre dans l'index (pas l'identifiant Gutenberg).
public record Resultat(int livre, int occurrences) {}
