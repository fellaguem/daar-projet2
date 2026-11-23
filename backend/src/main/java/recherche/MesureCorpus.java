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

// Quelques mesures pour les courbes du rapport
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

  private record Requete(String type, String texte) {
  }

  public static void main(String[] args) throws IOException {
    Path base = Path.of("data");
    Path dest = null;
    List<Integer> sizes = List.of(100, 200, 400, 800, 1200, 1893);
    int lim = 800;
    long seed = 42;

    for (int a = 0; a < args.length; a++) {
      switch (args[a]) {
        case "--dossier" -> base = Path.of(args[++a]);

        case "--sortie" -> dest = Path.of(args[++a]);
        case "--tailles" -> sizes = Stream.of(args[++a].split(",")).map(acc -> Integer.parseInt(acc.trim())).toList();
        case "--texte-max" -> lim = Integer.parseInt(args[++a]);
        case "--graine" -> seed = Long.parseLong(args[++a]);
        default -> {
          System.err.println("Option inconnue : " + args[a]);
          return;
        }
      }
    }
    if (dest == null) {
      dest = base.resolve("mesures");
    }

    List<Path> mix = livresMelanges(base.resolve("livres"), seed); // mêmes livres à chaque essai
    Metadonnees md = Metadonnees.charger(base.resolve("metadonnees.tsv"));
    System.out.printf(Locale.ROOT, "Java %s, %d coeurs, mémoire max %d Mo%n", System.getProperty("java.version"),
        Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() / 1024 / 1024);
    System.out.printf(Locale.ROOT, "%d livres dans %s, graine %d, tailles %s, texte complet jusqu'à %d%n",
        mix.size(), base.resolve("livres"), seed, sizes, lim);

    Files.createDirectories(dest);
    try (PrintStream oc = csv(dest.resolve("sous_corpus.csv"),
        "n,octets,mots_indexes,vocabulaire,postings,paires,mots_vides,seuil,aretes,isoles,iterations_pagerank");
        PrintStream ob = csv(dest.resolve("construction.csv"),
            "n,etape,essais,moyenne_ms,ecart_type_ms");
        PrintStream oq = csv(dest.resolve("requetes.csv"),
            "n,type,requete,essais,moyenne_ms,ecart_type_ms,livres_trouves,mots_trouves");
        PrintStream ot = csv(dest.resolve("index_texte.csv"),
            "n,regex,mode,essais,moyenne_ms,ecart_type_ms,livres_trouves,mots_testes")) {

      int prev = 0;
      for (int tab : sizes) {
        int nl = Math.min(tab, mix.size());
        if (nl <= prev) {
          continue;
        }
        prev = nl;
        List<Path> docs = new ArrayList<>(mix.subList(0, nl));
        docs.sort(Comparator.comparingInt(MesureCorpus::id)); // IDs dans l’ordre
        mesurer(nl, docs, base, md, nl <= lim, seed,
            oc, ob, oq, ot);
      }
    }
    System.out.println("\nRésultats dans " + dest);
  }

  private static void mesurer(int nl, List<Path> docs, Path base, Metadonnees md, boolean texte, long seed,
      PrintStream oc, PrintStream ob, PrintStream oq,
      PrintStream ot) throws IOException {

    long tAll = System.nanoTime();
    long totalB = 0;
    for (Path pp : docs) {
      totalB += Files.size(pp);
    }
    System.out.printf(Locale.ROOT, "%n===== %d livres (%.0f Mo de texte) =====%n", nl, totalB / 1e6);

    Index ix = construireIndex(docs); // chauffe, on ne la compte pas
    double[] tIx = new double[ESSAIS_INDEX];
    for (int v = 0; v < ESSAIS_INDEX; v++) {
      ix = null;
      System.gc();
      long tic = System.nanoTime();
      ix = construireIndex(docs);
      tIx[v] = ms(tic);
    }
    etape(ob, nl, "index", tIx);

    Path scratch = Files.createTempFile("mesure-index", ".bin"); // on recharge comme côté serveur
    ix.sauver(scratch);
    ix = null;
    System.gc();
    ix = Index.charger(scratch);
    Files.delete(scratch);

    int[] keys = new int[nl];
    for (int a = 0; a < nl; a++) {
      keys[a] = ix.idLivre(a);
    }
    double[] tSig = new double[ESSAIS_GRAPHE];
    double[] tD = new double[ESSAIS_GRAPHE];
    double[] tG = new double[ESSAIS_GRAPHE];
    double[] tPr = new double[ESSAIS_GRAPHE];
    Jaccard jac = null;
    float[] dd = null;
    Graphe g = null;
    PageRank.Resultat rk = null;
    for (int v = -1; v < ESSAIS_GRAPHE; v++) { // -1 : juste pour chauffer
      long t0 = System.nanoTime();

      jac = new Jaccard(ix, Jaccard.PART_MAX_DEFAUT, ConstructionGraphe.MOTS_PAR_LIVRE_DEFAUT);

      long t1 = System.nanoTime();

      dd = jac.toutesLesDistances();
      long t2 = System.nanoTime();
      g = Graphe.depuisDistances(keys, dd, Graphe.seuilPourDegreMoyen(dd, nl, DEGRE_MOYEN));
      long t3 = System.nanoTime();

      rk = PageRank.calculer(g, PageRank.AMORTISSEMENT_DEFAUT, PageRank.TOLERANCE_DEFAUT,
          PageRank.ITERATIONS_MAX_DEFAUT);
      long t4 = System.nanoTime();

      if (v >= 0) {
        tSig[v] = (t1 - t0) / 1e6;
        tD[v] = (t2 - t1) / 1e6;
        tG[v] = (t3 - t2) / 1e6;
        tPr[v] = (t4 - t3) / 1e6;

      }
    }
    etape(ob, nl, "empreintes", tSig);
    etape(ob, nl, "distances", tD);
    etape(ob, nl, "graphe", tG);
    etape(ob, nl, "pagerank", tPr);

    long wc = 0; // total des mots
    for (int a = 0; a < nl; a++) {
      wc += ix.longueurLivre(a);
    }
    System.out.printf(Locale.ROOT, "%d mots indexés, vocabulaire %d, %d postings, %d paires, %d mots vides, "
        + "seuil %.4f, %d arêtes, %d isolés, PageRank %d itérations%n",
        wc, ix.nbMots(), ix.nbPostings(), dd.length, jac.nbMotsVides(), g.seuil(),
        g.nbAretes(), g.nbIsoles(), rk.iterations());
    oc.printf(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d,%.6f,%d,%d,%d%n", nl, totalB, wc, ix.nbMots(),
        ix.nbPostings(), dd.length, jac.nbMotsVides(), g.seuil(), g.nbAretes(),
        g.nbIsoles(), rk.iterations());
    jac = null;
    dd = null;

    Moteur eng = new Moteur(ix, g, rk.scores(), md, base.resolve("livres"));
    List<Requete> rqList = new ArrayList<>();
    for (String kw : MOTS_CLEFS) {
      rqList.add(new Requete("motcle", kw));
    }
    for (String rr : REGEXS) {
      rqList.add(new Requete("regex", rr));
    }

    long warmEnd = System.nanoTime() + CHAUFFE_REQUETES_MS * 1_000_000; // 2 s de chauffe
    while (System.nanoTime() < warmEnd) {
      for (Requete rr : rqList) {
        executer(eng, rr);
      }
    }
    Random dice = new Random(seed);
    List<Integer> mix = new ArrayList<>();
    for (int u = 0; u < rqList.size(); u++) {
      mix.add(u);
    }
    double[][] tim = new double[rqList.size()][TOURS_REQUETES];
    Moteur.Reponse[] answers = new Moteur.Reponse[rqList.size()];
    for (int rot = 0; rot < TOURS_REQUETES; rot++) {
      Collections.shuffle(mix, dice); // ordre différent à chaque tour
      for (int u : mix) {

        long tic = System.nanoTime();
        answers[u] = executer(eng, rqList.get(u));
        tim[u][rot] = ms(tic);
      }
    }
    for (int u = 0; u < rqList.size(); u++) {
      Requete rr = rqList.get(u);
      Moteur.Reponse ans = answers[u];
      String found = ans.nbMotsTrouves() == null ? "" : String.valueOf(ans.nbMotsTrouves());
      System.out.printf(Locale.ROOT, "  %-6s %-26s %9.3f ms ± %7.3f   %5d livres%s%n", rr.type(), rr.texte(),
          moyenne(tim[u]), ecartType(tim[u]), ans.resultats().size(),
          found.isEmpty() ? "" : ", " + found + " mots");
      oq.printf(Locale.ROOT, "%d,%s,\"%s\",%d,%.4f,%.4f,%d,%s%n", nl, rr.type(), rr.texte(),
          TOURS_REQUETES, moyenne(tim[u]), ecartType(tim[u]), ans.resultats().size(), found);
    }

    if (texte) {
      String[] raw = new String[nl];
      for (int a = 0; a < nl; a++) {
        raw[a] = Files.readString(docs.get(a), StandardCharsets.UTF_8);
      }
      RechercheRegex rx0 = new RechercheRegex(ix);
      for (String pat : REGEXS_TEXTE) { // index vs texte brut
        double[] t0i = new double[ESSAIS_REGEX_INDEX];
        RechercheRegex.Reponse hitI = null;
        for (int v = -3; v < ESSAIS_REGEX_INDEX; v++) {
          long tic = System.nanoTime();
          hitI = rx0.chercher(pat, true);
          if (v >= 0) {
            t0i[v] = ms(tic);
          }
        }
        double[] t0t = new double[ESSAIS_REGEX_TEXTE];
        List<Resultat> hitT = null;
        for (int v = -1; v < ESSAIS_REGEX_TEXTE; v++) {
          long tic = System.nanoTime();
          hitT = chercherDansTexte(raw, pat);
          if (v >= 0) {
            t0t[v] = ms(tic);
          }

        }
        if (!hitT.equals(hitI.resultats())) {
          throw new IllegalStateException("Index et texte complet ne trouvent pas les mêmes livres pour "
              + pat + " (" + hitI.resultats().size() + " contre " + hitT.size() + ")");
        }
        System.out.printf(Locale.ROOT, "  %-26s index %8.3f ms ± %6.3f   texte %10.1f ms ± %7.1f   "
            + "%5d livres (identiques)%n",
            pat, moyenne(t0i), ecartType(t0i), moyenne(t0t), ecartType(t0t), hitT.size());

        ot.printf(Locale.ROOT, "%d,\"%s\",index,%d,%.4f,%.4f,%d,%d%n", nl, pat, ESSAIS_REGEX_INDEX,
            moyenne(t0i), ecartType(t0i), hitI.resultats().size(), ix.nbMots());
        ot.printf(Locale.ROOT, "%d,\"%s\",texte,%d,%.4f,%.4f,%d,%d%n", nl, pat, ESSAIS_REGEX_TEXTE,
            moyenne(t0t), ecartType(t0t), hitT.size(), wc);
      }
    }
    System.out.printf(Locale.ROOT, "(%d livres mesurés en %.0f s)%n", nl, (System.nanoTime() - tAll) / 1e9);

  }
  // même découpage en mots, mais sans l’index

  static List<Resultat> chercherDansTexte(String[] raw, String pat) {
    MatcherTable dfa = new MatcherTable(RechercheRegex.construireAutomate(pat, true));
    List<Resultat> foundBooks = new ArrayList<>();
    for (int bk = 0; bk < raw.length; bk++) {
      int[] occ = { 0 }; // petit compteur pour la lambda
      Index.pourChaqueMotIndexable(raw[bk], mot -> {
        if (dfa.reconnait(mot)) {
          occ[0]++;
        }
      });
      if (occ[0] > 0) {
        foundBooks.add(new Resultat(bk, occ[0]));
      }
    }
    foundBooks.sort(Comparator.comparingInt(Resultat::occurrences).reversed()
        .thenComparingInt(Resultat::livre));
    return foundBooks;

  }

  private static Moteur.Reponse executer(Moteur eng, Requete rr) {
    return rr.type().equals("motcle")
        ? eng.chercherMotCle(rr.texte(), "produit")
        : eng.chercherRegex(rr.texte(), "index", "true", "produit");
  }

  private static List<Path> livresMelanges(Path src, long seed) throws IOException {
    List<Path> docs;
    try (Stream<Path> flux = Files.list(src)) {
      docs = new ArrayList<>(flux.filter(p -> p.getFileName().toString().matches("\\d+\\.txt"))
          .sorted(Comparator.comparingInt(MesureCorpus::id))
          .toList());
    }
    Collections.shuffle(docs, new Random(seed));

    return docs;
  }

  private static Index construireIndex(List<Path> docs) throws IOException {
    Index.Constructeur bat = new Index.Constructeur();
    for (Path fic : docs) {

      bat.ajouterLivre(id(fic), Files.readString(fic, StandardCharsets.UTF_8));
    }
    return bat.construire();

  }

  private static int id(Path fic) {
    String nm = fic.getFileName().toString();
    return Integer.parseInt(nm.substring(0, nm.length() - ".txt".length()));
  }

  private static void etape(PrintStream csv, int nl, String etape, double[] ms) {
    System.out.printf(Locale.ROOT, "  %-9s %10.2f ms ± %8.2f  (%d essais)%n", etape, moyenne(ms), ecartType(ms),
        ms.length);
    csv.printf(Locale.ROOT, "%d,%s,%d,%.4f,%.4f%n", nl, etape, ms.length, moyenne(ms), ecartType(ms));

  }

  private static PrintStream csv(Path fic, String head) throws IOException {
    PrintStream ps = new PrintStream(Files.newOutputStream(fic), true, StandardCharsets.UTF_8);
    ps.println(head);
    return ps;
  }

  private static double ms(long tic) {
    return (System.nanoTime() - tic) / 1e6;
  }

  static double moyenne(double[] tab) {
    double acc = 0;
    for (double val : tab) {
      acc += val;

    }
    return acc / tab.length;
  }
  // version échantillon, donc n - 1

  static double ecartType(double[] tab) {
    if (tab.length < 2) {
      return 0;
    }
    double avg = moyenne(tab);
    double acc = 0;
    for (double val : tab) {
      acc += (val - avg) * (val - avg);
    }
    return Math.sqrt(acc / (tab.length - 1));
  }
}
