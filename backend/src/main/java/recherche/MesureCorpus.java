package recherche;

import corpus.Metadonnees;
import graphe.ConstructionGraphe;
import graphe.Graphe;
import graphe.Jaccard;
import graphe.PageRank;
import index.Index;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

// Mesures de performance sur des sous-corpus de N livres, pour les courbes du rapport.
//
//   java -Xmx4g -cp backend/target/classes recherche.MesureCorpus [--dossier data]
//        [--tailles 100,200,400,800,1200,1893] [--texte-max 800] [--graine 42] [--sortie DOSSIER/mesures]
//
// Sous-corpus : les livres (par identifiant croissant) sont mélangés une fois avec une graine fixe ;
// le sous-corpus de taille N est formé des N premiers livres de cet ordre. Les sous-corpus sont donc
// emboîtés (celui de 100 est dans celui de 200...), reproductibles et représentatifs du corpus.
//
// Pour chaque N, tout est construit en mémoire avec les mêmes classes et les mêmes réglages que le
// vrai pipeline (ConstructionIndex, ConstructionGraphe). Rien n'est écrit dans le dossier des données,
// sauf les fichiers CSV de --sortie.
//   1. index : lecture des fichiers, découpage en mots, tableaux (1 construction de chauffe, puis 5) ;
//      les fichiers sont dans le cache du système après la chauffe. L'index est ensuite sauvé dans un
//      fichier temporaire et rechargé, pour que la suite le trouve en mémoire comme le serveur
//   2. graphe (1 + 10), en 4 temps : empreintes de Jaccard (250 mots, mots vides recalculés pour les
//      N livres), distances des n(n-1)/2 paires, graphe (seuil pour un degré moyen de 10 : la règle du
//      vrai graphe, le seuil dépend donc de N), PageRank
//   3. requêtes par le moteur complet (recherche, classement, suggestions), sans HTTP :
//      2 s de chauffe, puis 30 tours ; dans chaque tour, toutes les requêtes dans un ordre mélangé
//   4. si N <= --texte-max : regex (mot entier) dans l'index contre regex dans le texte complet,
//      textes chargés en mémoire avant de chronométrer. Les deux doivent trouver les mêmes livres
//      avec les mêmes nombres d'occurrences, sinon le programme s'arrête.
//
// Fichiers écrits dans --sortie :
//   sous_corpus.csv     taille de chaque sous-corpus (mots, vocabulaire, paires, graphe)
//   construction.csv    temps de construction par étape
//   requetes.csv        temps des requêtes par mot-clef et par regex
//   index_texte.csv     regex : index contre texte complet
// Temps en millisecondes : moyenne et écart type d'échantillon (division par essais - 1).
public class MesureCorpus {

    private static final List<String> MOTS_CLEFS = List.of("whale", "love", "the", "white whale", "harpoon");
    private static final List<String> REGEXS = List.of(
        "whale", "wh(a|e)le", "s(a|g|r)*on", "a.*b", ".*ing", "(sh|wh)(a|o)(l|r)(e|k)", "q.*z", ".*");
    private static final List<String> REGEXS_TEXTE = List.of("wh(a|e)le", "(sh|wh)(a|o)(l|r)(e|k)", ".*ing");

    private static final int ESSAIS_INDEX = 5;
    private static final int ESSAIS_GRAPHE = 10;
    private static final long CHAUFFE_REQUETES_MS = 2000;
    private static final int TOURS_REQUETES = 30;
    private static final int ESSAIS_REGEX_INDEX = 30;
    private static final int ESSAIS_REGEX_TEXTE = 5;
    private static final double DEGRE_MOYEN = 10;

    private record Requete(String type, String texte) {}

