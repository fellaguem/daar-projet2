# Contrat d'API

Le serveur écoute sur `http://IP-du-serveur:8080`. Toutes les routes sont en GET et renvoient du JSON.

## Recherche par mot-clef

```
GET /api/search?q=whale
```

## Recherche avancée (RegEx)

```
GET /api/search/advanced?regex=wh(a|e)le&mode=index
```

`mode` vaut `index` (la regex est testée sur les mots de l'index, par défaut) ou `texte` (testée sur le texte complet, plus lent).

La regex ne gère que `( ) | * .` et les lettres.

## Réponse d'une recherche

Les suggestions sont incluses : le front ne fait qu'un appel.

```json
{
  "requete": "whale",
  "type": "motcle",
  "tempsMs": 12,
  "resultats": [
    { "id": 123, "titre": "…", "auteur": "…", "occurrences": 42, "pagerank": 0.0021 }
  ],
  "suggestions": [
    { "id": 456, "titre": "…", "auteur": "…" }
  ]
}
```

`type` vaut `motcle` ou `regex`.

### Recherche par mot-clef

- Le texte saisi est découpé en mots comme les livres (minuscules, lettres seulement, mots d'une lettre ignorés).
- Avec plusieurs mots, on renvoie les livres qui les contiennent tous (ET) ; `occurrences` est la somme des occurrences.

### Recherche par regex : champs en plus

Pour `type = "regex"`, la réponse contient deux champs de plus (absents pour un mot-clef) :

```json
{
  "nbMotsTrouves": 3,
  "motsTrouves": ["whale", "whaleboat", "whaler"]
}
```

- `nbMotsTrouves` : nombre de mots de l'index qui vérifient la regex.
- `motsTrouves` : les 50 premiers (ordre alphabétique), pour l'affichage.
- Un mot vérifie la regex s'il en contient une occurrence (`whal` trouve `whale` et `whaleboat`).
- Un livre est renvoyé s'il contient au moins un de ces mots ; `occurrences` est la somme des occurrences de ces mots.
- Syntaxe : lettres, `.`, `( )`, `|`, `*`, `+`, `?`, et classes `[abc]`, `[a-z]` (pas de `[^...]`). Les majuscules sont ignorées.

## Erreurs

Une requête refusée renvoie le code 400 et un message à afficher à l'utilisateur :

```json
{ "erreur": "Regex invalide : Parenthèse fermante manquante" }
```

Cas : regex vide, de plus de 100 caractères, mal écrite, qui accepte le mot vide (`a*`), ou trop complexe
(automate de plus de 2 000 états).

## Page d'un livre

```
GET /api/books/{id}
```

```json
{
  "id": 123,
  "titre": "…",
  "auteur": "…",
  "urlSource": "https://www.gutenberg.org/ebooks/123",
  "extrait": "…",
  "voisins": [ { "id": 456, "titre": "…", "auteur": "…" } ]
}
```

## État actuel

Les trois routes renvoient des données factices (voir `backend/src/main/java/api/DonneesFactices.java`).
