# Notes pour le rapport

Choix et chiffres à reprendre dans le rapport. On complète au fil de l'eau.

## Choix de conception

### Langue du corpus : anglais seulement (décidé le 12/11/2026)

Tous les livres de la bibliothèque sont en anglais.

Raison : le graphe de Jaccard compare les ensembles de mots de deux livres.
Deux livres de langues différentes n'ont presque aucun mot en commun, donc leur
distance de Jaccard est proche de 1. Avec un corpus multilingue, le graphe se
découperait en îlots, un par langue : les voisins et le PageRank refléteraient
la langue du livre et non son contenu. Les suggestions seraient alors
trompeuses.

Mise en œuvre : le catalogue officiel de Project Gutenberg indique la langue de
chaque livre. Le script de téléchargement ne garde que les livres dont la
langue est `en`. On le dit dans le rapport avec la provenance des fichiers.

### Recherche par mot-clef : recherche exacte dans l'index

L'énoncé demande les livres dont la table d'indexage contient la chaîne S. C'est
une recherche dans l'index (mot exact), pas un balayage de texte : KMP n'est
pas utilisé ici. KMP reste utile pour l'extrait d'un livre et pour la variante
« texte complet » de la recherche avancée.

### Classement : occurrences normalisées × PageRank (point de départ)

- Occurrences normalisées : nombre d'occurrences du mot divisé par la longueur
  du livre. Sinon les gros livres gagnent toujours.
- PageRank : importance du livre dans le graphe de Jaccard, calculée une fois
  hors ligne, indépendante de la requête.
- Le produit des deux donne le score. À comparer dans le rapport avec
  « occurrences seules » et « PageRank seul ».

### Suggestions

