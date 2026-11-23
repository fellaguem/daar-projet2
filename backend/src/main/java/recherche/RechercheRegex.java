package recherche;

import ast.interfaces.AstI;
import automate.Afd;
import automate.AutomateTropGrandException;
import automate.ConstructionAutomate;
import experimentation.MatcherUnePasse;
import index.Index;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import parser.Parser;

// Recherche regex directement sur les mots de l'index.
// Pas besoin de relire tous les livres à chaque fois.
public final class RechercheRegex {

    public static final int LONGUEUR_MAX_REGEX = 100;
    public static final int MOTS_RENVOYES = 50;

    public static final int ETATS_MAX = 2000; // évite les automates énormes

    public static class RegexInvalide extends RequeteInvalide {
        public RegexInvalide(String message) {
            super(message);
        }
    }

    // On garde aussi les numéros des mots pour calculer le score après.
    public record Reponse(List<Resultat> resultats, int nbMotsTrouves, List<String> motsTrouves,
            List<Integer> numerosMots) {
    }

    private final Index index;

    public RechercheRegex(Index index) {
        this.index = index;
    }

    // par défaut on cherche un morceau du mot
    public Reponse chercher(String regex) {
        return chercher(regex, false);
    }

    public Reponse chercher(String regex, boolean motEntier) {
        Afd a = construireAutomate(regex, motEntier);
        MatcherTable mt = new MatcherTable(a);

        List<Integer> ids = new ArrayList<>();

        // On parcourt juste le vocabulaire
        for (int k = 0; k < index.nbMots(); k++) {
            String w = index.mot(k);
            if (motEntier ? mt.reconnait(w) : mt.contientMotif(w))
                ids.add(k);
        }

        int[] occ = new int[index.nbLivres()];
        for (int m : ids) {
            for (int p = index.debut(m); p < index.fin(m); p++) {
                occ[index.livreDuPosting(p)] += index.occurrencesDuPosting(p);
            }
        }

        List<Resultat> res = new ArrayList<>();
        for (int b = 0; b < occ.length; b++) {
            if (occ[b] > 0)
                res.add(new Resultat(b, occ[b]));
        }

        // Le plus d'occurrences d'abord, puis le numéro du livre
        res.sort(Comparator.comparingInt(Resultat::occurrences).reversed()
                .thenComparingInt(Resultat::livre));

        List<String> top = new ArrayList<>();
        for (int j = 0; j < Math.min(MOTS_RENVOYES, ids.size()); j++)
            top.add(index.mot(ids.get(j)));

        return new Reponse(res, ids.size(), top, ids);
    }

    // Construit l'AFD une fois pour la regex demandée
    static Afd construireAutomate(String regex, boolean motEntier) {
        String r = regex == null ? "" : regex.strip();
        if (r.isEmpty())
            throw new RegexInvalide("La regex est vide.");

        if (r.length() > LONGUEUR_MAX_REGEX) {
            throw new RegexInvalide("La regex est trop longue (" + LONGUEUR_MAX_REGEX + " caractères au maximum).");
        }

        r = r.toLowerCase(Locale.ROOT); // l'index est en minuscules
        Afd a;
        try {
            AstI ast = new Parser(r).parse();
            a = new ConstructionAutomate(motEntier ? ast : MatcherUnePasse.envelopper(ast), ETATS_MAX)
                    .getAfdMinimal();
        } catch (AutomateTropGrandException ex) {
            throw new RegexInvalide("La regex est trop complexe (l'automate dépasse " + ETATS_MAX + " états).");
        } catch (RuntimeException ex) {
            throw new RegexInvalide("Regex invalide : " + ex.getMessage());
        }

        // En sous-chaîne, une regex qui accepte le vide matcherait tout
        if (!motEntier && a.getFinaux().contains(a.getInitial())) {
            throw new RegexInvalide("Cette regex accepte le mot vide : elle trouverait tous les livres.");
        }

        return a;
    }
}
