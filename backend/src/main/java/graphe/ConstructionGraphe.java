package graphe;

import corpus.Metadonnees;
import index.Index;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.PriorityQueue;

// Construit le graphe et calcule PageRank, avant de lancer le serveur.
// Exemple : java -cp backend/target/classes graphe.ConstructionGraphe --dossier data
// On peut changer le seuil, le nombre de mots par livre et l'amortissement.
// Résultats : graphe.bin, pagerank.bin et quelques CSV pour les mesures.

public class ConstructionGraphe {

       // 250 mots par livre, réglage gardé après les essais
     public static final int MOTS_PAR_LIVRE_DEFAUT = 250;

    public static void main(String[] args) throws IOException {
            Path base = Path.of("data");
       Path fIdx=null;
        Path fMd = null;
          double part = Jaccard.PART_MAX_DEFAUT;
       int topK = MOTS_PAR_LIVRE_DEFAUT;
           double degM = 10;
         Double limite=null;
      double amort = PageRank.AMORTISSEMENT_DEFAUT;

            for (int i = 0; i < args.length; i++) {
           switch (args[i]) {
                case "--dossier" -> base=Path.of(args[++i]);
                  case "--index" -> fIdx = Path.of(args[++i]);
               case "--metadonnees" -> fMd = Path.of(args[++i]);
                   case "--part-max" -> part = Double.parseDouble(args[++i]);
                 case "--mots-par-livre" -> topK = Integer.parseInt(args[++i]);
              case "--degre-moyen" -> degM=Double.parseDouble(args[++i]);
                case "--seuil" -> limite = Double.parseDouble(args[++i]);
                    case "--amortissement" -> amort = Double.parseDouble(args[++i]);
               default -> {
                    System.err.println("Option inconnue : " + args[i]);
                      return;
               }
               }
         }
      if (fIdx == null) {
            fIdx=base.resolve("index.bin");
            }
       if (fMd == null) {
            fMd = base.resolve("metadonnees.tsv");
          }
       Metadonnees meta=Files.exists(fMd) ? Metadonnees.charger(fMd) : null;

         // d’abord les distances
      Index index = Index.charger(fIdx);
        int n = index.nbLivres();
            int[] ids=new int[n];
       for (int i = 0; i < n; i++) {
            ids[i] = index.idLivre(i);
          }

           long t0=System.nanoTime();
         Jaccard jac = new Jaccard(index, part, topK);
      long t1 = System.nanoTime();
        float[] dist = jac.toutesLesDistances();
            long t2 = System.nanoTime();

        System.out.printf(Locale.ROOT,"Livres : %d, paires : %d%n",n,dist.length);
          int mini = Integer.MAX_VALUE;
       int maxi = 0;
           int manq = 0;
         for (int i=0; i < n; i++) {
          mini = Math.min(mini, jac.taille(i));
            maxi = Math.max(maxi,jac.taille(i));
                if (topK > 0 && jac.taille(i) < topK) {
               manq++;
            }
          }
       System.out.printf(Locale.ROOT, "Ensembles : %s, tailles de %d à %d mots%s%n",
               topK > 0 ? "les " + topK + " mots les plus fréquents de chaque livre" : "tous les mots",
             n == 0 ? 0 : mini, maxi,
          manq > 0 ? " ("+manq+" livres ont moins de "+topK+" mots)" : "");
        System.out.printf(Locale.ROOT,"Mots : %d partagés (un bit chacun), %d dans l'ensemble d'un seul livre, "
                + "%d mots vides retirés (dans plus de %.0f %% des livres)%n",
           jac.nbMotsPartages(), jac.nbMotsUniques(), jac.nbMotsVides(), part * 100);
        System.out.printf(Locale.ROOT,"Vecteurs de bits : %.1f Mo, construits en %d ms%n",
              jac.tailleBits() / 1e6, (t1 - t0) / 1_000_000);
       System.out.printf(Locale.ROOT, "Distances : %d ms (%d coeurs)%n",
               (t2 - t1) / 1_000_000,Runtime.getRuntime().availableProcessors());

      Files.createDirectories(base.resolve("mesures"));
        afficherDistribution(dist);
            ecrireHistogramme(dist, base.resolve("mesures/distances_histogramme.csv"));
       afficherPairesProches(dist, n, ids, meta, 10);

          // on forme le graphe ici
       double seuil=limite != null ? limite : Graphe.seuilPourDegreMoyen(dist, n, degM);
           long t3 = System.nanoTime();
         Graphe graphe = Graphe.depuisDistances(ids, dist, seuil);
      long t4 = System.nanoTime();
        int[] comp = graphe.composantes();
            System.out.printf(Locale.ROOT, "%nGraphe : seuil %.4f%s, %d arêtes, degré moyen %.2f, %d livres isolés, "
           + "%d composantes (la plus grande : %d livres), construit en %d ms%n",
            seuil,limite != null ? " (fixé)" : " (pour un degré moyen de " + degM + ")",
              graphe.nbAretes(), n == 0 ? 0 : 2.0 * graphe.nbAretes() / n, graphe.nbIsoles(),
           comp.length, comp.length == 0 ? 0 : comp[0], (t4 - t3)/1_000_000);

 
        // puis PageRank
      long t5 = System.nanoTime();
        PageRank.Resultat pr = PageRank.calculer(graphe,amort,
                PageRank.TOLERANCE_DEFAUT, PageRank.ITERATIONS_MAX_DEFAUT);
       long t6=System.nanoTime();
        System.out.printf(Locale.ROOT,"PageRank : %d itérations, écart final %.2e, %d ms%n",
              pr.iterations(), pr.ecart(), (t6 - t5) / 1_000_000);
       afficherMeilleurs(graphe, pr.scores(), meta, 10);

         // petite vérif : degré et longueur peuvent jouer sur les scores
      double[] dd = new double[n];
        double[] lg = new double[n];
            for (int i = 0; i < n; i++) {
           dd[i] = graphe.degre(i);
            lg[i]=index.longueurLivre(i);
          }
       System.out.printf(Locale.ROOT, "Corrélation (Pearson) entre PageRank et degré : %.4f%n",
               pearson(pr.scores(),dd));
         System.out.printf(Locale.ROOT, "Corrélation (Pearson) entre PageRank et longueur du livre : %.4f%n",
          pearson(pr.scores(), lg));
        afficherIsolesParLongueur(graphe,lg);

       // les fichiers qu’on garde
        graphe.sauver(base.resolve("graphe.bin"));
          graphe.ecrireAretesCsv(base.resolve("aretes.csv"));
       PageRank.sauver(base.resolve("pagerank.bin"), ids, pr.scores()); // pour le serveur
           ecrirePageRankCsv(graphe,pr.scores(),meta,base.resolve("pagerank.csv"));
         System.out.println("Fichiers écrits dans " + base);
  }

