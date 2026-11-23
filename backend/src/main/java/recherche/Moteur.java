package recherche;

import corpus.Metadonnees;
import graphe.Graphe;
import graphe.Jaccard;
import graphe.PageRank;
import index.Index;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Le moteur utilisé par l'API. Tout est chargé au départ depuis data/
// Les requêtes ne modifient rien, donc plusieurs clients peuvent chercher
public final class Moteur {

    public static final int NB_PREMIERS_POUR_SUGGESTIONS = 3;
    public static final int NB_RESULTATS_EXPLORES = 10;
    public static final int NB_SUGGESTIONS = 10;
    public static final int NB_VOISINS = 10;
    public static final int TAILLE_EXTRAIT = 1500;
    public static final int NB_TOURS_CHAUFFE = 5;

    public enum Tri {
        PRODUIT, OCCURRENCES, PAGERANK;

        // sans tri précisé, on prend celui-ci
        public static Tri lire(String v) {
            if (v == null || v.isBlank()) {
                return PRODUIT;

            }
            switch (v.strip().toLowerCase(Locale.ROOT)) {
                case "produit":
                    return PRODUIT;
                case "occurrences":
                    return OCCURRENCES;
                case "pagerank":
                    return PAGERANK;
                default:
                    throw new RequeteInvalide("Tri inconnu : " + v + " (produit, occurrences ou pagerank).");
            }
        }
    }

    public record LivreCourt(int id, String titre, String auteur) {
    }

    public record LivreTrouve(int id, String titre, String auteur, int occurrences, double pagerank, double score) {
    }

    // ces trois champs restent null pour les mots-clés
    public record Reponse(String requete, String type, double tempsMs, List<LivreTrouve> resultats,
            List<LivreCourt> suggestions, Integer nbMotsTrouves, List<String> motsTrouves,
            Boolean motEntier) {
    }

    public record Fiche(int id, String titre, String auteur, String urlSource, String extrait,
            List<LivreCourt> voisins) {
    }

    private final Index index;
    private final Graphe graphe;
    private final double[] pagerank; // même ordre que l'index
    private final Metadonnees meta;
    private final Path dossierLivres;
    private final boolean[] motVide; // les mots trop présents
    private final Map<Integer, Integer> numeroParId = new HashMap<>();
    private final RechercheIndex rechercheIndex;
    private final RechercheRegex rechercheRegex;

    // chargement

    public static Moteur charger(Path base) throws IOException {
        for (String f : List.of("index.bin", "graphe.bin", "pagerank.bin", "metadonnees.tsv")) {
            if (!Files.exists(base.resolve(f))) {
                throw new IOException("Fichier manquant : " + base.resolve(f).toAbsolutePath()
                        + " (construire l'index avec ConstructionIndex puis le graphe avec ConstructionGraphe)");
            }
        }
        Index idx = Index.charger(base.resolve("index.bin"));
        Graphe g = Graphe.charger(base.resolve("graphe.bin"));
        PageRank.Scores rangs = PageRank.charger(base.resolve("pagerank.bin"));
        Metadonnees md = Metadonnees.charger(base.resolve("metadonnees.tsv"));

        // on vérifie que les fichiers vont ensemble
        int[] ids0 = new int[idx.nbLivres()];
        for (int i = 0; i < ids0.length; i++) {

            ids0[i] = idx.idLivre(i);
        }
        if (!Arrays.equals(ids0, g.idsLivres()) || !Arrays.equals(ids0, rangs.idsLivres())) {
            throw new IOException("index.bin, graphe.bin et pagerank.bin ne contiennent pas les mêmes livres "
                    + "(reconstruire le graphe avec ConstructionGraphe après l'index)");
        }
        return new Moteur(idx, g, rangs.scores(), md, base.resolve("livres"));
    }

    Moteur(Index idx, Graphe g, double[] pr, Metadonnees md, Path rep) {
        this.index = idx;
        this.graphe = g;
        this.pagerank = pr;
        this.meta = md;
        this.dossierLivres = rep;
        this.rechercheIndex = new RechercheIndex(idx);

        this.rechercheRegex = new RechercheRegex(idx);

        for (int i = 0; i < idx.nbLivres(); i++) {
            numeroParId.put(idx.idLivre(i), i);
        }
        int lim = (int) Math.floor(Jaccard.PART_MAX_DEFAUT * idx.nbLivres()); // seuil des mots vides
        motVide = new boolean[idx.nbMots()];
        for (int m = 0; m < idx.nbMots(); m++) {
            motVide[m] = idx.fin(m) - idx.debut(m) > lim;
        }
    }

    public int nbLivres() {
        return index.nbLivres();
    }

