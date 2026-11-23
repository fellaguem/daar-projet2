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

// Index inversé. Chaque mot pointe vers ses livres et le nombre de fois qu'il apparaît, tout est rangé dans des tableaux, les postings restent triés par livre.
public final class Index {

    private static final int MAGIE = 0x49445832; // IDX2

    public static final int LONGUEUR_MINIMUM_MOT = 2;

    // on saute les mots d'une seule lettre
    public static void pourChaqueMotIndexable(CharSequence texte, Consumer<String> action) {
        Tokeniseur.pourChaqueMot(texte, w -> {
            if (w.length() >= LONGUEUR_MINIMUM_MOT)
                action.accept(w);
        });
    }

    public static List<String> motsDeRequete(CharSequence requete) {
        List<String> lst = new ArrayList<>();
        pourChaqueMotIndexable(requete, lst::add);
        return lst;
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

    public int longueurLivre(int numero) {
        return longueurs[numero];
    }

    public int nbMots() {
        return mots.length;
    }

    public String mot(int m) {
        return mots[m];
    }

    public int nbPostings() {
        return livres.length;
    }

    public int trouver(String mot) {
        int p = Arrays.binarySearch(mots, mot);
        return p >= 0 ? p : -1;
    }

    // debut inclus, fin exclue
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

    public int occurrences(int m, int livre) {
        int pos = Arrays.binarySearch(livres, debut[m], debut[m + 1], livre);
        return pos >= 0 ? occurrences[pos] : 0;
    }

    // construction de l'index
    public static final class Constructeur {

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
            int num = ids.size();

            Map<String, int[]> freq = new HashMap<>(); // compte pour ce livre
            int[] tot = { 0 };
            pourChaqueMotIndexable(texte, w -> {
                freq.computeIfAbsent(w, k -> new int[1])[0]++;
                tot[0]++;
            });

            for (Map.Entry<String, int[]> e : freq.entrySet()) {
                table.computeIfAbsent(e.getKey(), k -> new Liste()).ajouter(num, e.getValue()[0]);
            }
            ids.add(id);
            longueurs.add(tot[0]);
        }

        public Index construire() {
            String[] voc = table.keySet().toArray(new String[0]);
            Arrays.sort(voc);

            int npos = 0;
            for (Liste l : table.values())
                npos += l.taille;

            int[] deb = new int[voc.length + 1];
            int[] liv = new int[npos];
            int[] occ = new int[npos];

            int p = 0;
            for (int m = 0; m < voc.length; m++) {
                Liste l = table.get(voc[m]);
                deb[m] = p;
                System.arraycopy(l.livres, 0, liv, p, l.taille);
                System.arraycopy(l.occurrences, 0, occ, p, l.taille);
                p += l.taille;
            }
            deb[voc.length] = p;

            return new Index(
                    ids.stream().mapToInt(Integer::intValue).toArray(),
                    longueurs.stream().mapToInt(Integer::intValue).toArray(),
                    voc, deb, liv, occ);
        }
    }

    // enregistrement binaire, puis rechargement
    public void sauver(Path fichier) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(fichier), 1 << 16))) {
            out.writeInt(MAGIE);
            out.writeInt(idsLivres.length);
            out.writeInt(mots.length);
            out.writeInt(livres.length);

            for (int j = 0; j < idsLivres.length; j++) {
                out.writeInt(idsLivres[j]);
                out.writeInt(longueurs[j]);
            }
            for (String w : mots)
                out.writeUTF(w);

            for (int d : debut) {
                out.writeInt(d);
            }

            for (int k = 0; k < livres.length; k++) {
                out.writeInt(livres[k]);
                out.writeInt(occurrences[k]);
            }
        }
    }

    public static Index charger(Path fichier) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(fichier), 1 << 16))) {

            if (in.readInt() != MAGIE) {
                throw new IOException("Ce fichier n'est pas un index : " + fichier);
            }

            int nl = in.readInt();
            int nm = in.readInt();
            int np = in.readInt();

            int[] ids = new int[nl];
            int[] lens = new int[nl];
            for (int j = 0; j < nl; j++) {
                ids[j] = in.readInt();
                lens[j] = in.readInt();
            }

            String[] voc = new String[nm];
            for (int j = 0; j < nm; j++)
                voc[j] = in.readUTF();

            int[] deb = new int[nm + 1];
            for (int j = 0; j <= nm; j++)
                deb[j] = in.readInt();

            int[] liv = new int[np];
            int[] occ = new int[np];
            for (int j = 0; j < np; j++) {
                liv[j] = in.readInt();
                occ[j] = in.readInt();
            }

            return new Index(ids, lens, voc, deb, liv, occ);
        }
    }
}