Voisins dans le graphe de Jaccard des 2-3 meilleurs résultats, sans les
doublons avec la liste des résultats. Bonus possible plus tard : classer ces
voisins par nombre de clics (stratégie citée dans l'énoncé).

## Questions encore ouvertes

- Mots vides : comment les traiter (piste : les garder dans l'index mais les
  ignorer dans le classement).
- Regex : mot entier ou sous-chaîne ? Traducteur pour `+ ? [ ]` ?
- Build du front dans l'archive de 30 Mo : commité ou reconstruit ?

## Chiffres et mesures

(rien pour l'instant)

## Environnement de développement

- JDK 17 : Temurin 17.0.21 (installé avec winget).
- Maven : 3.9.9, fourni par le Maven Wrapper (`backend/mvnw`), rien à installer.
- Node : v26, npm 11.
- Le projet 1 (packages parser, ast, automate, kmp, experimentation) est copié
  tel quel dans `backend/src/main/java` ; il compile avec Java 17.
- Le build du front va dans `backend/src/main/resources/static`, ignoré par git
  (régénéré par `npm run build`). À décider : l'inclure ou non dans l'archive.

## Tests du projet 1 (référence avant toute modification)

Les tests du projet 1 sont repris en JUnit dans `backend/src/test/java/projet1`
(textes et regex dans `backend/src/test/resources/projet1`, 5,2 Mo). Lancés sur le
code du projet 1 non modifié (`mvnw test`, 13/11/2026) :

| Test | Comparaisons | Échecs | Durée |
|---|---|---|---|
| CorrectionProjet1Test (naïf, une passe, KMP contre java.util.regex) | 8 007 480 | 0 | 17 s |
| AutomatesProjet1Test (naïf contre une passe) | 3 490 440 | 0 | 13 s |

Les mêmes chiffres que dans le README du projet 1. On relance ces tests après
chaque modification de `parser`, `automate`, `kmp` ou `experimentation`.
Non repris : TestPerformance et TestPireCas (des mesures, pas des tests).

## Corpus : script de téléchargement (`corpus.TelechargeCorpus`)

Provenance : Project Gutenberg. Catalogue officiel
`https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv` (61 977 livres de type
Text en anglais le 13/11/2026), textes téléchargés depuis le miroir
`gutenberg.pglaf.org` (site officiel en secours), chemin `/cache/epub/ID/pgID.txt`.

Méthode : on parcourt le catalogue dans l'ordre des identifiants, langue exactement `en`,
pause entre deux téléchargements, retrait de l'en-tête et de la licence (marqueurs
`*** START OF ... ***` et `*** END OF ... ***`), on garde le livre s'il a au moins 10 000 mots
(mots comptés avec `index.Tokeniseur`). Le script est reprenable : relancé, il saute les
livres déjà traités. Il écrit `metadonnees.tsv` (id, titre, auteur, langue, URL, nombre de mots)
et `rejetes.tsv` (id, raison).

Lancer : `.\mvnw.cmd compile` dans backend, puis, depuis la racine,
`java -cp backend/target/classes corpus.TelechargeCorpus --max 1750 --pause 1500`.

Tokeniseur : un mot = une suite de lettres, en minuscules. Les apostrophes, tirets et
chiffres séparent les mots (« don't » donne « don » et « t »).

Test sur 20 livres (`data/test20`) : 30 téléchargements pour 20 livres gardés, 10 écartés
car trop courts (300 à 5 294 mots). 22 Mo pour 20 livres, soit environ 1,1 Mo par livre
(la Bible fait 4,5 Mo) : prévoir environ 1,5 à 2 Go pour 1 750 livres.

Problèmes vus sur ce test, à traiter avant le téléchargement complet :
- doublons : la Bible (10 et 30), Paradise Lost (20 et 26), le CIA World Factbook (14 et 25).
  Deux livres identiques ont une distance de Jaccard de 0 : ils faussent le graphe.
- livres qui n'en sont pas : CIA World Factbook, recensement américain, Roget's Thesaurus.

### Filtres du corpus (`corpus.Filtres`)

Appliqués avant le téléchargement et avant le comptage de `--max` : un livre écarté ne compte
pas parmi les livres gardés.

- Filtre de contenu : titres contenant Factbook, Census, Thesaurus, Dictionary ou Gazetteer
  (listes de mots ou de chiffres). Les encyclopédies et la Bible sont gardées.
- Filtre de doublons : deux livres sont des doublons s'ils ont la même clé. Clé du titre :
  minuscules, ponctuation retirée, mots neutres retirés (the, a, an, of, and, complete,
  illustrated, annotated, unabridged, edition), ordre des mots ignoré, chiffres gardés
  (Volume 1 et Volume 2 restent distincts). Clé de l'auteur : mêmes règles sans les chiffres
  (dates). On garde le plus petit identifiant ; un livre écarté (trop court, absent) ne bloque
  pas son doublon. Deux livres de même titre mais d'auteurs différents sont gardés tous les deux.
- Limite connue : deux éditions dont les titres diffèrent par un mot autre que ceux de la liste
  ne sont pas repérées ici. Le graphe de Jaccard les retrouvera (distance proche de 0).

## Index inversé (`index.Index`, `index.ConstructionIndex`)

Structure : cinq tableaux d'entiers, chargés en mémoire au démarrage.
`mots[]` (vocabulaire trié), `debut[]` (plage des postings du mot m : `debut[m]` à `debut[m+1]`),
`livres[]` et `occurrences[]` (un couple par posting, postings triés par livre), plus `idsLivres[]`
et `longueurs[]` par livre. Mot exact : dichotomie sur `mots[]`. Occurrences d'un mot dans un livre :
dichotomie sur les postings du mot. Fichier binaire unique (`data/index.bin`, format « IDX2 »).

Choix :
- Les mots vides restent dans l'index (chercher « the » doit marcher). Ils seront retirés du graphe
  de Jaccard et du classement, avec un seuil automatique (mots présents dans plus de X % des livres,
  X à fixer) plutôt qu'une liste écrite à la main. Dans Jaccard on retirera aussi les mots présents
  dans un seul livre (ils ne changent pas l'intersection et allègent les vecteurs de bits).
- Les mots d'une lettre ne sont pas indexés (« whale's » donne « whale », les « s » et « t » isolés
  disparaissent). C'est fait dans l'index et pour les requêtes, pas dans le tokeniseur : le seuil de
  10 000 mots du corpus compte tous les mots. `longueurLivre` compte les mots indexés.
- Limite à noter : « don't » est lu « don ».
- Recherche à plusieurs mots : ET (livres contenant tous les mots), score = somme des occurrences
  (`recherche.RechercheIndex`). On parcourt les postings du mot le plus rare et on vérifie les autres
  par dichotomie. La recherche par regex sera un OU : un livre est retenu s'il contient au moins un
  mot du vocabulaire qui vérifie la regex.

Mesure sur les 20 livres de test (`data/test20`) : 71 959 mots différents, 191 597 postings,
construction + sauvegarde 0,8 s, fichier de 2 Mo. À refaire sur le vrai corpus (estimation :
environ 20 millions de postings, 150 à 200 Mo ; à mesurer).

Tests : `IndexTest` (11), `RechercheIndexTest` (7), `TokeniseurTest` (6).

## Recherche avancée par regex, mode index (`recherche.RechercheRegex`)

Principe : regex minuscule -> `Parser` -> `MatcherUnePasse.envelopper` (`.*R`, donc recherche d'une
sous-chaîne comme egrep) -> AFD minimal (projet 1) -> test sur chaque mot du vocabulaire -> union des
livres de ces mots (OU), score = somme des occurrences des mots trouvés. Réponse : livres triés,
nombre de mots trouvés, 50 premiers mots (ordre alphabétique).

Refusées avec un message (RegexInvalide, code 400 côté API) : regex vide, plus de 100 caractères,
mal écrite, qui accepte le mot vide (`a*`, `a|`, `(a|b)*` : elle trouverait tous les livres), ou dont
l'AFD dépasse 2 000 états.

### Explosion du nombre d'états (mesures du 13/11/2026)

Regex `a` suivie de k points, automate de `.*a.{k}` : l'AFD a 2^(k+1) états.

| k | états (AFD) | construction |
|---|---|---|
| 9 | 1 025 | 47 ms |
| 10 | 2 049 | 124 ms |
| 11 | 4 097 | 379 ms |
| 12 | 8 193 | 1,4 s |
| 13 | 16 385 | 5,8 s |
| 14 | 32 769 | 27 s |

Le nombre d'états double à chaque point, et le temps est ensuite multiplié par 4 à chaque doublement
(la minimisation de Moore du projet 1 est quadratique : `indicePartition` parcourt toutes les
partitions). Une regex de 100 caractères comme `a` + 99 points ne finirait jamais (2^100 états) :
d'où la limite de 2 000 états (environ 0,1 s dans le pire cas), appliquée par un paramètre
optionnel de `Determinisation` et `ConstructionAutomate` (pas de limite par défaut, le projet 1
se comporte comme avant). À discuter dans le rapport : le blow-up exponentiel de la
déterminisation est le point faible connu de l'approche AFD ; un NFA simulé évite l'explosion
mais coûte plus par caractère.

### Temps mesurés sur les 20 livres de test

`recherche.MesureRegex` (3 appels de chauffe, 10 essais, moyenne ± écart type), fichier
`data/test20/mesures/regex_index.csv`. Index : 20 livres, 71 959 mots, 191 597 postings.

| regex | temps (ms) | mots trouvés | livres |
|---|---|---|---|
| `whale` | 22,0 ± 4,9 | 13 | 10 |
| `wh(a\|e)le` | 17,4 ± 1,0 | 13 | 10 |
| `s(a\|g\|r)*on` | 19,7 ± 2,7 | 268 | 20 |
| `a.*b` | 14,3 ± 1,2 | 3 034 | 20 |
| `.*ing` | 18,5 ± 3,0 | 4 360 | 20 |
| `(sh\|wh)(a\|o)(l\|r)(e\|k)` | 20,9 ± 1,8 | 56 | 19 |
| `q.*z` | 16,3 ± 4,0 | 36 | 12 |

Le temps est dominé par le parcours du vocabulaire (environ 0,25 µs par mot) et ne dépend presque pas
de la regex. À refaire sur le vrai corpus (vocabulaire plus grand).
Pour comparaison, le texte complet des 20 livres (21,4 millions de caractères) prend 824 ms pour
`wh(a|e)le` avec le projet 1 tel quel, soit 39 ns par caractère.

## Projet 1 étendu (modifications du code copié dans backend/)

1. `Determinisation` et `ConstructionAutomate` : constructeur avec limite `etatsMax` sur le nombre
   d'états de l'AFD (nouvelle classe `AutomateTropGrandException`). Les anciens constructeurs n'ont
   pas de limite : comportement inchangé.
2. `Parser` : syntaxe étendue.
   - `x+` devient `x x*` et `x?` devient `x | vide` (aucun nouveau noeud d'arbre ni nouvel automate).
   - Classes de caractères `[abc]`, `[a-z]`, `[a-cx-z]`, `[é-ü]` : alternative `a|b|c`. Le `-` au début
     ou à la fin est une lettre. Refusés : `[]`, `[^...]` (non géré), plage à l'envers, classe de plus
     de 1 000 lettres, crochet non fermé.
   - Changement de comportement : `+`, `?` et `[` étaient des lettres, ce sont maintenant des opérateurs.
     `]` seul reste une lettre. `{ }` restent des lettres.
   - Piège pour les tests : en Java, `a++`, `a*+` et `a?+` sont des quantificateurs possessifs, qui
     n'ont pas le même sens ici (`(a+)+`). `input7.txt` évite ces enchaînements.

Tests : `ParserTest` (11), `input7.txt` (24 regex) lu par les tests du projet 1 :
`CorrectionProjet1Test` passe à 13 037 820 comparaisons (naïf, une passe et KMP contre java.util.regex),
`AutomatesProjet1Test` à 5 954 280, toujours 0 erreur. `RechercheRegexTest` (10) compare, pour 48 regex,
les mots trouvés et les totaux par livre à `java.util.regex` sur un index des 14 textes de test.
Suite complète : 61 tests.

## Graphe de Jaccard et PageRank (`graphe.Jaccard`, `graphe.Graphe`, `graphe.PageRank`, `graphe.ConstructionGraphe`)

Distance : d(A, B) = 1 - |A ∩ B| / |A ∪ B| sur les ensembles de mots des livres, calculée depuis l'index.

Mots pris en compte (décidé le 13/11/2026) :
- mots vides retirés : mots présents dans plus de 50 % des livres (seuil automatique, pas de liste) ;
- mots présents dans un seul livre gardés. Ils ne peuvent pas être dans une intersection : ils n'ont
  pas de bit, ils sont seulement comptés dans la taille |A|. La distance est exactement celle des
  ensembles complets (vérifié par `JaccardTest` contre un calcul avec des `HashSet`, sur les 14 textes
  du projet 1 et sur 20 petits corpus aléatoires), avec des vecteurs de bits bien plus petits.
  Effet à discuter : un livre plein de mots rares est plus loin de tous les autres.
- deux livres sans aucun mot gardé ont une distance de 1 (pas d'information).

Calcul : un vecteur de bits (long[]) par livre sur les mots partagés ; |A ∩ B| = bitCount(A ET B),
|A ∪ B| = |A| + |B| - |A ∩ B|. Les n(n-1)/2 distances sont calculées en parallèle (une ligne par tâche)
et gardées en float (7 Mo pour 1 900 livres) pour l'histogramme et le choix du seuil.

Graphe : arête si d < seuil (strictement). Seuil donné (`--seuil`) ou déduit d'un degré moyen visé
(`--degre-moyen`, 10 par défaut) : on garde les degré * n / 2 plus petites distances. Rangé en tableaux
(debut[], voisins[], distances[]), voisins triés du plus proche au plus loin (utile pour les suggestions).
Fichiers : graphe.bin, aretes.csv (liste d'arêtes), pagerank.bin, pagerank.csv, mesures/distances_histogramme.csv.

PageRank : classique (non pondéré), itération de la puissance, amortissement 0,85, arrêt quand la somme
des |PR' - PR| < 1e-10 (100 itérations au plus). Le score des livres isolés est redistribué
uniformément à tous les livres (la somme reste 1). Vérifié par `PageRankTest` : cycle uniforme, centre
d'étoile, formule exacte avec un isolé (y = (1-a)/(3-a)), et calcul matriciel naïf sur 30 graphes
aléatoires (écart < 1e-10). Version pondérée par la similarité : bonus, si on a le temps.

Point ouvert (à décider après l'histogramme du vrai corpus) : livres isolés acceptés tels quels, ou
garantir à chaque livre ses 3 plus proches voisins.

### Essai sur les 20 livres de test (pas une calibration)

20 livres, 190 paires. Mots : 35 885 partagés, 33 285 présents dans un seul livre, 2 789 mots vides.
Vecteurs de bits 0,1 Mo (8 ms), distances 14 ms.
Distances : min 0,0018, 5 % 0,83, médiane 0,94, 95 % 0,998, moyenne 0,92 : presque toutes entre 0,8
et 1 (histogramme : 177 paires sur 190 au-dessus de 0,85).
Paires les plus proches : les doublons que le filtre par titre n'avait pas encore (ce test date d'avant
le filtre) : les deux Bibles (0,0018), les deux CIA World Factbook (0,17), les deux Paradise Lost (0,47).
Puis Alice et De l'autre côté du miroir (0,82) : Jaccard retrouve bien les livres proches.
Degré moyen visé 10 -> seuil 0,9417, 100 arêtes, 1 isolé (le recensement de 1990, liste de chiffres),
2 composantes. PageRank : 22 itérations, 2 ms. Corrélation PageRank / degré : 0,998 (attendu sur un
graphe non orienté : PageRank y est presque proportionnel au degré ; à discuter dans le rapport).

Tests : JaccardTest (8), GrapheTest (7), PageRankTest (8), MetadonneesTest (1). Suite complète : 85 tests.

### Téléchargement du corpus (lancé le 13/11/2026 à 01:08)

Environ 5 livres gardés par minute (106 requêtes en 16,6 min, soit environ 9 s par livre avec la pause
de 1,5 s) : le corpus complet demandera environ 6 h, et non 1 h 30 à 2 h comme estimé.

## Moteur et API réelle (`recherche.Moteur`, `api.*`)

`Moteur` charge au démarrage, depuis un dossier (`moteur.dossier`, `data` par défaut) : index.bin, graphe.bin,
pagerank.bin, metadonnees.tsv et livres/. Il vérifie que l'index, le graphe et le PageRank contiennent les mêmes
livres dans le même ordre (sinon le serveur refuse de démarrer). Ensuite tout est en lecture seule : plusieurs
clients en même temps sans verrou (testé : 200 requêtes sur 16 fils, mêmes réponses qu'en séquentiel).

Classement (décidé le 13/11/2026) :
- score = (occurrences des mots non vides / nombre de mots du livre) × PageRank ;
- mots vides = mots présents dans plus de 50 % des livres (même seuil que Jaccard) : gardés dans la recherche
  (ET pour un mot-clef), ignorés dans le score ; si tous les mots de la requête sont vides, ils comptent tous ;
- pour une regex, mêmes règles sur les mots trouvés ;
- paramètre `tri` pour comparer les classements dans le rapport : `produit` (défaut), `occurrences`
  (occurrences totales, le critère simple de l'énoncé), `pagerank`. Le champ `score` est toujours renvoyé.

Suggestions : voisins de Jaccard des 3 premiers résultats (selon le tri), sans ces 3 livres, triés par distance
au plus proche des 3, 10 au plus. Choix : on n'exclut que les 3 premiers et pas tous les résultats, sinon un mot
courant (présent dans presque tous les livres) ne laisserait aucune suggestion.

Tous les résultats sont renvoyés (le front pagine). `tempsMs` = temps du moteur seul (recherche, classement,
suggestions), sans réseau ni JSON, en ms avec 2 décimales.

Erreurs : 400 `{ "erreur": "..." }` (requête vide, regex invalide, tri ou mode inconnu, mode texte pas encore
disponible), 404 (livre inconnu). `DonneesFactices.java` supprimé ; le mode données factices reste dans le front.

Essai sur data/test20 (serveur réel) : « white whale » -> 5 livres en 3,95 ms, Moby-Dick en tête
(1 526 occurrences, score 4,2e-4, loin devant). Les trois tris donnent des ordres différents :
produit 15, 22, 17, 10 ; occurrences 15, 10, 30, 22 ; pagerank 15, 10, 17, 30.
Regex `wh(a|e)les?` : 13 mots (dont narwhale), 10 livres, 67 ms au premier appel (JVM pas encore chauffée).
Page d'Alice (11) : premier voisin Through the Looking-Glass (12).

Tests : MoteurTest (13, sur un corpus de 6 livres construit comme le vrai), ApiTest (6, serveur réel sur un port
libre : contrat JSON, tris, suggestions, 400/404, 200 requêtes simultanées). Suite complète : 104 tests.
