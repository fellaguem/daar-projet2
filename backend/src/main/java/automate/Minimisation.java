
package automate;

import java.util.*;

public class Minimisation {

    // Cette classe minimise notre AFD avec l'algorithme de Moore
    private final Afd afd;

    public Minimisation(Afd afd) {
        this.afd = afd;
    }

    //On supprime les etats qu'on peut jamais atteindre depuis l'initial
    private Afd supprimerInaccessibles() {

        if (afd.getInitial() == null) {
            throw new IllegalStateException(
                "L'AFD n'a pas d'etat initial" 
            );
        }

        Set<Etat> accessibles = new HashSet<>();
        Queue<Etat> file = new ArrayDeque<>();

        accessibles.add(afd.getInitial());
        file.add(afd.getInitial());

        //parcours en largeur depuis l'etat initial
        while (!file.isEmpty()) {

            Etat e = file.poll();

            Map<String, Etat> sorties =
                afd.getTransitions().getOrDefault(
                    e,
                    Collections.emptyMap()
                );

            // on recupere les etats qu'on peut atteindre depuis e
            for (Etat dest : sorties.values()) {

                if (accessibles.add(dest)) {
                    file.add(dest);
                }
            }
        }

        Afd res = new Afd();
        res.setAlphabet(afd.getAlphabet());

        // garder seulement les etats accessibles
        for (Etat e : afd.getEtats()) {

            if (accessibles.contains(e)) {

                res.ajouterEtat(e);

                if (afd.getFinaux().contains(e)) {
                    res.ajouterEtatFinal(e);
                }
            }
        }

        res.setInitial(afd.getInitial());

        //on recopie les transitions entre les etats conservés
        for (Map.Entry<Etat, Map<String, Etat>> entree :
                afd.getTransitions().entrySet()) {

            Etat src = entree.getKey();

            if (!accessibles.contains(src)) {
                continue;
            }

            for (Map.Entry<String, Etat> sortie :
                    entree.getValue().entrySet()) {

                Etat dest = sortie.getValue();

                if (accessibles.contains(dest)) {

                    res.ajouterTransition(
                        new Transition(
                            src,
                            dest,
                            sortie.getKey()
                        )
                    );
                }
            }
        }
        return res;
    }

    // Completer l'AFD en ajoutant un etat puits si il manque des transitions
    private Afd completerAutomate(Afd automate) {

        Set<String> alphabet = automate.getAlphabet();

        Afd res = new Afd();
        res.setAlphabet(alphabet);

        // recopie des etats et des finaux
        for (Etat e : automate.getEtats()) {
            res.ajouterEtat(e);
        }

        for (Etat e : automate.getFinaux()) {
            res.ajouterEtatFinal(e);
        }

        res.setInitial(automate.getInitial());

        // copier les transitions de notre AFD
        for (Map.Entry<Etat, Map<String, Etat>> entree :
                automate.getTransitions().entrySet()) {

            Etat src = entree.getKey();

            for (Map.Entry<String, Etat> sortie :
                    entree.getValue().entrySet()) {

                res.ajouterTransition(
                    new Transition(
                        src,
                        sortie.getValue(),
                        sortie.getKey()
                    )
                );
            }
        }

        Etat puits = null;

        for (Etat e : automate.getEtats()) {

            for (String sym : alphabet) {

                //si aucune transition existe sur ce symbole
                if (automate.transition(e, sym) == null) {

                    // on crée le puits seulement si on en a besoin
                    if (puits == null) {
                        puits = new Etat();
                        res.ajouterEtat(puits);
                    }

                    res.ajouterTransition(
                        new Transition(e, puits, sym)
                    );
                }
            }
        }

        // le puits revient toujours sur lui meme
        if (puits != null) {

            for (String sym : alphabet) {

                res.ajouterTransition(
                    new Transition(puits, puits, sym)
                );
            }
        }

        return res;
    }

    //premiere partition ou on mets les etats finaux d'un coté et les non finaux de l'autre
    private List<Set<Etat>> partitionsInitiales(Afd automate) {

        Set<Etat> finaux = new LinkedHashSet<>();
        Set<Etat> nonFinaux = new LinkedHashSet<>();

        for (Etat e : automate.getEtats()) {

            if (automate.getFinaux().contains(e)) {
                finaux.add(e);
            } else {
                nonFinaux.add(e);
            }
        }

        List<Set<Etat>> partitions = new ArrayList<>();

        if (!finaux.isEmpty()) {
            partitions.add(finaux);
        }

        if (!nonFinaux.isEmpty()) {
            partitions.add(nonFinaux);
        }

        return partitions;
    }

    //retrouver dans quel groupe se trouve un etat
    private int indicePartition(
            Etat etat,
            List<Set<Etat>> partitions) {

        for (int i = 0; i < partitions.size(); i++) {

            if (partitions.get(i).contains(etat)) {
                return i;
            }
        }

        throw new IllegalStateException(
            "Etat absent des partitions"
        );
    }

