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

// Recherche avancée par RegEx, dans l'index.
//
// La regex est testée sur chaque mot du vocabulaire (jamais sur les livres) avec l'AFD minimal
// du projet 1, recopié dans un tableau (MatcherTable). Deux façons de "vérifier" la regex :
// - mot entier (défaut de l'API) : tout le mot doit être reconnu ; "wh(a|e)le" trouve "whale",
//   pas "narwhale". Une regex qui accepte le mot vide est permise (".*" trouve tous les mots).
// - sous-chaîne (comme egrep) : le mot doit contenir un morceau reconnu ; "whal" trouve "whale" et
//   "narwhale". On construit l'automate de ".*R". Une regex qui accepte le mot vide est refusée :
//   elle trouverait tous les mots.
// Un livre est retenu s'il contient au moins un mot trouvé (OU) ; son score est la somme
// des occurrences des mots trouvés dans ce livre.
public final class RechercheRegex {

    public static final int LONGUEUR_MAX_REGEX = 100;
    public static final int MOTS_RENVOYES = 50;

    // Limite du nombre d'états de l'AFD. "a" suivi de k points donne 2^(k+1) états : 2 049 états
    // se construisent en 0,12 s, 16 385 en 5,8 s et 32 769 en 27 s (mesures du 13/11/2026).
    public static final int ETATS_MAX = 2000;

    // regex vide, trop longue, mal écrite ou qui accepte le mot vide : le message est montré à l'utilisateur
    public static class RegexInvalide extends RequeteInvalide {
        public RegexInvalide(String message) {
            super(message);
        }
    }

    // resultats : livres triés par occurrences décroissantes ;
    // motsTrouves : les MOTS_RENVOYES premiers mots trouvés (ordre alphabétique) ;
    // nbMotsTrouves : le nombre total de mots du vocabulaire qui vérifient la regex ;
    // numerosMots : les numéros (dans l'index) de tous les mots trouvés, pour le classement
    public record Reponse(List<Resultat> resultats, int nbMotsTrouves, List<String> motsTrouves,
                          List<Integer> numerosMots) {}

    private final Index index;

    public RechercheRegex(Index index) {
        this.index = index;
    }

    // recherche en sous-chaîne (comportement d'origine, comme egrep)
    public Reponse chercher(String regex) {
        return chercher(regex, false);
    }

    public Reponse chercher(String regex, boolean motEntier) {
        Afd automate = construireAutomate(regex, motEntier);
        MatcherTable matcher = new MatcherTable(automate); // même résultat que MatcherUnePasse, plus rapide

        // 1. les mots du vocabulaire qui vérifient la regex
        List<Integer> mots = new ArrayList<>();
        for (int m = 0; m < index.nbMots(); m++) {
            String mot = index.mot(m);
            if (motEntier ? matcher.reconnait(mot) : matcher.contientMotif(mot)) {
                mots.add(m);
            }
        }

        // 2. les livres qui les contiennent, avec la somme des occurrences
        int[] total = new int[index.nbLivres()];
        for (int m : mots) {
            for (int p = index.debut(m); p < index.fin(m); p++) {
                total[index.livreDuPosting(p)] += index.occurrencesDuPosting(p);
            }
        }
        List<Resultat> resultats = new ArrayList<>();
        for (int livre = 0; livre < total.length; livre++) {
            if (total[livre] > 0) {
                resultats.add(new Resultat(livre, total[livre]));
            }
        }
        resultats.sort(Comparator.comparingInt(Resultat::occurrences).reversed()
            .thenComparingInt(Resultat::livre));

        List<String> premiers = new ArrayList<>();
        for (int i = 0; i < Math.min(MOTS_RENVOYES, mots.size()); i++) {
            premiers.add(index.mot(mots.get(i)));
        }
        return new Reponse(resultats, mots.size(), premiers, mots);
    }

    // regex -> AFD minimal de "regex" (mot entier) ou de ".*regex" (sous-chaîne)
    // (aussi utilisée par MesureCorpus, pour chercher dans le texte complet avec le même automate)
    static Afd construireAutomate(String regex, boolean motEntier) {
        String r = regex == null ? "" : regex.strip();
        if (r.isEmpty()) {
            throw new RegexInvalide("La regex est vide.");
        }
        if (r.length() > LONGUEUR_MAX_REGEX) {
            throw new RegexInvalide("La regex est trop longue (" + LONGUEUR_MAX_REGEX + " caractères au maximum).");
        }
        // l'index est en minuscules : la regex aussi
        r = r.toLowerCase(Locale.ROOT);

        Afd automate;
        try {
            AstI ast = new Parser(r).parse();
            automate = new ConstructionAutomate(motEntier ? ast : MatcherUnePasse.envelopper(ast), ETATS_MAX)
                .getAfdMinimal();
        } catch (AutomateTropGrandException e) {
            throw new RegexInvalide("La regex est trop complexe (l'automate dépasse " + ETATS_MAX + " états).");
        } catch (RuntimeException e) {
            throw new RegexInvalide("Regex invalide : " + e.getMessage());
        }

        // en sous-chaîne, si l'état initial est final, la regex accepte le mot vide et trouverait tous
        // les mots. En mot entier, c'est permis : le mot vide n'est pas dans l'index ("a*" ne trouve
        // que les mots faits de "a").
        if (!motEntier && automate.getFinaux().contains(automate.getInitial())) {
            throw new RegexInvalide("Cette regex accepte le mot vide : elle trouverait tous les livres.");
        }
        return automate;
    }
}
