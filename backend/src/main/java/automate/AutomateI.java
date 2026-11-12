package automate;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AutomateI<T> {

    void ajouterEtat(Etat etat);

    void ajouterTransition(Transition transition);

    void ajouterEtatFinal(Etat etat);

    Etat getInitial();

    void setInitial(Etat initial);

    List<Etat> getEtats();

    Map<Etat, Map<String, T>> getTransitions();

    Set<Etat> getFinaux();
}
