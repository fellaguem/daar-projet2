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
construction + sauvegarde 0,8 s, fichier de 2 Mo.

Mesure sur le vrai corpus (13/11/2026, 1 900 livres, 794 Mo de texte, `-Xmx2g`, une seule exécution) :

| | |
|---|---|
| Mots dans les livres (tokeniseur) | 145 024 292 (de 10 016 à 1 315 683 par livre, moyenne 76 329) |
| Mots différents | 416 275 |
| Postings (couples mot-livre) | 12 440 683 |
| Construction | 21,5 s (lecture des fichiers comprise) |
| Construction + sauvegarde | 21,9 s |
| Fichier `data/index.bin` | 100 Mo |
| Mémoire maximale du processus | 874 Mo (working set Windows) |

L'estimation (environ 20 millions de postings, 150 à 200 Mo) était trop haute. Vérification à la main :
« whale » est dans 285 livres ; en tête, Moby-Dick (1 244 occurrences), puis « The Cruise of the
Cachalot » (342), un récit de chasse au cachalot.

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

### Premier calcul sur le vrai corpus (13/11/2026, 1 900 livres, réglages par défaut)

1 804 050 paires. Mots : 185 698 partagés (un bit chacun), 227 559 dans un seul livre, 3 018 mots vides
retirés (plus de 50 % des livres). Vecteurs de bits 44,1 Mo (95 ms), distances 1,7 s sur 12 cœurs,
programme complet 4,0 s, mémoire maximale 371 Mo.
Distances : min 0,0048, 1 % 0,784, 5 % 0,824, 25 % 0,876, médiane 0,911, 75 % 0,941, 95 % 0,966,
max 0,998 (`data/mesures/distances_histogramme.csv`).

Doublons que le filtre par titre a laissé passer (titres différents), retrouvés par Jaccard :
Alice 11 / 928 « HTML Edition » (0,0048), A Personal Record / Some Reminiscences (0,043, le même livre de
Conrad sous deux titres), Captains Courageous ×2 (0,060), Around the World in Eighty Days / « Junior
Deluxe Edition » (0,076), The Survivors of the Chancellor ×2 (0,091), Clotelle ×2 (0,119), Jargon File
2.9.10 / 4.0.0 (0,122). Puis des paires proches mais distinctes : Sonnets de Shakespeare (0,22), Beatrix
Potter (0,23), deux éditions de l'Origine des espèces (0,33).

Degré moyen 10 → seuil 0,7695, 9 500 arêtes, **1 127 livres isolés sur 1 900**, 1 199 composantes (la
plus grande : 582 livres). PageRank : 100 itérations sans atteindre 1e-10 (écart final 6e-10), 24 ms.
En tête : Monte-Cristo, Dombey and Son, Little Dorrit, David Copperfield… (des romans très longs).
Corrélation PageRank / degré : 0,91.

Autres seuils (fixés à la main) :

| Seuil | Degré moyen | Livres isolés | Plus grande composante |
|---|---|---|---|
| 0,7695 | 10 | 1 127 | 582 |
| 0,80 | 37 | 768 | 977 |
| 0,83 | 120 | 474 | 1 309 |
| 0,85 | 236 | 316 | 1 499 |
| 0,87 | 413 | 188 | 1 692 |
| 0,89 | 648 | 100 | 1 790 |
| 0,91 | 933 | 38 | 1 860 |

Cause : **biais de longueur**. Un livre long a un grand vocabulaire ; deux livres longs en partagent
beaucoup, alors qu'un livre court comparé à un long a une petite intersection et une grande union.
Au seuil 0,7695 :

| Longueur (mots) | Livres | Isolés | Degré moyen |
|---|---|---|---|
| 10 000 à 25 000 | 353 | 90 % | 0,1 |
| 25 000 à 50 000 | 458 | 87 % | 0,2 |
| 50 000 à 100 000 | 657 | 56 % | 2,4 |
| 100 000 à 200 000 | 342 | 11 % | 30,7 |
| plus de 200 000 | 90 | 7 % | 75,2 |

Médiane de longueur : 40 229 mots pour les isolés, 100 441 pour les autres. Avec ce graphe, PageRank
mesure surtout la longueur du livre. Voisins de Moby-Dick : cinq romans de Dickens.

### Doublons retirés (`corpus.RetraitDoublons`, 13/11/2026)

