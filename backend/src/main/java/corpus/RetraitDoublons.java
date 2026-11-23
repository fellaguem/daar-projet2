package corpus;

import graphe.Jaccard;
import index.Index;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/*
 * Retire les doublons qui ont des titres differents.
 * On compare les livres avec Jaccard, puis on garde le plus petit id.
 * Sans --appliquer, on affiche juste la liste.
 */
public class RetraitDoublons {

    public static final double SEUIL_DEFAUT = 0.13;

    // le livre enleve, celui qu'on garde, et leur distance
    public record Retrait(int idRetire, int idGarde, double distance) {
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        double lim = SEUIL_DEFAUT;
        boolean go = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dossier" -> dir = Path.of(args[++i]);
                case "--seuil" -> lim = Double.parseDouble(args[++i]);
                case "--appliquer" -> go = true;
                default -> {
                    System.err.println("Option inconnue : " + args[i]);
                    return;
                }
            }
        }

        Index ix = Index.charger(dir.resolve("index.bin"));
        Path fm = dir.resolve("metadonnees.tsv");
        Metadonnees md = Files.exists(fm) ? Metadonnees.charger(fm) : null;

        List<Retrait> del = chercher(ix, lim);
        System.out.printf(Locale.ROOT, "%d livres à retirer (distance de Jaccard < %.2f) :%n", del.size(), lim);

        for (Retrait r : del) {
            System.out.printf(Locale.ROOT, "  %.4f  retiré %s  |  gardé %s%n",
                    r.distance(), nom(r.idRetire(), md), nom(r.idGarde(), md));
        }

        if (!go) {
            System.out.println("Rien n'est modifié (ajouter --appliquer).");
            return;
        }

        appliquer(dir, del);
        System.out.println("Fait. Reconstruire maintenant l'index, puis le graphe.");
    }

    // les paires proches d'abord ; pas deux retraits pour un meme livre
    public static List<Retrait> chercher(Index index, double seuil) {
        int n = index.nbLivres();
        float[] dd = new Jaccard(index, Jaccard.PART_MAX_DEFAUT).toutesLesDistances();

        List<int[]> pairs = new ArrayList<>();
        int pos = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++, pos++) {
                if (dd[pos] < seuil)
                    pairs.add(new int[] { i, j, pos });
            }
        }

        pairs.sort(Comparator.comparingDouble((int[] p) -> dd[p[2]]));

        List<Retrait> del = new ArrayList<>();
        Set<Integer> vus = new HashSet<>();

        for (int[] p : pairs) {
            int keep = Math.min(index.idLivre(p[0]), index.idLivre(p[1]));
            int drop = Math.max(index.idLivre(p[0]), index.idLivre(p[1]));
            if (vus.add(drop)) {
                del.add(new Retrait(drop, keep, dd[p[2]])); // le plus grand part
            }
        }

        return del;
    }

    public static void appliquer(Path dossier, List<Retrait> retraits) throws IOException {
        if (retraits.isEmpty())
            return;

        Path dst = dossier.resolve("doublons");
        Files.createDirectories(dst);

        Set<Integer> ids = new HashSet<>();
        StringBuilder log = new StringBuilder();

        for (Retrait r : retraits) {
            ids.add(r.idRetire());
            Path src = dossier.resolve("livres").resolve(r.idRetire() + ".txt");

            if (Files.exists(src)) {
                Files.move(src, dst.resolve(r.idRetire() + ".txt"));
            }

            log.append(String.format(Locale.ROOT, "%d\tdoublon de %d (distance de Jaccard %.4f)%n",
                    r.idRetire(), r.idGarde(), r.distance()));
        }

        Path fm = dossier.resolve("metadonnees.tsv");
        if (Files.exists(fm)) {
            List<String> ok = new ArrayList<>();
            for (String ln : Files.readAllLines(fm, StandardCharsets.UTF_8)) {
                if (!ids.contains(id(ln)))
                    ok.add(ln);
            }
            Files.write(fm, ok, StandardCharsets.UTF_8);
        }

        Files.writeString(dossier.resolve("rejetes.tsv"), log, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // -1 quand la premiere colonne n'est pas un id
    private static int id(String ligne) {
        int t = ligne.indexOf('\t');
        try {
            return Integer.parseInt((t < 0 ? ligne : ligne.substring(0, t)).trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String nom(int id, Metadonnees meta) {
        Metadonnees.Livre x = meta == null ? null : meta.livre(id);
        return x == null ? "livre " + id : id + " " + x.titre();
    }
}
