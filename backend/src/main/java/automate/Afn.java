
package automate;

import java.util.*;

// Cette classe represente notre automate fini non deterministe (AFN) avec ses etats et transitions
public class Afn implements AutomateI<Set<Etat>> {

    private List<Etat> etats;
    private Map<Etat, Map<String, Set<Etat>>> transitions;
    private Etat initial;
    private Set<Etat> finaux;

    public Afn() {
        this.etats = new ArrayList<>();
        this.transitions = new HashMap<>();
        this.finaux = new HashSet<>();
    }

    // ajouter un etat si il n'existe pas deja
    @Override
    public void ajouterEtat(Etat etat) {
        if (!etats.contains(etat)) {
            etats.add(etat);
        }
    }

    // ajout d'une transition dans notre hashmap, avec plusieurs destinations possibles pour un meme symbole
    @Override
    public void ajouterTransition(Transition transition) {

        transitions
            .computeIfAbsent(
                transition.getSource(),
                k -> new HashMap<>()
            )
            .computeIfAbsent(
                transition.getSymbole(),
                k -> new HashSet<>()
            )
            .add(transition.getDestination());
    }

    // Récuperer les etats accessibles depuis une source en lisant un symbole
    public Set<Etat> destinations(Etat source, String symbole) {
        return transitions
            .getOrDefault(source, Collections.emptyMap())
            .getOrDefault(symbole, Collections.emptySet());
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
    public Map<Etat, Map<String, Set<Etat>>> getTransitions() {
        return transitions;
    }

    @Override
    public Set<Etat> getFinaux() {
        return finaux;
    }
}
