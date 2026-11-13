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

// Le moteur de recherche complet, utilisé par l'API.
//
// Au démarrage, il charge depuis un dossier (data/ ou data/test20/) :
//   index.bin, graphe.bin, pagerank.bin, metadonnees.tsv, livres/ID.txt (pour les extraits)
// Ensuite tout est en lecture seule : plusieurs clients peuvent chercher en même temps.
//
// Classement (tri "produit", par défaut) :
//   score = (occurrences des mots non vides / nombre de mots du livre) × PageRank du livre
// Les mots vides (présents dans plus de 50 % des livres, même seuil que pour Jaccard) restent
// dans la recherche (ET pour un mot-clef) mais ne comptent pas dans le score. Si tous les mots
// de la requête sont vides, on les compte tous.
// Autres tris, pour comparer dans le rapport : "occurrences" (occurrences totales) et "pagerank".
//
// Suggestions : les voisins dans le graphe de Jaccard des 3 premiers résultats, sauf ces 3 livres,
// du plus proche au plus loin (distance au plus proche des 3), 10 au plus.
public final class Moteur {

    public static final int NB_PREMIERS_POUR_SUGGESTIONS = 3;
    public static final int NB_SUGGESTIONS = 10;
    public static final int NB_VOISINS = 10;
    public static final int TAILLE_EXTRAIT = 1500;

    public enum Tri {
        PRODUIT, OCCURRENCES, PAGERANK;

        // null ou vide : PRODUIT
        public static Tri lire(String texte) {
            if (texte == null || texte.isBlank()) {
                return PRODUIT;
            }
            switch (texte.strip().toLowerCase(Locale.ROOT)) {
                case "produit": return PRODUIT;
                case "occurrences": return OCCURRENCES;
                case "pagerank": return PAGERANK;
                default: throw new RequeteInvalide("Tri inconnu : " + texte + " (produit, occurrences ou pagerank).");
            }
        }
    }

    public record LivreCourt(int id, String titre, String auteur) {}

    public record LivreTrouve(int id, String titre, String auteur, int occurrences, double pagerank, double score) {}

    // nbMotsTrouves et motsTrouves valent null pour une recherche par mot-clef
    public record Reponse(String requete, String type, double tempsMs, List<LivreTrouve> resultats,
                          List<LivreCourt> suggestions, Integer nbMotsTrouves, List<String> motsTrouves) {}

    public record Fiche(int id, String titre, String auteur, String urlSource, String extrait,
                        List<LivreCourt> voisins) {}

    private final Index index;
    private final Graphe graphe;
    private final double[] pagerank;      // par numéro de livre (ordre de l'index)
    private final Metadonnees meta;
    private final Path dossierLivres;
    private final boolean[] motVide;      // par numéro de mot
    private final Map<Integer, Integer> numeroParId = new HashMap<>();
    private final RechercheIndex rechercheIndex;
    private final RechercheRegex rechercheRegex;

    // ---------- chargement ----------

    public static Moteur charger(Path dossier) throws IOException {
        for (String nom : List.of("index.bin", "graphe.bin", "pagerank.bin", "metadonnees.tsv")) {
            if (!Files.exists(dossier.resolve(nom))) {
                throw new IOException("Fichier manquant : " + dossier.resolve(nom).toAbsolutePath()
                    + " (construire l'index avec ConstructionIndex puis le graphe avec ConstructionGraphe)");
            }
        }
        Index index = Index.charger(dossier.resolve("index.bin"));
        Graphe graphe = Graphe.charger(dossier.resolve("graphe.bin"));
        PageRank.Scores scores = PageRank.charger(dossier.resolve("pagerank.bin"));
        Metadonnees meta = Metadonnees.charger(dossier.resolve("metadonnees.tsv"));

        // l'index, le graphe et le PageRank doivent parler des mêmes livres, dans le même ordre
        int[] ids = new int[index.nbLivres()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = index.idLivre(i);
        }
        if (!Arrays.equals(ids, graphe.idsLivres()) || !Arrays.equals(ids, scores.idsLivres())) {
            throw new IOException("index.bin, graphe.bin et pagerank.bin ne contiennent pas les mêmes livres "
                + "(reconstruire le graphe avec ConstructionGraphe après l'index)");
        }
        return new Moteur(index, graphe, scores.scores(), meta, dossier.resolve("livres"));
    }

    Moteur(Index index, Graphe graphe, double[] pagerank, Metadonnees meta, Path dossierLivres) {
        this.index = index;
        this.graphe = graphe;
        this.pagerank = pagerank;
        this.meta = meta;
        this.dossierLivres = dossierLivres;
        this.rechercheIndex = new RechercheIndex(index);
        this.rechercheRegex = new RechercheRegex(index);

        for (int i = 0; i < index.nbLivres(); i++) {
            numeroParId.put(index.idLivre(i), i);
        }
        int dfMax = (int) Math.floor(Jaccard.PART_MAX_DEFAUT * index.nbLivres());
        motVide = new boolean[index.nbMots()];
        for (int m = 0; m < index.nbMots(); m++) {
            motVide[m] = index.fin(m) - index.debut(m) > dfMax;
        }
    }