    // Ici on sépare les etats qui n'ont pas le meme comportement
    private List<Set<Etat>> raffinerPartitions(
            Afd automate,
            List<Set<Etat>> partitions) {

        Set<String> alphabet = automate.getAlphabet();
        List<Set<Etat>> nouvelles = new ArrayList<>();

        for (Set<Etat> groupe : partitions) {

            // chaque signature correspond aux groupes atteints par les transitions
            Map<List<Integer>, Set<Etat>> sousGroupes =
                new LinkedHashMap<>();

            for (Etat e : groupe) {

                List<Integer> signature = new ArrayList<>();

                for (String sym : alphabet) {

                    Etat dest = automate.transition(e, sym);

                    // chercher le numero de partition de la destination
                    int indice = indicePartition(dest, partitions);
                    signature.add(indice);
                }

                // les etats avec la meme signature restent ensemble
                sousGroupes
                    .computeIfAbsent(
                        signature,
                        k -> new LinkedHashSet<>()
                    )
                    .add(e);
            }

            nouvelles.addAll(sousGroupes.values());
        }

        return nouvelles;
    }

    //onstruire le nouvel AFD en fusionnant les etats de chaque partition
    private Afd construireAfdMinimal(
            Afd automate,
            List<Set<Etat>> partitions) {

        Afd minimal = new Afd();
        minimal.setAlphabet(automate.getAlphabet());

        // associer chaque ancien etat à son nouvel etat
        Map<Etat, Etat> correspondance = new HashMap<>();

        for (Set<Etat> groupe : partitions) {

            Etat nvEtat = new Etat();
            minimal.ajouterEtat(nvEtat);

            for (Etat ancien : groupe) {

                correspondance.put(ancien, nvEtat);

                if (automate.getFinaux().contains(ancien)) {
                    minimal.ajouterEtatFinal(nvEtat);
                }
            }

            // si le groupe contient l'ancien initial, il devient initial
            if (groupe.contains(automate.getInitial())) {
                minimal.setInitial(nvEtat);
            }
        }

        Set<String> alphabet = automate.getAlphabet();

        // On reconstruit les transitions avec les nouveaux etats
        for (Set<Etat> groupe : partitions) {

            //un seul représentant suffit car les etats du groupe sont equivalents
            Etat representant = groupe.iterator().next();

            Etat src = correspondance.get(representant);

            for (String sym : alphabet) {

                Etat ancDest =
                    automate.transition(representant, sym);

                Etat nvDest = correspondance.get(ancDest);

                minimal.ajouterTransition(
                    new Transition(
                        src,
                        nvDest,
                        sym
                    )
                );
            }
        }

        return minimal;
    }

    // methode principale qui lance toutes les étapes de la minimisation
    public Afd minimiser() {

        Afd accessible = supprimerInaccessibles();
        Afd complet = completerAutomate(accessible);

        List<Set<Etat>> partitions =
            partitionsInitiales(complet);

        // on recommence jusqu'a ce que les groupes ne changent plus
        while (true) {

            List<Set<Etat>> nouvelles =
                raffinerPartitions(complet, partitions);

            if (nouvelles.equals(partitions)) {
                break;
            }

            partitions = nouvelles;
        }

        // Construire l'AFD minimal puis enlever le puits
        return retirerPuits(construireAfdMinimal(complet, partitions));
    }

    // retirer les etats qui ne peuvent pas mener vers un etat final
    private Afd retirerPuits(Afd automate) {

        // pour chaque etat on stocke les etats qui arrivent vers lui
        Map<Etat, List<Etat>> predecesseurs = new HashMap<>();

        for (Map.Entry<Etat, Map<String, Etat>> entree :
                automate.getTransitions().entrySet()) {

            for (Etat dest : entree.getValue().values()) {

                predecesseurs
                    .computeIfAbsent(dest, k -> new ArrayList<>())
                    .add(entree.getKey());
            }
        }

        // on part des etats finaux pour retrouver les etats utiles
        Set<Etat> utiles = new HashSet<>(automate.getFinaux());
        Deque<Etat> file = new ArrayDeque<>(automate.getFinaux());

        while (!file.isEmpty()) {

            Etat e = file.poll();

            for (Etat prec : predecesseurs.getOrDefault(
                    e, Collections.emptyList())) {

                if (utiles.add(prec)) {
                    file.add(prec);
                }
            }
        }

        // garder toujours l'etat initial meme si aucun mot est accepté
        utiles.add(automate.getInitial());

        Afd res = new Afd();
        res.setAlphabet(automate.getAlphabet());

        for (Etat e : automate.getEtats()) {

            if (utiles.contains(e)) {

                res.ajouterEtat(e);

                if (automate.getFinaux().contains(e)) {
                    res.ajouterEtatFinal(e);
                }
            }
        }

        res.setInitial(automate.getInitial());

        //Recopier les transitions uniquement entre les etats utiles
        for (Map.Entry<Etat, Map<String, Etat>> entree :
                automate.getTransitions().entrySet()) {

            Etat src = entree.getKey();

            if (!utiles.contains(src)) {
                continue;
            }

            for (Map.Entry<String, Etat> sortie :
                    entree.getValue().entrySet()) {

                if (utiles.contains(sortie.getValue())) {

                    res.ajouterTransition(
                        new Transition(
                            src,
                            sortie.getValue(),
                            sortie.getKey()
                        )
                    );
                }
             }
          }

        return res;
    }
}