        // les distances en quelques chiffres
   private static void afficherDistribution(float[] dist) {
        if(dist.length == 0) {
              return;
       }
           float[] tri = dist.clone();
         Arrays.sort(tri); // pour les quantiles
      double tot = 0;
        for(float d : tri) {
                tot += d;
       }
        System.out.printf(Locale.ROOT,"Distances : min %.4f | 1 %% %.4f | 5 %% %.4f | 25 %% %.4f | médiane %.4f "
              + "| 75 %% %.4f | 95 %% %.4f | max %.4f | moyenne %.4f%n",
           tri[0], quantile(tri, 0.01), quantile(tri, 0.05), quantile(tri, 0.25), quantile(tri, 0.5),
               quantile(tri,0.75),quantile(tri,0.95),tri[tri.length - 1],tot / tri.length);
     }

    private static float quantile(float[] tri,double q) {
            return tri[(int) Math.min(tri.length - 1, Math.floor(q*tri.length))];
   }

      // 20 tranches de 0,05 (la distance 1 reste dans la dernière)
   private static void ecrireHistogramme(float[] dist, Path f) throws IOException {
           long[] tranches = new long[20];
         for (float d : dist) {
          tranches[Math.min(19, (int) (d * 20))]++;
        }
            try (PrintStream ps = new PrintStream(Files.newOutputStream(f), false, StandardCharsets.UTF_8)) {
           ps.println("debut,fin,nb_paires");
            for (int c=0; c < 20; c++) {
                  ps.printf(Locale.ROOT, "%.2f,%.2f,%d%n", c * 0.05, (c + 1) * 0.05, tranches[c]);
           }
           }
     }