    public int nbLivres() {
        return index.nbLivres();
    }

    public boolean estMotVide(String mot) {
        int m = index.trouver(mot);
        return m >= 0 && motVide[m];
    }

    // ---------- recherche par mot-clef ----------

    public Reponse chercherMotCle(String requete, String tri) {
        long debut = System.nanoTime();
        Tri ordre = Tri.lire(tri);

        List<String> mots = new ArrayList<>(new LinkedHashSet<>(Index.motsDeRequete(requete == null ? "" : requete)));
        if (mots.isEmpty()) {
            throw new RequeteInvalide("Aucun mot à chercher (les mots d'une seule lettre et la ponctuation sont ignorés).");
        }
        List<Resultat> trouves = rechercheIndex.motCle(requete);

        // mots qui comptent dans le score : les mots non vides (ou tous, s'ils sont tous vides)
        List<Integer> numeros = new ArrayList<>();
        for (String mot : mots) {
            int m = index.trouver(mot);
            if (m >= 0) {
                numeros.add(m);
            }
        }
        List<Integer> utiles = motsUtiles(numeros);

        int[] occUtiles = new int[trouves.size()];
        for (int r = 0; r < trouves.size(); r++) {
            for (int m : utiles) {
                occUtiles[r] += index.occurrences(m, trouves.get(r).livre());
            }
        }
        return reponse(requete, "motcle", debut, trouves, occUtiles, ordre, null, null);
    }

    // ---------- recherche par regex ----------

    public Reponse chercherRegex(String regex, String mode, String tri) {
        long debut = System.nanoTime();
        String m = mode == null || mode.isBlank() ? "index" : mode.strip().toLowerCase(Locale.ROOT);
        if (m.equals("texte")) {
            throw new RequeteInvalide("La recherche dans le texte complet n'est pas encore disponible : utiliser mode=index.");
        }
        if (!m.equals("index")) {
            throw new RequeteInvalide("Mode inconnu : " + mode + " (index ou texte).");
        }
        Tri ordre = Tri.lire(tri);

        RechercheRegex.Reponse rep = rechercheRegex.chercher(regex);

        // occurrences des mots trouvés non vides (ou de tous, s'ils sont tous vides), par livre
        int[] parLivre = new int[index.nbLivres()];
        for (int mot : motsUtiles(rep.numerosMots())) {
            for (int p = index.debut(mot); p < index.fin(mot); p++) {
                parLivre[index.livreDuPosting(p)] += index.occurrencesDuPosting(p);
            }
        }
        List<Resultat> trouves = rep.resultats();
        int[] occUtiles = new int[trouves.size()];
        for (int r = 0; r < trouves.size(); r++) {
            occUtiles[r] = parLivre[trouves.get(r).livre()];
        }
        return reponse(regex, "regex", debut, trouves, occUtiles, ordre, rep.nbMotsTrouves(), rep.motsTrouves());
    }

    private List<Integer> motsUtiles(List<Integer> numeros) {
        List<Integer> utiles = new ArrayList<>();
        for (int m : numeros) {
            if (!motVide[m]) {
                utiles.add(m);
            }
        }
        return utiles.isEmpty() ? numeros : utiles;
    }

    // ---------- classement et suggestions ----------

    // un résultat avec son numéro de livre (pour le graphe)
    private record Classe(int livre, LivreTrouve trouve) {}

    private Reponse reponse(String requete, String type, long debut, List<Resultat> trouves, int[] occUtiles,
                            Tri ordre, Integer nbMotsTrouves, List<String> motsTrouves) {
        List<Classe> classes = new ArrayList<>(trouves.size());
        for (int r = 0; r < trouves.size(); r++) {
            int livre = trouves.get(r).livre();
            int longueur = index.longueurLivre(livre);
            double score = longueur == 0 ? 0 : (double) occUtiles[r] / longueur * pagerank[livre];
            Metadonnees.Livre l = meta.livre(index.idLivre(livre));
            classes.add(new Classe(livre, new LivreTrouve(index.idLivre(livre), titre(l, livre), auteur(l),
                trouves.get(r).occurrences(), pagerank[livre], score)));
        }

        Comparator<Classe> parScore = Comparator.comparingDouble((Classe c) -> c.trouve().score()).reversed();
        Comparator<Classe> parId = Comparator.comparingInt((Classe c) -> c.trouve().id());
        Comparator<Classe> comparateur = switch (ordre) {
            case PRODUIT -> parScore
                .thenComparing(Comparator.comparingInt((Classe c) -> c.trouve().occurrences()).reversed())
                .thenComparing(parId);
            case OCCURRENCES -> Comparator.comparingInt((Classe c) -> c.trouve().occurrences()).reversed()
                .thenComparing(parScore).thenComparing(parId);
            case PAGERANK -> Comparator.comparingDouble((Classe c) -> c.trouve().pagerank()).reversed()
                .thenComparing(parScore).thenComparing(parId);
        };
        classes.sort(comparateur);

        List<LivreTrouve> resultats = new ArrayList<>(classes.size());
        for (Classe c : classes) {
            resultats.add(c.trouve());
        }
        List<LivreCourt> suggestions = suggestions(classes);

        double tempsMs = Math.round((System.nanoTime() - debut) / 1e4) / 100.0;
        return new Reponse(requete, type, tempsMs, resultats, suggestions, nbMotsTrouves, motsTrouves);
    }

