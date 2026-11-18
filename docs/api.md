# Contrat d'API

Le serveur écoute sur `http://IP-du-serveur:8080`. Les routes renvoient du JSON (UTF-8).
Les données viennent du moteur réel (`recherche.Moteur`), chargé au démarrage depuis le dossier
`moteur.dossier` (`data` par défaut ; `--moteur.dossier=data/test20` pour les 20 livres de test).

Les paramètres doivent être encodés (`encodeURIComponent`) : Tomcat refuse `|`, `(` ou `)` en clair
dans l'URL.

## Ce qui est disponible

| Fonction | État |
|---|---|
| Recherche par mot-clef, regex (mode `index`), page d'un livre, tris `produit` / `occurrences` / `pagerank`, erreurs 400 / 404 | disponible |
| Suggestions avec repli pour les livres isolés ; démarrage à chaud ; regex « mot entier » par défaut (`motEntier`) | disponible (13/11) |
| Tri `bm25`, champ `bm25` ; repli OU (`repli`, `motsPresents`) | abandonné (hors énoncé) |
| « Pourquoi ce résultat » (`occurrencesUtiles`, `longueur`, `motsVides`) ; extrait surligné (`passages`) ; clics (`POST .../clic`, champ `clics` des suggestions) | abandonné (hors énoncé) |

Les ajouts marqués « abandonné (hors énoncé) » n'ont pas été codés (décision du 18/11 : ils ne sont pas
demandés par l'énoncé). Leurs routes, paramètres et champs n'existent pas dans le serveur ; leur description
est gardée ci-dessous pour mémoire.

## Routes

```
GET  /api/search?q=whale&tri=produit                                     recherche par mot-clef
GET  /api/search/advanced?regex=wh(a|e)le&mode=index&motEntier=true&tri=…   recherche avancée (RegEx)
GET  /api/books/{id}                                                      page d'un livre
GET  /api/books/{id}?q=white whale                                        passages surlignés (abandonné, hors énoncé)
POST /api/books/{id}/clic                                                 un clic sur un livre (abandonné, hors énoncé)
```

Paramètres :

