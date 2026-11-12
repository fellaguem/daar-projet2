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