    private List<LivreCourt> suggestions(List<Classe> classes) {
        int nbPremiers = Math.min(NB_PREMIERS_POUR_SUGGESTIONS, classes.size());
        boolean[] premier = new boolean[index.nbLivres()];
        for (int r = 0; r < nbPremiers; r++) {
            premier[classes.get(r).livre()] = true;
        }

        // pour chaque voisin : sa distance au plus proche des premiers résultats
        Map<Integer, Float> distance = new HashMap<>();
        for (int r = 0; r < nbPremiers; r++) {
            int livre = classes.get(r).livre();
            for (int p = graphe.debut(livre); p < graphe.fin(livre); p++) {
                int v = graphe.voisin(p);
                if (!premier[v]) {
                    distance.merge(v, graphe.distance(p), Math::min);
                }
            }
        }
        List<Integer> voisins = new ArrayList<>(distance.keySet());
        voisins.sort(Comparator.comparing((Integer v) -> distance.get(v)).thenComparing(v -> index.idLivre(v)));

        List<LivreCourt> liste = new ArrayList<>();
        for (int v : voisins.subList(0, Math.min(NB_SUGGESTIONS, voisins.size()))) {
            liste.add(court(v));
        }
        return liste;
    }

    // ---------- page d'un livre ----------

    public Fiche livre(int id) {
        Integer numero = numeroParId.get(id);
        if (numero == null) {
            throw new LivreIntrouvable(id);
        }
        Metadonnees.Livre l = meta.livre(id);
        List<LivreCourt> voisins = new ArrayList<>();
        for (int p = graphe.debut(numero); p < graphe.fin(numero) && voisins.size() < NB_VOISINS; p++) {
            voisins.add(court(graphe.voisin(p)));
        }
        String url = l != null && !l.urlSource().isBlank() ? l.urlSource() : "https://www.gutenberg.org/ebooks/" + id;
        return new Fiche(id, titre(l, numero), auteur(l), url, extrait(dossierLivres.resolve(id + ".txt")), voisins);
    }

    // Début du texte : environ TAILLE_EXTRAIT caractères, coupés sur un espace.
    // Les retours à la ligne à l'intérieur d'un paragraphe deviennent des espaces ;
    // les paragraphes restent séparés par une ligne vide.
    static String extrait(Path fichier) {
        if (!Files.exists(fichier)) {
            return "";
        }
        char[] tampon = new char[TAILLE_EXTRAIT * 4];
        int lus = 0;
        try (BufferedReader in = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            int n;
            while (lus < tampon.length && (n = in.read(tampon, lus, tampon.length - lus)) > 0) {
                lus += n;
            }
        } catch (IOException e) {
            return "";
        }
        String texte = new String(tampon, 0, lus).replace("\r", "");

        StringBuilder sb = new StringBuilder();
        for (String paragraphe : texte.split("\n\\s*\n")) {
            String p = paragraphe.strip().replaceAll("\\s+", " ");
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(p);
            if (sb.length() >= TAILLE_EXTRAIT) {
                break;
            }
        }
        if (sb.length() <= TAILLE_EXTRAIT) {
            return sb.toString();
        }
        int coupe = sb.lastIndexOf(" ", TAILLE_EXTRAIT);
        return sb.substring(0, coupe > 0 ? coupe : TAILLE_EXTRAIT) + " …";
    }

    // ---------- petits outils ----------

    private LivreCourt court(int livre) {
        Metadonnees.Livre l = meta.livre(index.idLivre(livre));
        return new LivreCourt(index.idLivre(livre), titre(l, livre), auteur(l));
    }

    private String titre(Metadonnees.Livre l, int livre) {
        return l != null ? l.titre() : "Livre " + index.idLivre(livre);
    }

    private static String auteur(Metadonnees.Livre l) {
        return l != null ? l.auteur() : "";
    }
}
