package recherche;

import index.Index;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

// Recherche par mot-clef dans l'index.
//
// La requête est lue avec la même règle que les livres (Index.motsDeRequete).
// Avec plusieurs mots, on garde les livres qui les contiennent TOUS (ET) ;
// le score d'un livre est la somme des occurrences de ces mots dans le livre.
public final class RechercheIndex {

    private final Index index;

    public RechercheIndex(Index index) {
        this.index = index;
    }

    // Résultats triés par nombre d'occurrences décroissant (à égalité, par numéro de livre).
    // Liste vide si la requête ne contient aucun mot à chercher ou si un mot est absent de l'index.
    public List<Resultat> motCle(String requete) {
        // chaque mot compte une seule fois : "whale whale" revient à "whale"
        LinkedHashSet<String> mots = new LinkedHashSet<>(Index.motsDeRequete(requete));
        if (mots.isEmpty()) {
            return List.of();
        }

        int[] numeros = new int[mots.size()];
        int i = 0;
        int plusRare = 0;
        for (String mot : mots) {
            int m = index.trouver(mot);
            if (m < 0) {
                return List.of(); // un mot absent : aucun livre ne contient tous les mots
            }
            numeros[i] = m;
            if (index.fin(m) - index.debut(m) < index.fin(numeros[plusRare]) - index.debut(numeros[plusRare])) {
                plusRare = i;
            }
            i++;
        }

        // On parcourt les livres du mot le plus rare, et on vérifie les autres mots par dichotomie
        // dans leurs postings (triés par livre).
        List<Resultat> resultats = new ArrayList<>();
        int rare = numeros[plusRare];
        for (int p = index.debut(rare); p < index.fin(rare); p++) {
            int livre = index.livreDuPosting(p);
            int total = index.occurrencesDuPosting(p);
            boolean tousPresents = true;
            for (int k = 0; k < numeros.length && tousPresents; k++) {
                if (k == plusRare) {
                    continue;
                }
                int occurrences = index.occurrences(numeros[k], livre);
                if (occurrences == 0) {
                    tousPresents = false;
                } else {
                    total += occurrences;
                }
            }
            if (tousPresents) {
                resultats.add(new Resultat(livre, total));
            }
        }

        resultats.sort(Comparator.comparingInt(Resultat::occurrences).reversed()
            .thenComparingInt(Resultat::livre));
        return resultats;
    }
}
