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
