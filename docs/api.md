# Contrat d'API

Le serveur écoute sur `http://IP-du-serveur:8080`. Toutes les routes sont en GET et renvoient du JSON (UTF-8).
Les données viennent du moteur réel (`recherche.Moteur`), chargé au démarrage depuis le dossier
`moteur.dossier` (`data` par défaut ; `--moteur.dossier=data/test20` pour les 20 livres de test).

## Routes

```
GET /api/search?q=whale&tri=produit                         recherche par mot-clef
GET /api/search/advanced?regex=wh(a|e)le&mode=index&tri=…   recherche avancée (RegEx)
GET /api/books/{id}                                         page d'un livre
```

Paramètres :

| Paramètre | Valeurs | Défaut |
|---|---|---|
| `q` | texte saisi (obligatoire pour `/api/search`) | — |
| `regex` | la regex (obligatoire pour `/api/search/advanced`) | — |
| `mode` | `index` (regex testée sur les mots de l'index) ou `texte` (texte complet, **pas encore disponible** : erreur 400) | `index` |
| `tri` | `produit`, `occurrences` ou `pagerank` (voir Classement) | `produit` |

## Réponse d'une recherche

Les suggestions sont incluses : le front ne fait qu'un appel. **Tous** les résultats sont renvoyés
(au plus le nombre de livres, environ 1 900) : c'est le front qui pagine.

```json
{
  "requete": "whale",
  "type": "motcle",
  "tempsMs": 3.95,
  "resultats": [
    { "id": 2701, "titre": "…", "auteur": "…", "occurrences": 1526, "pagerank": 0.0713, "score": 0.000422 }
  ],
  "suggestions": [
    { "id": 456, "titre": "…", "auteur": "…" }
  ]
}
```

- `type` : `motcle` ou `regex`.
- `tempsMs` : nombre à virgule (millisecondes, 2 décimales). Temps du moteur seul : recherche, classement et
  suggestions, sans le réseau ni l'écriture du JSON.
- `occurrences` : nombre total d'occurrences des mots cherchés (mot-clef) ou des mots trouvés (regex) dans le livre.
- `pagerank` : PageRank du livre dans le graphe de Jaccard (la somme sur tous les livres vaut 1).
- `score` : `(occurrences des mots non vides / nombre de mots du livre) × pagerank`. C'est le critère du tri
  `produit` ; il est toujours renvoyé, quel que soit le tri.
- `suggestions` : voisins dans le graphe de Jaccard des 3 premiers résultats (selon le tri choisi), sans ces
  3 livres, du plus proche au plus loin, 10 au plus. Liste vide si ces livres n'ont pas de voisin.

### Classement (`tri`)

| `tri` | Ordre des résultats |
|---|---|
| `produit` (défaut) | `score` décroissant |
| `occurrences` | `occurrences` décroissant |
| `pagerank` | `pagerank` décroissant |

Mots vides : un mot présent dans plus de 50 % des livres (« the », « and »…) est cherché normalement mais ne
compte pas dans `score`. Pour « the whale », les livres doivent contenir les deux mots, mais seul « whale »
compte dans le score. Si tous les mots sont vides (« the »), ils comptent tous.

### Recherche par mot-clef

- Le texte saisi est découpé en mots comme les livres (minuscules, lettres seulement, mots d'une lettre ignorés).
- Avec plusieurs mots, on renvoie les livres qui les contiennent tous (ET).

### Recherche par regex : champs en plus

Pour `type = "regex"`, la réponse contient deux champs de plus (absents pour un mot-clef) :

```json
{
  "nbMotsTrouves": 13,
  "motsTrouves": ["narwhale", "whale", "whaleboat", "whaler"]
}
```

- `nbMotsTrouves` : nombre de mots de l'index qui vérifient la regex.
- `motsTrouves` : les 50 premiers (ordre alphabétique), pour l'affichage.
- Un mot vérifie la regex s'il en contient une occurrence (`whal` trouve `whale` et `narwhale`).
- Un livre est renvoyé s'il contient au moins un de ces mots.
- Syntaxe : lettres, `.`, `( )`, `|`, `*`, `+`, `?`, et classes `[abc]`, `[a-z]` (pas de `[^...]`). Les majuscules
  sont ignorées.

## Page d'un livre

```
GET /api/books/{id}
```

```json
{
  "id": 11,
  "titre": "Alice's Adventures in Wonderland",
  "auteur": "Carroll, Lewis, 1832-1898",
  "urlSource": "https://www.gutenberg.org/ebooks/11",
  "extrait": "Premier paragraphe…\n\nDeuxième paragraphe…",
  "voisins": [ { "id": 12, "titre": "Through the Looking-Glass", "auteur": "…" } ]
}
```

- `extrait` : environ les 1 500 premiers caractères du texte, coupés sur un mot (terminé par « … » s'il est
  coupé). Les paragraphes sont séparés par une ligne vide (`\n\n`) : à afficher avec `white-space: pre-line`.
- `voisins` : voisins du livre dans le graphe de Jaccard, du plus proche au plus loin, 10 au plus
  (liste vide pour un livre isolé).

## Erreurs

Une requête refusée renvoie un code d'erreur et un message à afficher tel quel à l'utilisateur :

```json
{ "erreur": "Regex invalide : Parenthèse fermante manquante" }
```

| Code | Cas |
|---|---|
| 400 | `q` ou `regex` absent ; mot-clef sans aucun mot à chercher (vide, ponctuation, mots d'une lettre) |
| 400 | regex vide, de plus de 100 caractères, mal écrite, qui accepte le mot vide (`a*`), ou trop complexe (automate de plus de 2 000 états) |
| 400 | `tri` ou `mode` inconnu ; `mode=texte` (pas encore disponible) ; `{id}` qui n'est pas un nombre |
| 404 | `/api/books/{id}` : livre introuvable |