Les 7 paires à distance < 0,13 (sur tous les mots) sont des éditions du même livre : on garde le plus
petit identifiant. Retirés : 928 (Alice, édition HTML), 1316 (Some Reminiscences = A Personal Record),
2225 (Captains Courageous), 2154 (Around the World, Junior Deluxe Edition), 1698 (Survivors of the
Chancellor), 2095 (Clotelle), 817 (Jargon File 4.0, gardé 2.9.10). La paire suivante est à 0,22 (deux
recueils différents des Sonnets) : le seuil 0,13 sépare nettement doublons et livres proches.
Les fichiers sont déplacés dans `data/doublons/` (rien n'est supprimé), les livres retirés de
`metadonnees.tsv` et ajoutés à `rejetes.tsv`. **Corpus final : 1 893 livres** (≥ 1 664).
Index reconstruit : 415 869 mots, 12 390 627 postings, 100 Mo (55 s cette fois contre 22 s la première :
une mesure isolée ne suffit pas, à répéter pour les courbes).

### Empreinte : les K mots les plus fréquents de chaque livre (décision du point 5)

Pour enlever le biais de longueur, chaque livre est représenté pour Jaccard par ses K mots non vides
les plus fréquents (à égalité : ordre alphabétique). Tous les ensembles ont la même taille, la règle
de l'énoncé ne change pas (distance de Jaccard, arête sous un seuil). Calculé depuis l'index
(les occurrences y sont), sans nouveau fichier. `Jaccard(index, partMax, K)`, K = 0 : tous les mots.

Comparaison (1 893 livres, degré moyen visé 10, mots vides à 50 %) :

| K | Seuil | Livres isolés | Plus grande composante | Corrélation PageRank / longueur | Isolés < 25 000 mots |
|---|---|---|---|---|---|
| tous | 0,770 | 1 133 | 584 | 0,52 | 89 % |
| 250 | 0,915 | **270** | 1 491 | **0,33** | 29 % |
| 500 | 0,901 | 277 | 1 542 | 0,42 | 32 % |
| 1 000 | 0,878 | 337 | 1 453 | 0,51 | 42 % |
| 2 000 | 0,845 | 474 | 1 246 | 0,57 | 57 % |

Avec moins de mots vides (seuil à 20 % ou 10 % des livres), le gain est faible : 277 à 320 isolés,
corrélation 0,21 à 0,31. On garde 50 %, la même définition que pour le classement du moteur.
Environ 270 livres (14 %) restent isolés quel que soit le réglage : des livres sans vrai voisin dans
le corpus (une option à discuter si besoin : relier aussi chaque livre à son plus proche voisin).

**Choix : K = 250** (`ConstructionGraphe.MOTS_PAR_LIVRE_DEFAUT`). Les voisins deviennent thématiques :

| Livre | Tous les mots | K = 250 |
|---|---|---|
| Moby-Dick | Dombey and Son, Martin Chuzzlewit, Little Dorrit, David Copperfield, Bleak House | The Cruise of the Cachalot, Two Years Before the Mast, The Sea-Wolf, The Coral Island, The Mirror of the Sea |
| Monte-Cristo | Ten Years Later, Little Dorrit, Dombey and Son, Nicholas Nickleby, Les Misérables | Les Trois Mousquetaires, Ten Years Later, Vingt ans après, The Mystery of Orcival |
| Alice | (isolé) | Through the Looking-Glass |
| David Copperfield | 5 Dickens | 5 Dickens |
| L'Origine des espèces | autre édition, Life of Darwin ×2, Descent of Man | autre édition, Descent of Man, Darwin and Modern Science |

Graphe final (K = 250) : 29 387 mots partagés, vecteurs de bits 7,0 Mo (contre 44 Mo), distances
0,11 s (contre 1,7 s). Distances : min 0,127, 5 % 0,954, médiane 0,986, 95 % 0,998. Seuil 0,9154,
8 823 arêtes, degré moyen 9,32 (des égalités au seuil), 270 isolés, 311 composantes, la plus grande
1 491 livres. PageRank : 107 itérations (limite passée de 100 à 200), 37 ms. Corrélation PageRank /
degré 0,82. En tête : Gutenberg Encyclopedia vol. 1, Bulchevy's Book of English Verse, Critical and
Historical Essays : des recueils variés, au centre du graphe, ce qui est attendu pour une centralité.

### Serveur sur le vrai corpus (13/11/2026)

Démarrage 6,9 s, 297 Mo en mémoire. « whale » : 282 livres ; `produit` : Moby-Dick, Cruise of the
Cachalot, South Sea Tales ; suggestions : Adventure, Two Years Before the Mast, The Mirror of the Sea,
The Sea-Wolf… `pagerank` met en tête des livres qui ne citent le mot qu'une à quatre fois (l'encyclopédie) :
c'est pour cela que le tri par défaut est le produit. Regex `wh(a|e)le` : 30 mots, 414 livres,
759 ms au premier appel (à mesurer à chaud pour les courbes ; 14 à 22 ms sur test20).

### Téléchargement du corpus (lancé le 13/11/2026 à 01:08)

Environ 5 livres gardés par minute (106 requêtes en 16,6 min, soit environ 9 s par livre avec la pause
de 1,5 s) : le corpus complet demandera environ 6 h, et non 1 h 30 à 2 h comme estimé.

Fin le 13/11/2026 à 05:06 (environ 4 h en tout, le rythme est monté à 7 livres par minute).
1 900 livres gardés pour 2 120 téléchargements essayés ; 29 titres exclus et 125 doublons écartés avant
téléchargement. Rejetés après téléchargement (`data/rejetes.tsv`) : 214 trop courts, 5 fichiers absents
(404), 1 sans en-tête Gutenberg reconnu. Corpus : 794 Mo de texte, `metadonnees.tsv` de 237 Ko.

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
