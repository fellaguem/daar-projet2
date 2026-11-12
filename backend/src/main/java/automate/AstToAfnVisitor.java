package automate;

import java.util.Map;
import java.util.Set;
import ast.AstChar;
import ast.AstConcat;
import ast.AstDot;
import ast.AstOr;
import ast.AstRepeat;
import ast.AstVide;
import ast.interfaces.AstVisitor;

public class AstToAfnVisitor implements AstVisitor<Afn> {

    /** Dans cette classe on fait la transition d'un AST vers un AFN avec le design pattern Visitor */

    public AstToAfnVisitor() {
    }

    // cette methode permet de copier les etats et transitions d'un afn dans un autre
    private void copierAfn(Afn source, Afn destination) {

        // On récupére tout les états de l'AFN source
        for (Etat etat : source.getEtats()) {
            destination.ajouterEtat(etat);
        }
        // parcourir la hashmap des transitions
        for (Map.Entry<Etat, Map<String, Set<Etat>>> entreeEtat :
                source.getTransitions().entrySet()) {

            Etat depart = entreeEtat.getKey();

            // pour chaque état on récupere les symboles des transitions
            for (Map.Entry<String, Set<Etat>> entreeSymbole :
                    entreeEtat.getValue().entrySet()) {

                String symbole = entreeSymbole.getKey();

                // un symbole peut avoir plusieurs destinations dans un AFN
                for (Etat arrivee : entreeSymbole.getValue()) {

                    destination.ajouterTransition(
                        new Transition(depart, arrivee, symbole)
                    );
                }
            }
        }
    }

    // Cas de visite d'un caractére q0 --char--> q1
    @Override
    public Afn visit(AstChar noeud) {

        // noeud initial
        Etat debut = new Etat();
        // noeud final
        Etat fin = new Etat();

        // notre afn
        Afn afn = new Afn();

        afn.ajouterEtat(debut);
        afn.ajouterEtat(fin);

        afn.setInitial(debut);
        afn.ajouterEtatFinal(fin);

        // creation de la transition avec la valeur du caractére
        afn.ajouterTransition(
            new Transition(
                debut,
                fin,
                String.valueOf(noeud.getValeur())
            )
        );

        return afn;
    }

    // cas du point . qui représente n'importe quel caractére
    @Override
    public Afn visit(AstDot noeud) {

        Etat debut = new Etat();
        Etat fin = new Etat();
        Afn afn = new Afn();

        afn.ajouterEtat(debut);
        afn.ajouterEtat(fin);
        afn.setInitial(debut);
        afn.ajouterEtatFinal(fin);

        // meme construction qu'un char normal mais avec le symbole .
        afn.ajouterTransition(
            new Transition(debut, fin, ".")
        );

        return afn;
    }

    // cas du mot vide epsilon, pas besoin de transition
    @Override
    public Afn visit(AstVide noeud) {

        Etat etat = new Etat();

        Afn afn = new Afn();

        afn.ajouterEtat(etat);

        // le meme état est initial et final pour accepter le mot vide
        afn.setInitial(etat);
        afn.ajouterEtatFinal(etat);

        return afn;
    }

    // Cas de la concaténation, par exemple ab
    @Override
    public Afn visit(AstConcat noeud) {

        // On construit d'abord les deux AFN des fils avec le visitor
        Afn gauche = noeud.getFilsGauche().accept(this);
        Afn droite = noeud.getFilsDroit().accept(this);

        Afn resultat = new Afn();

        // on regroupe les deux automates dans le resultat
        copierAfn(gauche, resultat);
        copierAfn(droite, resultat);

        // Relier les etats finaux du gauche à l'initial du droit par epsilon
        for (Etat etat : gauche.getFinaux()) {

            resultat.ajouterTransition(
                new Transition(
                    etat,
                    droite.getInitial(),
                    "EPSILON"
                )
            );
        }

        // On commence par l'automate gauche
        resultat.setInitial(gauche.getInitial());

        // et on termine dans les etats finaux du droit
        for (Etat etat : droite.getFinaux()) {
            resultat.ajouterEtatFinal(etat);
        }

        return resultat;
    }

    // cas de l'alternative a|b, on doit pouvoir choisir entre les deux chemins
    @Override
    public Afn visit(AstOr noeud) {
        Afn gauche = noeud.getFilsGauche().accept(this);
        Afn droite = noeud.getFilsDroit().accept(this);

        // On rajoute un nouvel état initial et un final
        Etat debut = new Etat();
        Etat fin = new Etat();

        Afn resultat = new Afn();
        copierAfn(gauche, resultat);
        copierAfn(droite, resultat);

        resultat.ajouterEtat(debut);
        resultat.ajouterEtat(fin);
        // Depuis le debut on peut aller vers la branche gauche sans lire de char
        resultat.ajouterTransition(
            new Transition(
                debut,
                gauche.getInitial(),
                "EPSILON"
            )
        );

        // ou bien vers la branche droite
        resultat.ajouterTransition(
            new Transition(
                debut,
                droite.getInitial(),
                "EPSILON"
            )
        );

        // les deux branches doivent rejoindre le meme état final
        for (Etat etat : gauche.getFinaux()) {
            resultat.ajouterTransition(
                new Transition(etat, fin, "EPSILON")
            );
        }

        for (Etat etat : droite.getFinaux()) {

            resultat.ajouterTransition(
                new Transition(etat, fin, "EPSILON")
            );
        }

        resultat.setInitial(debut);
        resultat.ajouterEtatFinal(fin);
        return resultat;
    }

    // cas de l'étoile de Kleene, permet de répéter 0 ou plusieurs fois
    @Override
    public Afn visit(AstRepeat noeud) {

        // on construit l'AFN de l'expression qu'on veut répéter
        Afn interieur = noeud.getFils().accept(this);
        Etat debut = new Etat();
        Etat fin = new Etat();

        Afn resultat = new Afn();

        copierAfn(interieur, resultat);

        resultat.ajouterEtat(debut);
        resultat.ajouterEtat(fin);

        // entrer dans l'automate interieur pour faire une repetition
        resultat.ajouterTransition(
            new Transition(
                debut,
                interieur.getInitial(),
                "EPSILON"
            )
        );

        // ou directement aller à la fin, donc 0 répétition
        resultat.ajouterTransition(
            new Transition(debut, fin, "EPSILON")
        );

        for (Etat etat : interieur.getFinaux()) {
            // revenir au debut de l'expression pour recommencer
            resultat.ajouterTransition(
                new Transition(
                    etat,
                    interieur.getInitial(),
                    "EPSILON"
                )
            );
            // Sinon on peut sortir de la boucle et terminer
            resultat.ajouterTransition(
                new Transition(etat, fin, "EPSILON")
            );
        }

        resultat.setInitial(debut);
        resultat.ajouterEtatFinal(fin);
        return resultat;
    }
}
