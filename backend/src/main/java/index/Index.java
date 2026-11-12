package index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Index inversé : pour chaque mot, la liste des livres qui le contiennent
// avec le nombre d'occurrences dans chacun.
//
// Tout est stocké dans des tableaux d'entiers (pas une table par livre) :
//   mots[m]                      le m-ième mot, vocabulaire trié (recherche par dichotomie)
//   debut[m] .. debut[m+1]-1     la plage des "postings" du mot m
//   livres[p], occurrences[p]    pour le posting p : numéro du livre et nombre d'occurrences
// Les postings d'un mot sont triés par numéro de livre.
//
// Le "numéro" d'un livre va de 0 à nbLivres-1 ; idLivre(numero) donne son identifiant Gutenberg.
public final class Index {

    private static final int MAGIE = 0x49445832; // "IDX2", pour reconnaître le format du fichier

    // Les mots d'une seule lettre ("s" de "whale's", "t" de "don't", "a", "i") ne sont pas indexés.
    // Le Tokeniseur, lui, les compte : c'est ici, à la construction de l'index et à la lecture
    // d'une requête, que la même règle est appliquée des deux côtés.
    public static final int LONGUEUR_MINIMUM_MOT = 2;

    // les mots d'un texte qui entrent dans l'index
    public static void pourChaqueMotIndexable(CharSequence texte, Consumer<String> action) {
        Tokeniseur.pourChaqueMot(texte, mot -> {
            if (mot.length() >= LONGUEUR_MINIMUM_MOT) {
                action.accept(mot);
            }
        });
    }

    // les mots à chercher dans l'index pour une requête saisie par l'utilisateur
    public static List<String> motsDeRequete(CharSequence requete) {
        List<String> mots = new ArrayList<>();
        pourChaqueMotIndexable(requete, mots::add);
        return mots;
    }

    private final int[] idsLivres;
    private final int[] longueurs;
    private final String[] mots;
    private final int[] debut;
    private final int[] livres;
    private final int[] occurrences;

    private Index(int[] idsLivres, int[] longueurs, String[] mots, int[] debut, int[] livres, int[] occurrences) {
        this.idsLivres = idsLivres;
        this.longueurs = longueurs;
        this.mots = mots;
        this.debut = debut;
        this.livres = livres;
        this.occurrences = occurrences;
    }

    public int nbLivres() {
        return idsLivres.length;
    }

    public int idLivre(int numero) {
        return idsLivres[numero];
    }

    // nombre total de mots indexés du livre (avec les répétitions, sans les mots d'une lettre)
    public int longueurLivre(int numero) {
        return longueurs[numero];
    }

    public int nbMots() {
        return mots.length;
    }

    public String mot(int m) {
        return mots[m];
    }

    // nombre total de postings (couples mot-livre)
    public int nbPostings() {
        return livres.length;
    }

    // numéro du mot dans le vocabulaire, ou -1 s'il n'est pas dans l'index
    public int trouver(String mot) {
        int m = Arrays.binarySearch(mots, mot);
        return m >= 0 ? m : -1;
    }

    // plage des postings du mot m : de debut(m) inclus à fin(m) exclu
    public int debut(int m) {
        return debut[m];
    }

    public int fin(int m) {
        return debut[m + 1];
    }

    public int livreDuPosting(int p) {
        return livres[p];
    }

    public int occurrencesDuPosting(int p) {
        return occurrences[p];
    }

    // nombre d'occurrences du mot m dans le livre (0 s'il n'y est pas), par dichotomie sur les postings
    public int occurrences(int m, int livre) {
        int p = Arrays.binarySearch(livres, debut[m], debut[m + 1], livre);
        return p >= 0 ? occurrences[p] : 0;
    }

    // ---------- construction ----------

    // On ajoute les livres un par un (dans l'ordre), puis construire() range tout dans les tableaux.
    public static final class Constructeur {

        // postings d'un mot pendant la construction : tableaux qui grandissent
        private static final class Liste {
            int[] livres = new int[4];
            int[] occurrences = new int[4];
            int taille = 0;