    // quelques recherches au démarrage pour éviter un premier appel trop lent
    public long chauffer() {
        long t0 = System.nanoTime();
        for (int t = 0; t < NB_TOURS_CHAUFFE; t++) {
            chercherMotCle("the whale", null);
            chercherMotCle("ship sea", "occurrences");
            chercherRegex("wh(a|e)le", "index", null, null);
            chercherRegex("s(a|g|r)*on", "index", "false", "pagerank");
            if (index.nbLivres() > 0) {
                livre(index.idLivre(0));
            }
        }
        return (System.nanoTime() - t0) / 1_000_000; // millisecondes
    }

    public boolean estMotVide(String w) {
        int k = index.trouver(w);
        return k >= 0 && motVide[k];
    }

    // mots-clés

    public Reponse chercherMotCle(String q, String triTxt) {
        long t0 = System.nanoTime();

        Tri choix = Tri.lire(triTxt);

        List<String> ws = new ArrayList<>(new LinkedHashSet<>(Index.motsDeRequete(q == null ? "" : q)));
        if (ws.isEmpty()) {
            throw new RequeteInvalide(
                    "Aucun mot à chercher (les mots d'une seule lettre et la ponctuation sont ignorés).");
        }
        List<Resultat> hits = rechercheIndex.motCle(q);

        // on garde les mots utiles au calcul du score
        List<Integer> num = new ArrayList<>();
        for (String w : ws) {
            int k = index.trouver(w);
            if (k >= 0) {
                num.add(k);
            }
        }
        List<Integer> bons = motsUtiles(num); // évite les mots trop fréquents

        int[] freq = new int[hits.size()];
        for (int i = 0; i < hits.size(); i++) {
            for (int k : bons) {

                freq[i] += index.occurrences(k, hits.get(i).livre());
            }
        }
        return reponse(q, "motcle", t0, hits, freq, choix, null, null, null);
    }

    // regex

    // true = mot entier ; false = sous-chaîne
    public Reponse chercherRegex(String rx, String mode0, String entTxt, String triTxt) {
        long t0 = System.nanoTime();
        String voie = mode0 == null || mode0.isBlank() ? "index" : mode0.strip().toLowerCase(Locale.ROOT);

        if (voie.equals("texte")) {
            throw new RequeteInvalide("La recherche dans le texte complet n'est pas disponible dans l'application "
                    + "(elle sert aux mesures) : utiliser mode=index.");
        }
        if (!voie.equals("index")) {
            throw new RequeteInvalide("Mode inconnu : " + mode0 + " (index ou texte).");
        }
        boolean plein = lireMotEntier(entTxt);
        Tri choix = Tri.lire(triTxt);

        RechercheRegex.Reponse rep = rechercheRegex.chercher(rx, plein);

        // on compte les occurrences utiles pour chaque livre
        int[] compte = new int[index.nbLivres()];
        for (int w : motsUtiles(rep.numerosMots())) {
            for (int j = index.debut(w); j < index.fin(w); j++) {
                compte[index.livreDuPosting(j)] += index.occurrencesDuPosting(j);
            }
        }
        List<Resultat> hits = rep.resultats();
        int[] freq = new int[hits.size()];
        for (int i = 0; i < hits.size(); i++) {
            freq[i] = compte[hits.get(i).livre()];
        }
        return reponse(rx, "regex", t0, hits, freq, choix, rep.nbMotsTrouves(), rep.motsTrouves(),
                plein);
    }

    private static boolean lireMotEntier(String v) {
        if (v == null || v.isBlank()) {
            return true;
        }
        switch (v.strip().toLowerCase(Locale.ROOT)) {
            case "true":
                return true;
            case "false":
                return false;
            default:
                throw new RequeteInvalide("motEntier doit valoir true ou false : " + v);
        }
    }

    private List<Integer> motsUtiles(List<Integer> num) {

        List<Integer> bons = new ArrayList<>();
        for (int k : num) {
            if (!motVide[k]) {

                bons.add(k);
            }
        }
        return bons.isEmpty() ? num : bons;
    }

    // classement + suggestions

    // on garde aussi la position dans le graphe
    private record Classe(int livre, LivreTrouve trouve) {
    }

    private Reponse reponse(String q, String typ, long t0, List<Resultat> hits, int[] freq,
            Tri choix, Integer nbW, List<String> motsOK, Boolean plein) {
        List<Classe> rangs = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            int b = hits.get(i).livre();
            int lg = index.longueurLivre(b);
            double sc = lg == 0 ? 0 : (double) freq[i] / lg * pagerank[b];
            Metadonnees.Livre md = meta.livre(index.idLivre(b));
            rangs.add(new Classe(b, new LivreTrouve(index.idLivre(b), titre(md, b), auteur(md),
                    hits.get(i).occurrences(), pagerank[b], sc)));
        }