    public static void main(String[] args) throws IOException {
        Path dossier = Path.of("data");
        Path sortie = null;
        List<Integer> tailles = List.of(100, 200, 400, 800, 1200, 1893);
        int texteMax = 800;
        long graine = 42;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dossier" -> dossier = Path.of(args[++i]);
                case "--sortie" -> sortie = Path.of(args[++i]);
                case "--tailles" -> tailles = Stream.of(args[++i].split(",")).map(s -> Integer.parseInt(s.trim())).toList();
                case "--texte-max" -> texteMax = Integer.parseInt(args[++i]);
                case "--graine" -> graine = Long.parseLong(args[++i]);
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    return;
                }
            }
        }
        if (sortie == null) {
            sortie = dossier.resolve("mesures");
        }

        List<Path> ordre = livresMelanges(dossier.resolve("livres"), graine);
        Metadonnees meta = Metadonnees.charger(dossier.resolve("metadonnees.tsv"));
        System.out.printf(Locale.ROOT, "Java %s, %d coeurs, mémoire max %d Mo%n", System.getProperty("java.version"),
            Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() / 1024 / 1024);
        System.out.printf(Locale.ROOT, "%d livres dans %s, graine %d, tailles %s, texte complet jusqu'à %d%n",
            ordre.size(), dossier.resolve("livres"), graine, tailles, texteMax);

        Files.createDirectories(sortie);
        try (PrintStream csvCorpus = csv(sortie.resolve("sous_corpus.csv"),
                 "n,octets,mots_indexes,vocabulaire,postings,paires,mots_vides,seuil,aretes,isoles,iterations_pagerank");
             PrintStream csvConstruction = csv(sortie.resolve("construction.csv"),
                 "n,etape,essais,moyenne_ms,ecart_type_ms");
             PrintStream csvRequetes = csv(sortie.resolve("requetes.csv"),
                 "n,type,requete,essais,moyenne_ms,ecart_type_ms,livres_trouves,mots_trouves");
             PrintStream csvTexte = csv(sortie.resolve("index_texte.csv"),
                 "n,regex,mode,essais,moyenne_ms,ecart_type_ms,livres_trouves,mots_testes")) {

            int precedent = 0;
            for (int t : tailles) {
                int n = Math.min(t, ordre.size());
                if (n <= precedent) {
                    continue; // taille plus grande que le corpus, déjà mesurée
                }
                precedent = n;
                List<Path> fichiers = new ArrayList<>(ordre.subList(0, n));
                fichiers.sort(Comparator.comparingInt(MesureCorpus::id)); // comme ConstructionIndex
                mesurer(n, fichiers, dossier, meta, n <= texteMax, graine,
                    csvCorpus, csvConstruction, csvRequetes, csvTexte);
            }
        }
        System.out.println("\nRésultats dans " + sortie);
    }

    private static void mesurer(int n, List<Path> fichiers, Path dossier, Metadonnees meta, boolean texte, long graine,
                                PrintStream csvCorpus, PrintStream csvConstruction, PrintStream csvRequetes,
                                PrintStream csvTexte) throws IOException {
        long debutN = System.nanoTime();
        long octets = 0;
        for (Path f : fichiers) {
            octets += Files.size(f);
        }
        System.out.printf(Locale.ROOT, "%n===== %d livres (%.0f Mo de texte) =====%n", n, octets / 1e6);

        // ---------- 1. index ----------
        Index index = construireIndex(fichiers); // chauffe, non comptée
        double[] tIndex = new double[ESSAIS_INDEX];
        for (int e = 0; e < ESSAIS_INDEX; e++) {
            index = null;
            System.gc();
            long debut = System.nanoTime();
            index = construireIndex(fichiers);
            tIndex[e] = ms(debut);
        }
        etape(csvConstruction, n, "index", tIndex);

        // L'index construit en mémoire est sauvé puis rechargé, comme le serveur le charge (index.bin).
        // Sans cela, les mots du vocabulaire restent éparpillés dans la mémoire (créés pendant la
        // lecture des livres) et le parcours du vocabulaire par une regex est 3 à 5 fois plus lent :
        // whale 14,5 ms au lieu de 2,8 ms sur 1 893 livres (essai du 18/11/2026, docs/notes.md).
        Path temporaire = Files.createTempFile("mesure-index", ".bin");
        index.sauver(temporaire);
        index = null;
        System.gc();
        index = Index.charger(temporaire);
        Files.delete(temporaire);

        // ---------- 2. Jaccard, graphe, PageRank ----------
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = index.idLivre(i);
        }
        double[] tEmpreintes = new double[ESSAIS_GRAPHE];
        double[] tDistances = new double[ESSAIS_GRAPHE];
        double[] tGraphe = new double[ESSAIS_GRAPHE];
        double[] tPageRank = new double[ESSAIS_GRAPHE];
        Jaccard jaccard = null;
        float[] dist = null;
        Graphe graphe = null;
        PageRank.Resultat pr = null;
        for (int e = -1; e < ESSAIS_GRAPHE; e++) { // e = -1 : chauffe, non comptée
            long t0 = System.nanoTime();
            // empreintes : mots vides et 250 mots les plus fréquents de chaque livre (parcours de l'index)
            jaccard = new Jaccard(index, Jaccard.PART_MAX_DEFAUT, ConstructionGraphe.MOTS_PAR_LIVRE_DEFAUT);
            long t1 = System.nanoTime();
            // distances : les n(n-1)/2 paires
            dist = jaccard.toutesLesDistances();
            long t2 = System.nanoTime();
            graphe = Graphe.depuisDistances(ids, dist, Graphe.seuilPourDegreMoyen(dist, n, DEGRE_MOYEN));
            long t3 = System.nanoTime();
            pr = PageRank.calculer(graphe, PageRank.AMORTISSEMENT_DEFAUT, PageRank.TOLERANCE_DEFAUT,
                PageRank.ITERATIONS_MAX_DEFAUT);
            long t4 = System.nanoTime();
            if (e >= 0) {
                tEmpreintes[e] = (t1 - t0) / 1e6;
                tDistances[e] = (t2 - t1) / 1e6;
                tGraphe[e] = (t3 - t2) / 1e6;
                tPageRank[e] = (t4 - t3) / 1e6;
            }
        }
        etape(csvConstruction, n, "empreintes", tEmpreintes);
        etape(csvConstruction, n, "distances", tDistances);
        etape(csvConstruction, n, "graphe", tGraphe);
        etape(csvConstruction, n, "pagerank", tPageRank);

        long mots = 0;
        for (int i = 0; i < n; i++) {
            mots += index.longueurLivre(i);
        }
        System.out.printf(Locale.ROOT, "%d mots indexés, vocabulaire %d, %d postings, %d paires, %d mots vides, "
                + "seuil %.4f, %d arêtes, %d isolés, PageRank %d itérations%n",
            mots, index.nbMots(), index.nbPostings(), dist.length, jaccard.nbMotsVides(), graphe.seuil(),
            graphe.nbAretes(), graphe.nbIsoles(), pr.iterations());
        csvCorpus.printf(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d,%.6f,%d,%d,%d%n", n, octets, mots, index.nbMots(),
            index.nbPostings(), dist.length, jaccard.nbMotsVides(), graphe.seuil(), graphe.nbAretes(),
            graphe.nbIsoles(), pr.iterations());
        jaccard = null;
        dist = null;

        // ---------- 3. requêtes par le moteur complet ----------
        Moteur moteur = new Moteur(index, graphe, pr.scores(), meta, dossier.resolve("livres"));
        List<Requete> requetes = new ArrayList<>();
        for (String q : MOTS_CLEFS) {
            requetes.add(new Requete("motcle", q));
        }
        for (String r : REGEXS) {
            requetes.add(new Requete("regex", r));
        }

        long finChauffe = System.nanoTime() + CHAUFFE_REQUETES_MS * 1_000_000;
        while (System.nanoTime() < finChauffe) {
            for (Requete r : requetes) {
                executer(moteur, r);
            }
        }
        Random hasard = new Random(graine);
        List<Integer> ordre = new ArrayList<>();
        for (int k = 0; k < requetes.size(); k++) {
            ordre.add(k);
        }
        double[][] temps = new double[requetes.size()][TOURS_REQUETES];
        Moteur.Reponse[] reponses = new Moteur.Reponse[requetes.size()];
        for (int tour = 0; tour < TOURS_REQUETES; tour++) {
            Collections.shuffle(ordre, hasard);
            for (int k : ordre) {
                long debut = System.nanoTime();
                reponses[k] = executer(moteur, requetes.get(k));
                temps[k][tour] = ms(debut);
            }
        }
        for (int k = 0; k < requetes.size(); k++) {
            Requete r = requetes.get(k);
            Moteur.Reponse rep = reponses[k];
            String motsTrouves = rep.nbMotsTrouves() == null ? "" : String.valueOf(rep.nbMotsTrouves());
            System.out.printf(Locale.ROOT, "  %-6s %-26s %9.3f ms ± %7.3f   %5d livres%s%n", r.type(), r.texte(),
                moyenne(temps[k]), ecartType(temps[k]), rep.resultats().size(),
                motsTrouves.isEmpty() ? "" : ", " + motsTrouves + " mots");
            csvRequetes.printf(Locale.ROOT, "%d,%s,\"%s\",%d,%.4f,%.4f,%d,%s%n", n, r.type(), r.texte(),
                TOURS_REQUETES, moyenne(temps[k]), ecartType(temps[k]), rep.resultats().size(), motsTrouves);
        }

        // ---------- 4. regex : index contre texte complet ----------
        if (texte) {
            String[] textes = new String[n];
            for (int i = 0; i < n; i++) {
                textes[i] = Files.readString(fichiers.get(i), StandardCharsets.UTF_8);
            }
            RechercheRegex rechercheRegex = new RechercheRegex(index);
            for (String regex : REGEXS_TEXTE) {
                double[] tIdx = new double[ESSAIS_REGEX_INDEX];
                RechercheRegex.Reponse repIndex = null;
                for (int e = -3; e < ESSAIS_REGEX_INDEX; e++) { // 3 appels de chauffe
                    long debut = System.nanoTime();
                    repIndex = rechercheRegex.chercher(regex, true);
                    if (e >= 0) {
                        tIdx[e] = ms(debut);
                    }
                }
                double[] tTxt = new double[ESSAIS_REGEX_TEXTE];
                List<Resultat> repTexte = null;
                for (int e = -1; e < ESSAIS_REGEX_TEXTE; e++) { // 1 appel de chauffe (plusieurs secondes)
                    long debut = System.nanoTime();
                    repTexte = chercherDansTexte(textes, regex);
                    if (e >= 0) {
                        tTxt[e] = ms(debut);
                    }
                }
                if (!repTexte.equals(repIndex.resultats())) {
                    throw new IllegalStateException("Index et texte complet ne trouvent pas les mêmes livres pour "
                        + regex + " (" + repIndex.resultats().size() + " contre " + repTexte.size() + ")");
                }
                System.out.printf(Locale.ROOT, "  %-26s index %8.3f ms ± %6.3f   texte %10.1f ms ± %7.1f   "
                        + "%5d livres (identiques)%n",
                    regex, moyenne(tIdx), ecartType(tIdx), moyenne(tTxt), ecartType(tTxt), repTexte.size());
                csvTexte.printf(Locale.ROOT, "%d,\"%s\",index,%d,%.4f,%.4f,%d,%d%n", n, regex, ESSAIS_REGEX_INDEX,
                    moyenne(tIdx), ecartType(tIdx), repIndex.resultats().size(), index.nbMots());
                csvTexte.printf(Locale.ROOT, "%d,\"%s\",texte,%d,%.4f,%.4f,%d,%d%n", n, regex, ESSAIS_REGEX_TEXTE,
                    moyenne(tTxt), ecartType(tTxt), repTexte.size(), mots);
            }
        }
        System.out.printf(Locale.ROOT, "(%d livres mesurés en %.0f s)%n", n, (System.nanoTime() - debutN) / 1e9);
    }

    // La même recherche que RechercheRegex.chercher(regex, true), sans l'index : chaque mot de chaque livre
    // est lu par le même découpage (Index.pourChaqueMotIndexable) et testé par le même automate.
    // Résultat : les livres qui contiennent au moins un mot reconnu, avec le nombre de ces mots, dans
    // le même ordre que RechercheRegex (occurrences décroissantes, puis numéro de livre).
    static List<Resultat> chercherDansTexte(String[] textes, String regex) {
        MatcherTable matcher = new MatcherTable(RechercheRegex.construireAutomate(regex, true));
        List<Resultat> resultats = new ArrayList<>();
        for (int livre = 0; livre < textes.length; livre++) {
            int[] nb = {0};
            Index.pourChaqueMotIndexable(textes[livre], mot -> {
                if (matcher.reconnait(mot)) {
                    nb[0]++;
                }
            });
            if (nb[0] > 0) {
                resultats.add(new Resultat(livre, nb[0]));
            }
        }
        resultats.sort(Comparator.comparingInt(Resultat::occurrences).reversed()
            .thenComparingInt(Resultat::livre));
        return resultats;
    }

    private static Moteur.Reponse executer(Moteur moteur, Requete r) {
        return r.type().equals("motcle")
            ? moteur.chercherMotCle(r.texte(), "produit")
            : moteur.chercherRegex(r.texte(), "index", "true", "produit");
    }

    // tous les livres ID.txt du dossier, par identifiant croissant, puis mélangés avec la graine
    private static List<Path> livresMelanges(Path dossierLivres, long graine) throws IOException {
        List<Path> fichiers;
        try (Stream<Path> stream = Files.list(dossierLivres)) {
            fichiers = new ArrayList<>(stream.filter(p -> p.getFileName().toString().matches("\\d+\\.txt"))
                .sorted(Comparator.comparingInt(MesureCorpus::id))
                .toList());
        }
        Collections.shuffle(fichiers, new Random(graine));
        return fichiers;
    }

    // comme ConstructionIndex.depuisDossier, sur une liste de fichiers
    private static Index construireIndex(List<Path> fichiers) throws IOException {
        Index.Constructeur constructeur = new Index.Constructeur();
        for (Path fichier : fichiers) {
            constructeur.ajouterLivre(id(fichier), Files.readString(fichier, StandardCharsets.UTF_8));
        }
        return constructeur.construire();
    }

    private static int id(Path fichier) {
        String nom = fichier.getFileName().toString();
        return Integer.parseInt(nom.substring(0, nom.length() - ".txt".length()));
    }

    private static void etape(PrintStream csv, int n, String etape, double[] ms) {
        System.out.printf(Locale.ROOT, "  %-9s %10.2f ms ± %8.2f  (%d essais)%n", etape, moyenne(ms), ecartType(ms),
            ms.length);
        csv.printf(Locale.ROOT, "%d,%s,%d,%.4f,%.4f%n", n, etape, ms.length, moyenne(ms), ecartType(ms));
    }

    private static PrintStream csv(Path fichier, String entete) throws IOException {
        PrintStream out = new PrintStream(Files.newOutputStream(fichier), true, StandardCharsets.UTF_8);
        out.println(entete);
        return out;
    }

    private static double ms(long debut) {
        return (System.nanoTime() - debut) / 1e6;
    }

    static double moyenne(double[] t) {
        double s = 0;
        for (double x : t) {
            s += x;
        }
        return s / t.length;
    }

    // écart type d'échantillon (division par n - 1)
    static double ecartType(double[] t) {
        if (t.length < 2) {
            return 0;
        }
        double m = moyenne(t);
        double s = 0;
        for (double x : t) {
            s += (x - m) * (x - m);
        }
        return Math.sqrt(s / (t.length - 1));
    }
}