| Paramètre | Valeurs | Défaut |
|---|---|---|
| `q` | texte saisi (obligatoire pour `/api/search` ; pour `/api/books/{id}` : abandonné, hors énoncé) | — |
| `regex` | la regex (obligatoire pour `/api/search/advanced`) | — |
| `mode` | `index` (regex testée sur les mots de l'index) ou `texte` (texte complet : réservé aux mesures, erreur 400) | `index` |
| `motEntier` | `true` : le mot entier doit vérifier la regex ; `false` : le mot doit contenir un morceau qui la vérifie | `true` |
| `tri` | `produit`, `occurrences` ou `pagerank` (`bm25` : abandonné, hors énoncé ; voir Classement) | `produit` |

## Réponse d'une recherche

Les suggestions sont incluses : le front ne fait qu'un appel. **Tous** les résultats sont renvoyés
(au plus le nombre de livres, environ 1 900) : c'est le front qui pagine. Exemple de réponse, valeurs
données à titre d'exemple. Les champs `motsVides`, `occurrencesUtiles`, `longueur`, `bm25` et `clics` sont
abandonnés (hors énoncé) : ils sont absents de la réponse réelle.

```json
{
  "requete": "the whale",
  "type": "motcle",
  "tempsMs": 3.95,
  "motsVides": ["the"],
  "resultats": [
    {
      "id": 15, "titre": "Moby-Dick; or, The Whale", "auteur": "Melville, Herman, 1819-1891",
      "occurrences": 15668, "occurrencesUtiles": 1244, "longueur": 215803,
      "pagerank": 0.00118, "score": 0.0000068, "bm25": 7.42
    }
  ],
  "suggestions": [
    { "id": 1356, "titre": "The Cruise of the \"Cachalot\" Round the World After Sperm Whales", "auteur": "…", "clics": 4 }
  ]
}
```

- `type` : `motcle` ou `regex`.
- `tempsMs` : nombre à virgule (millisecondes, 2 décimales). Temps du moteur seul : recherche, classement et
  suggestions, sans le réseau ni l'écriture du JSON.
- `motsVides` *(abandonné, hors énoncé)* : mots de la requête (mot-clef) ou mots trouvés (regex, 50 au plus) qui ne comptent
  pas dans le classement (voir Mots vides). Liste vide s'il n'y en a pas.

Champs de chaque résultat :

| Champ | Sens | Depuis |
|---|---|---|
| `id`, `titre`, `auteur` | le livre (`id` = numéro Gutenberg) | disponible |
| `occurrences` | nombre total d'occurrences des mots cherchés (mot-clef) ou des mots trouvés (regex) dans le livre | disponible |
| `pagerank` | PageRank du livre dans le graphe de Jaccard (la somme sur tous les livres vaut 1) | disponible |
| `score` | `occurrencesUtiles / longueur × pagerank` ; critère du tri `produit`, toujours renvoyé | disponible |
| `bm25` | score BM25 du livre pour la requête (formule plus bas), toujours renvoyé | abandonné (hors énoncé) |
| `occurrencesUtiles` | occurrences des mots non vides seulement (celles qui comptent dans `score`) | abandonné (hors énoncé) |
| `longueur` | nombre de mots indexés du livre | abandonné (hors énoncé) |
| `motsPresents` | seulement en repli OU : mots de la requête présents dans ce livre | abandonné (hors énoncé) |

*(Abandonné, hors énoncé.)* Avec `occurrencesUtiles`, `longueur` et `pagerank`, le front aurait pu afficher
« pourquoi ce résultat » : `score = occurrencesUtiles / longueur × pagerank`. Le front affiche à la place une
pertinence relative (score / meilleur score de la recherche, en %).

### Classement (`tri`)

| `tri` | Ordre des résultats |
|---|---|
| `produit` (défaut) | `score` décroissant |
| `occurrences` | `occurrences` décroissant |
| `pagerank` | `pagerank` décroissant |
| `bm25` *(abandonné, hors énoncé)* | `bm25` décroissant |

À égalité : `score` décroissant, puis `id` croissant.

**BM25** *(abandonné, hors énoncé)*. Pour un livre L et les mots utiles M de la requête (mots non vides ; tous les mots
s'ils sont tous vides ; pour une regex, les mots trouvés) :

```
bm25(L) = somme sur m dans M de  idf(m) × tf(m, L) × (k1 + 1) / ( tf(m, L) + k1 × (1 - b + b × |L| / moy) )

idf(m)  = ln( 1 + (N - df(m) + 0,5) / (df(m) + 0,5) )
```

- `tf(m, L)` : occurrences de m dans L ; `df(m)` : nombre de livres qui contiennent m ; `N` : nombre de livres ;
- `|L|` : nombre de mots indexés de L (`longueur`) ; `moy` : moyenne de `|L|` sur tous les livres ;
- `k1 = 1,2` et `b = 0,75` ; `ln` : logarithme népérien. Un mot absent de L ne compte pas (tf = 0).

### Mots vides

Un mot présent dans plus de 50 % des livres (« the », « and »…) est cherché normalement mais ne compte
pas dans `score` ni dans `bm25`. Pour « the whale », les livres doivent contenir les deux mots, mais seul
« whale » compte. Si tous les mots sont vides (« the »), ils comptent tous.

### Recherche par mot-clef

- Le texte saisi est découpé en mots comme les livres (minuscules, lettres seulement, mots d'une lettre ignorés).
- Avec plusieurs mots, on renvoie les livres qui les contiennent tous (ET).

**Repli OU** *(abandonné, hors énoncé)*. Si la requête a au moins 2 mots différents et qu'aucun livre ne les contient tous :

- on renvoie les livres qui contiennent **au moins un** des mots utiles (les mots vides ne comptent pas
  dans le OU, sauf si la requête n'a que des mots vides) ;
- la réponse contient `"repli": "ou"`, et chaque résultat a `motsPresents` (les mots utiles présents dans
  ce livre) ;
- ordre : d'abord le nombre de `motsPresents` (décroissant), puis le tri choisi ;
- le front affiche par exemple « Aucun livre ne contient tous les mots ; voici les livres qui en
  contiennent au moins un ».

Sans repli (cas normal, ou si le OU ne trouve rien non plus), `repli` et `motsPresents` sont absents.

```json
{ "requete": "whale unicorn", "type": "motcle", "repli": "ou",
  "resultats": [ { "id": 15, "…": "…", "motsPresents": ["whale"] } ] }
```

### Recherche par regex : champs en plus

Pour `type = "regex"`, la réponse contient ces champs de plus (absents pour un mot-clef) :

```json
{
  "nbMotsTrouves": 3,
  "motsTrouves": ["whale", "whaler", "whales"],
  "motEntier": true
}
```

- `nbMotsTrouves` : nombre de mots de l'index qui vérifient la regex.
- `motsTrouves` : les 50 premiers (ordre alphabétique), pour l'affichage.
- `motEntier` : le choix appliqué.
  - `true` (défaut) : le mot entier doit vérifier la regex. `wh(a|e)le` trouve `whale` et `whele`, pas
    `narwhale` ; pour un morceau de mot, écrire `.*whale.*`.
  - `false` : le mot doit contenir un morceau qui vérifie la regex (comme `egrep`) : `whal` trouve `whale`,
    `whaler` et `narwhale`.
- Un livre est renvoyé s'il contient au moins un des mots trouvés.
- Syntaxe : lettres, `.`, `( )`, `|`, `*`, `+`, `?`, et classes `[abc]`, `[a-z]` (pas de `[^...]`). Les majuscules
  sont ignorées.

## Suggestions

1. On prend les **3 premiers résultats qui ont des voisins** dans le graphe de Jaccard, parmi les 10 premiers
   (14 % des livres n'ont aucun voisin) : ce sont les sources.
2. On réunit tous les voisins de ces sources, sauf les sources elles-mêmes.
3. *(abandonné, hors énoncé)* On garde les **10 qui ont le plus de clics** ; à égalité, les plus proches (distance de Jaccard à la
   source la plus proche), puis l'`id` croissant. Version réelle : les 10 plus proches (distance de Jaccard à
   la source la plus proche), puis l'`id` croissant.

Chaque suggestion : `{ id, titre, auteur }` (`clics` : abandonné, hors énoncé). Liste vide si aucun des 10
premiers résultats n'a de voisin.

## Page d'un livre

```
GET /api/books/{id}
GET /api/books/{id}?q=white whale     (abandonné, hors énoncé)
```

Le champ `passages` de l'exemple ci-dessous est abandonné (hors énoncé) : il est absent de la réponse réelle.

```json
{
  "id": 11,
  "titre": "Alice's Adventures in Wonderland",
  "auteur": "Carroll, Lewis, 1832-1898",
  "urlSource": "https://www.gutenberg.org/ebooks/11",
  "extrait": "Premier paragraphe…\n\nDeuxième paragraphe…",
  "voisins": [ { "id": 12, "titre": "Through the Looking-Glass", "auteur": "…" } ],
  "passages": [ { "texte": "…said the White Rabbit, as…", "surlignes": [[9, 14]] } ]
}
```

- `extrait` : environ les 1 500 premiers caractères du texte, coupés sur un mot (terminé par « … » s'il est
  coupé). Les paragraphes sont séparés par une ligne vide (`\n\n`) : à afficher avec `white-space: pre-line`.
- `voisins` : voisins du livre dans le graphe de Jaccard, du plus proche au plus loin, 10 au plus
  (liste vide pour un livre isolé).

**Passages surlignés** *(abandonné, hors énoncé)*, seulement si `q` est donné :

- `q` est découpé en mots comme une requête. Pour une regex, le front envoie les `motsTrouves` séparés par
  des espaces.
- Le serveur cherche les mots dans le texte du livre avec KMP, sur une copie du texte mise en minuscules
  lettre par lettre (`Character.toLowerCase`) : une lettre reste une lettre, les positions ne bougent pas.
  Une occurrence ne compte que si c'est un mot entier (pas de lettre juste avant ni juste après).
- Au plus **3 passages** d'environ 300 caractères, autour des premières occurrences, sans chevauchement.
  `texte` est le texte d'origine (majuscules gardées) ; chaque retour à la ligne ou tabulation y est
  remplacé par un espace, un caractère pour un caractère.
- `surlignes` : positions `[début, fin[` dans `texte` (indices de caractères, comme `String.slice` en
  JavaScript). Le front découpe `texte` avec ces positions : pas de HTML à échapper côté serveur.
- `passages` vaut `[]` si aucun mot n'est trouvé dans le texte ; le champ est absent sans `q`.

## Clics *(abandonné, hors énoncé)*

```
POST /api/books/{id}/clic      →  204 (sans corps)
```

- Le front l'appelle quand l'utilisateur ouvre un livre depuis une liste de résultats ou de suggestions.
- Les compteurs sont gardés en mémoire et écrits dans `clics.tsv` (dans le dossier des données : `id`,
  nombre de clics), toutes les 30 secondes s'ils ont changé et à l'arrêt du serveur ; relus au démarrage.
- Ils servent à ordonner les suggestions (champ `clics`).

## Erreurs

Une requête refusée renvoie un code d'erreur et un message à afficher tel quel à l'utilisateur :

```json
{ "erreur": "Regex invalide : Parenthèse fermante manquante" }
```

| Code | Cas |
|---|---|
| 400 | `q` ou `regex` absent ; mot-clef sans aucun mot à chercher (vide, ponctuation, mots d'une lettre) |
| 400 | regex vide, de plus de 100 caractères, mal écrite, ou trop complexe (automate de plus de 2 000 états) |
| 400 | avec `motEntier=false` seulement : regex qui accepte le mot vide (`a*`), qui trouverait tous les mots |
| 400 | `tri`, `mode` ou `motEntier` inconnu ; `mode=texte` (réservé aux mesures) ; `{id}` qui n'est pas un nombre |
| 404 | `/api/books/{id}` : livre introuvable (`POST /api/books/{id}/clic` : abandonné, hors énoncé) |

En mode mot entier, une regex qui accepte le mot vide est permise : `a*` trouve les mots faits de « a »,
`.*` trouve tous les mots (donc tous les livres).