    // les paires les plus proches, au cas où il reste des doublons
        private static void afficherPairesProches(float[] dist, int n, int[] ids, Metadonnees meta, int nbTop) {
       // on garde les plus petites distances dans un tas
        PriorityQueue<long[]> tas = new PriorityQueue<>(
              Comparator.comparingDouble((long[] x) -> dist[(int) x[2]]).reversed());
       int k = 0;  // position dans dist
           for (int i = 0; i < n; i++) {
             for (int j = i + 1; j < n; j++, k++) {
              if (tas.size() < nbTop) {
                    tas.add(new long[] {i,j,k});
                    } else if (dist[k] < dist[(int) tas.peek()[2]]) {
                   tas.poll();
                    tas.add(new long[] {i,j,k});
                  }
           }
           }
         long[][] duos = tas.toArray(new long[0][]);
      Arrays.sort(duos, Comparator.comparingDouble((long[] x) -> dist[(int) x[2]]));
        System.out.println("Paires les plus proches :");
            for (long[] p : duos) {
           System.out.printf(Locale.ROOT, "  %.4f  %s  |  %s%n", dist[(int) p[2]],
                nom(ids[(int) p[0]],meta),nom(ids[(int) p[1]],meta));
          }
   }

     private static void afficherMeilleurs(Graphe g, double[] sc, Metadonnees meta, int nbTop) {
      Integer[] rang = new Integer[g.nbSommets()];
        for(int i = 0; i < rang.length; i++) {
                rang[i] = i;
       }
        Arrays.sort(rang,(a,b) -> Double.compare(sc[b],sc[a]));
          System.out.println("PageRank le plus élevé :");
       for (int r = 0; r < Math.min(nbTop, rang.length); r++) {
               int i = rang[r];
             System.out.printf(Locale.ROOT, "  %.6f  degré %3d  %s%n", sc[i], g.degre(i), nom(g.idLivre(i), meta));
      }
    }

   private static void ecrirePageRankCsv(Graphe g, double[] sc, Metadonnees meta, Path f)
            throws IOException {
          try (PrintStream ps = new PrintStream(Files.newOutputStream(f), false, StandardCharsets.UTF_8)) {
           ps.println("id,degre,pagerank,titre");
               for (int i = 0; i < g.nbSommets(); i++) {
                 Metadonnees.Livre l = meta == null ? null : meta.livre(g.idLivre(i));
              String titre=l == null ? "" : l.titre().replace("\"", "\"\"");
                ps.printf(Locale.ROOT,"%d,%d,%.10f,\"%s\"%n",g.idLivre(i),g.degre(i),sc[i],titre);
                }
       }
    }

   // livres isolés selon leur longueur
       private static void afficherIsolesParLongueur(Graphe g,double[] lg) {
         int[] plages = {0, 25_000, 50_000, 100_000, 200_000, Integer.MAX_VALUE};
      System.out.println("Par longueur (mots indexés) :");
        for (int t=0; t + 1 < plages.length; t++) {
                int nb = 0;
           int iso = 0;
            long dd = 0;
              for (int i = 0; i<g.nbSommets(); i++) {
               if (lg[i] >= plages[t] && lg[i] < plages[t + 1]) {
                       nb++;
                     dd += g.degre(i);
                  if (g.degre(i) == 0) {
                        iso++;
                        }
               }
            }
              if (nb > 0) {
               System.out.printf(Locale.ROOT, "  %7d à %-10s : %4d livres, %5.1f %% isolés, degré moyen %.1f%n",
                       plages[t],plages[t+1] == Integer.MAX_VALUE ? "plus" : String.valueOf(plages[t+1]),
                     nb, 100.0 * iso / nb, (double) dd / nb);
          }
        }
        }

    private static String nom(int id,Metadonnees meta) {
          Metadonnees.Livre l = meta == null ? null : meta.livre(id);
       if (l == null) {
               return "livre " + id;
         }
      String titre = l.titre().length() > 50 ? l.titre().substring(0, 50) + "…" : l.titre();
        return id+" "+titre;
        }

    static double pearson(double[] x,double[] y) {
          int n = x.length;
       if (n<2) {
               return Double.NaN;
         }
      double mx=0;
        double my = 0;
            for (int i = 0; i < n; i++) {
           mx += x[i];
            my += y[i];
          }
       mx /= n;
           my /= n;
         double sxy = 0;
      double sxx = 0;
        double syy=0;
            for (int i = 0; i<n; i++) {
           sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
              syy += (y[i] - my)*(y[i] - my);
       }
           return sxy / Math.sqrt(sxx * syy);
     }
}
