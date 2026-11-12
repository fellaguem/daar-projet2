
package automate;

import java.util.*;

public class Determinisation {

    // Cette classe permet de transformer notre AFN epsilon en AFD avec la methode des sous ensembles
    private final Afn afn;

    public Determinisation(Afn afn) {
        this.afn = afn;
    }

    // fermeture epsilon : recuperer tout les etats accessibles sans lire de caractere
    public Set<Etat> epsilonClosure(Set<Etat> etats) {

        // les etats deja visités
        Set<Etat> fermeture = new HashSet<>(etats);
        // pile des etats qu'on doit encore explorer
        Deque<Etat> pile = new ArrayDeque<>(etats);

        while (!pile.isEmpty()) {
            Etat e = pile.pop();

            // on regarde uniquement les transitions epsilon de l'etat courant
            for (Etat dest : afn.destinations(e, "EPSILON")) {

                // si l'etat n'a pas deja été visité on l'ajoute dans la pile
                if (fermeture.add(dest)) {
                    pile.push(dest);
                }
            }
        }
        return fermeture;
    }

    // Move permet de voir les etats qu'on peut atteindre en lisant un symbole
    public Set<Etat> move(Set<Etat> etats, String symbole) {

        Set<Etat> resultat = new HashSet<>();

        for (Etat e : etats) {

            // si c'est un caractere normal on recupere ses destinations
            if (!symbole.equals(Afd.AUTRE)) {
                resultat.addAll(
                    afn.destinations(e, symbole)
                );
            }

            // on rajoute aussi les transitions du point . car il accepte n'importe quel caractere
            resultat.addAll(
                afn.destinations(e, ".")
            );
        }
        return resultat;
    }

    // Construction de l'AFD a partir de notre AFN
    public Afd convertir() {

        Afd afd = new Afd();

        // Associer chaque ensemble d'etats AFN à un seul etat AFD
        Map<Set<Etat>, Etat> correspondance = new HashMap<>();
        Queue<Set<Etat>> file = new ArrayDeque<>();

        // recuperation de l'alphabet de notre automate
        Set<String> alphabet = new TreeSet<>();

        for (Map<String, Set<Etat>> sorties :
                afn.getTransitions().values()) {

            for (String sym : sorties.keySet()) {

                // le point est remplacé par AUTRE pour les caracteres non presents
                if (sym.equals(".")) {
                    alphabet.add(Afd.AUTRE);
                }
                else if (!sym.equals("EPSILON")) {
                    alphabet.add(sym);
                }
            }
        }

        afd.setAlphabet(alphabet);

        // l'etat initial de l'AFD est la fermeture epsilon de l'initial AFN
        Set<Etat> depart = epsilonClosure(
            Set.of(afn.getInitial())
        );

        Etat initialAfd = new Etat();

        afd.ajouterEtat(initialAfd);
        afd.setInitial(initialAfd);

        correspondance.put(depart, initialAfd);
        file.add(depart);

        // Si l'ensemble contient un etat final alors notre etat AFD est final
        if (contientFinal(depart)) {
            afd.ajouterEtatFinal(initialAfd);
        }

        // on traite tout les ensembles qui sont dans la file
        while (!file.isEmpty()) {

            Set<Etat> courant = file.poll();
            Etat sourceAfd = correspondance.get(courant);

            for (String sym : alphabet) {

                // on avance avec le symbole puis on fait la fermeture epsilon
                Set<Etat> destAf =
                    epsilonClosure(move(courant, sym));

                // aucun etat accessible avec ce symbole
                if (destAf.isEmpty()) {
                    continue;
                }

                // si cet ensemble n'existe pas encore on cree un nouvel etat AFD
                if (!correspondance.containsKey(destAf)) {

                    Etat nvEtat = new Etat();
                    afd.ajouterEtat(nvEtat);

                    correspondance.put(destAf, nvEtat);
                    file.add(destAf);

                    if (contientFinal(destAf)) {
                        afd.ajouterEtatFinal(nvEtat);
                    }
                }

                // on relie les deux etats de l'AFD avec le symbole
                afd.ajouterTransition(
                    new Transition(
                        sourceAfd,
                        correspondance.get(destAf),
                        sym
                    )
                );
            }
        }

        return afd;
    }

    // verifier si au moins un des etats de l'ensemble est final dans l'AFN
    private boolean contientFinal(Set<Etat> etats) {

        for (Etat e : etats) {
            if (afn.getFinaux().contains(e)) {
                return true;
            }
        }
        return false;
    }
}
