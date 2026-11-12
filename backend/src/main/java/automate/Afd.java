
package automate;

import java.util.*;


// méme chose que le AFN sauf que ici on une transition d'un etat peux mener a plusieurs autres etats
public class Afd implements AutomateI<Etat> {

    // symbole utilisé pour les caracteres qui ne sont pas directement dans l'alphabet de la regex
    public static final String AUTRE = "AUTRE";

    private List<Etat> etats;
    private Map<Etat, Map<String, Etat>> transitions;
    private Etat initial;
    private Set<Etat> finaux;

    private Set<String> alphabet;

    public Afd() {
        this.etats = new ArrayList<>();
        this.transitions = new HashMap<>();
        this.finaux = new HashSet<>();
        this.alphabet = new TreeSet<>();
    }

    @Override
    public void ajouterEtat(Etat etat) {
        if (!etats.contains(etat)) {
            etats.add(etat);
        }
    }

    // Ajout d'une transition, en verifiant qu'elle respecte bien le determinisme
    @Override
    public void ajouterTransition(Transition transition) {

        String symbole = transition.getSymbole();

        // un AFD ne peut pas avoir de transitions epsilon
        if ("EPSILON".equals(symbole)) {
            throw new IllegalArgumentException(
                "Transition epsilon interdite dans un AFD");
        }

        Etat source = transition.getSource();
        Etat destination = transition.getDestination();

        Map<String, Etat> sorties = transitions
            .computeIfAbsent(source, k -> new HashMap<>());

        Etat existante = sorties.get(symbole);

        // si une autre destination existe deja avec ce symbole alors ce n'est pas deterministe
        if (existante != null && existante != destination) {
            throw new IllegalArgumentException(
                "Transition non deterministe sur " + symbole);
        }

        sorties.put(symbole, destination);
    }

    // retrouver directement la destination d'un état pour un symbole avec la hashmap
    public Etat transition(Etat source, String symbole) {
        return transitions
            .getOrDefault(source, Collections.emptyMap())
            .get(symbole);
    }

    @Override
    public void ajouterEtatFinal(Etat etat) {
        finaux.add(etat);
    }

    @Override
    public Etat getInitial() {
        return initial;
    }

    @Override
    public void setInitial(Etat initial) {
        this.initial = initial;
    }

    @Override
    public List<Etat> getEtats() {
        return etats;
    }

    @Override
    public Map<Etat, Map<String, Etat>> getTransitions() {
        return transitions;
    }

    @Override
    public Set<Etat> getFinaux() {
        return finaux;
    }

    public Set<String> getAlphabet() {
        return alphabet;
    }

    
    // permet de trouver le prochain etat selon le caractere lu si il n'est pas dans l'alphabet on utilise AUTRE
    public Etat suivant(Etat e, char c) {

        String sym = String.valueOf(c);

        if (alphabet.contains(sym)) {
            return transition(e, sym);
        }

        return transition(e, AUTRE);
    }


    public void setAlphabet(Set<String> alphabet) {
        this.alphabet = alphabet;
    }
}