            void ajouter(int livre, int nb) {
                if (taille == livres.length) {
                    livres = Arrays.copyOf(livres, taille * 2);
                    occurrences = Arrays.copyOf(occurrences, taille * 2);
                }
                livres[taille] = livre;
                occurrences[taille] = nb;
                taille++;
            }
        }

        private final Map<String, Liste> table = new HashMap<>();
        private final List<Integer> ids = new ArrayList<>();
        private final List<Integer> longueurs = new ArrayList<>();

        public void ajouterLivre(int id, CharSequence texte) {
            int numero = ids.size();

            // comptage des mots de ce livre (table temporaire, jetée ensuite)
            Map<String, int[]> compte = new HashMap<>();
            int[] total = {0};
            pourChaqueMotIndexable(texte, mot -> {
                compte.computeIfAbsent(mot, k -> new int[1])[0]++;
                total[0]++;
            });

            for (Map.Entry<String, int[]> e : compte.entrySet()) {
                table.computeIfAbsent(e.getKey(), k -> new Liste()).ajouter(numero, e.getValue()[0]);
            }
            ids.add(id);
            longueurs.add(total[0]);
        }

        public Index construire() {
            String[] mots = table.keySet().toArray(new String[0]);
            Arrays.sort(mots);

            int nbPostings = 0;
            for (Liste l : table.values()) {
                nbPostings += l.taille;
            }

            int[] debut = new int[mots.length + 1];
            int[] livres = new int[nbPostings];
            int[] occurrences = new int[nbPostings];

            int p = 0;
            for (int m = 0; m < mots.length; m++) {
                Liste l = table.get(mots[m]);
                debut[m] = p;
                System.arraycopy(l.livres, 0, livres, p, l.taille);
                System.arraycopy(l.occurrences, 0, occurrences, p, l.taille);
                p += l.taille;
            }
            debut[mots.length] = p;

            return new Index(
                ids.stream().mapToInt(Integer::intValue).toArray(),
                longueurs.stream().mapToInt(Integer::intValue).toArray(),
                mots, debut, livres, occurrences);
        }
    }

    // ---------- sauvegarde et chargement (fichier binaire) ----------

    public void sauver(Path fichier) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(fichier), 1 << 16))) {
            out.writeInt(MAGIE);
            out.writeInt(idsLivres.length);
            out.writeInt(mots.length);
            out.writeInt(livres.length);
            for (int i = 0; i < idsLivres.length; i++) {
                out.writeInt(idsLivres[i]);
                out.writeInt(longueurs[i]);
            }
            for (String mot : mots) {
                out.writeUTF(mot);
            }
            for (int d : debut) {
                out.writeInt(d);
            }
            for (int i = 0; i < livres.length; i++) {
                out.writeInt(livres[i]);
                out.writeInt(occurrences[i]);
            }
        }
    }

    public static Index charger(Path fichier) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(fichier), 1 << 16))) {
            if (in.readInt() != MAGIE) {
                throw new IOException("Ce fichier n'est pas un index : " + fichier);
            }
            int nbLivres = in.readInt();
            int nbMots = in.readInt();
            int nbPostings = in.readInt();

            int[] ids = new int[nbLivres];
            int[] longueurs = new int[nbLivres];
            for (int i = 0; i < nbLivres; i++) {
                ids[i] = in.readInt();
                longueurs[i] = in.readInt();
            }
            String[] mots = new String[nbMots];
            for (int i = 0; i < nbMots; i++) {
                mots[i] = in.readUTF();
            }
            int[] debut = new int[nbMots + 1];
            for (int i = 0; i <= nbMots; i++) {
                debut[i] = in.readInt();
            }
            int[] livres = new int[nbPostings];
            int[] occurrences = new int[nbPostings];
            for (int i = 0; i < nbPostings; i++) {
                livres[i] = in.readInt();
                occurrences[i] = in.readInt();
            }
            return new Index(ids, longueurs, mots, debut, livres, occurrences);
        }
    }
}