        Comparator<Classe> parSc = Comparator.comparingDouble((Classe c) -> c.trouve().score()).reversed();
        Comparator<Classe> parNum = Comparator.comparingInt((Classe c) -> c.trouve().id());
        Comparator<Classe> cmp = switch (choix) {
            case PRODUIT -> parSc
                    .thenComparing(Comparator.comparingInt((Classe c) -> c.trouve().occurrences()).reversed())
                    .thenComparing(parNum);
            case OCCURRENCES -> Comparator.comparingInt((Classe c) -> c.trouve().occurrences()).reversed()
                    .thenComparing(parSc).thenComparing(parNum);

            case PAGERANK -> Comparator.comparingDouble((Classe c) -> c.trouve().pagerank()).reversed()
                    .thenComparing(parSc).thenComparing(parNum);
        };
        rangs.sort(cmp);

        List<LivreTrouve> hitsOK = new ArrayList<>(rangs.size());
        for (Classe c : rangs) {
            hitsOK.add(c.trouve());

        }
        List<LivreCourt> suggestions = suggestions(rangs);

        double duree = Math.round((System.nanoTime() - t0) / 1e4) / 100.0;
        return new Reponse(q, typ, duree, hitsOK, suggestions, nbW, motsOK, plein);
    }

    private List<LivreCourt> suggestions(List<Classe> rangs) {
        // on part des meilleurs résultats qui ont des voisins
        List<Integer> dep = new ArrayList<>();
        boolean[] pris = new boolean[index.nbLivres()];
        for (int i = 0; i < Math.min(NB_RESULTATS_EXPLORES, rangs.size())
                && dep.size() < NB_PREMIERS_POUR_SUGGESTIONS; i++) {
            int b = rangs.get(i).livre();
            if (graphe.degre(b) > 0) {
                dep.add(b);
                pris[b] = true;
            }

        }

        // distance au meilleur voisin de départ
        Map<Integer, Float> dist = new HashMap<>();
        for (int b : dep) {
            for (int j = graphe.debut(b); j < graphe.fin(b); j++) {
                int v0 = graphe.voisin(j);
                if (!pris[v0]) {
                    dist.merge(v0, graphe.distance(j), Math::min);

                }
            }
        }
        List<Integer> candidats = new ArrayList<>(dist.keySet());
        candidats.sort(Comparator.comparing((Integer v0) -> dist.get(v0)).thenComparing(v0 -> index.idLivre(v0)));

        List<LivreCourt> res = new ArrayList<>();
        for (int v0 : candidats.subList(0, Math.min(NB_SUGGESTIONS, candidats.size()))) {
            res.add(court(v0));
        }
        return res;
    }

    // fiche du livre

    public Fiche livre(int id) {
        Integer pos = numeroParId.get(id);
        if (pos == null) {
            throw new LivreIntrouvable(id);
        }

        Metadonnees.Livre md = meta.livre(id);
        List<LivreCourt> proches = new ArrayList<>();
        for (int j = graphe.debut(pos); j < graphe.fin(pos) && proches.size() < NB_VOISINS; j++) {
            proches.add(court(graphe.voisin(j)));

        }
        String lien = md != null && !md.urlSource().isBlank() ? md.urlSource()
                : "https://www.gutenberg.org/ebooks/" + id;
        return new Fiche(id, titre(md, pos), auteur(md), lien, extrait(dossierLivres.resolve(id + ".txt")), proches);
    }

    // un bout du texte, sans couper un mot au milieu
    static String extrait(Path f) {
        if (!Files.exists(f)) {
            return "";
        }
        char[] buf = new char[TAILLE_EXTRAIT * 4];
        int lu = 0;
        try (BufferedReader br = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            int nb;
            while (lu < buf.length && (nb = br.read(buf, lu, buf.length - lu)) > 0) {
                lu += nb;
            }
        } catch (IOException e) {
            return "";
        }
        String txt = new String(buf, 0, lu).replace("\r", "");

        StringBuilder b = new StringBuilder();
        for (String bloc : txt.split("\n\\s*\n")) {
            String s = bloc.strip().replaceAll("\\s+", " ");

            if (s.isEmpty()) {
                continue;

            }

            if (b.length() > 0) {
                b.append("\n\n");
            }
            b.append(s);

            if (b.length() >= TAILLE_EXTRAIT) {
                break;
            }

        }
        if (b.length() <= TAILLE_EXTRAIT) {
            return b.toString();
        }
        int cut = b.lastIndexOf(" ", TAILLE_EXTRAIT);
        return b.substring(0, cut > 0 ? cut : TAILLE_EXTRAIT) + " …";
    }

    // petits utilitaires

    private LivreCourt court(int b) {
        Metadonnees.Livre md = meta.livre(index.idLivre(b));
        return new LivreCourt(index.idLivre(b), titre(md, b), auteur(md));
    }

    private String titre(Metadonnees.Livre md, int b) {
        return md != null ? md.titre() : "Livre " + index.idLivre(b);
    }

    private static String auteur(Metadonnees.Livre md) {
        return md != null ? md.auteur() : "";
    }
}
