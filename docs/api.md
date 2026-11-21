# Contrat d'API

JSON (UTF-8), sur `http://IP-du-serveur:8080`. Paramètres encodés avec `encodeURIComponent`.

```
GET /api/search?q=whale&tri=produit
GET /api/search/advanced?regex=wh(a|e)le&motEntier=true&tri=produit
GET /api/books/{id}
```

- `tri` : `produit` (défaut), `occurrences` ou `pagerank`.
- `motEntier` : `true` (défaut, le mot entier vérifie la regex) ou `false` (un morceau du mot).

## Recherche

```json
{
  "requete": "whale",
  "type": "motcle",                // ou "regex"
  "tempsMs": 2.35,
  "resultats": [
    {
      "id": 15,
      "titre": "Moby-Dick; or, The Whale",
      "auteur": "Melville, Herman, 1819-1891",
      "occurrences": 1244,
      "pagerank": 0.00118,
      "score": 0.0000070
    }
  ],
  "suggestions": [
    {
      "id": 1163,
      "titre": "Adventure",
      "auteur": "London, Jack, 1876-1916"
    }
  ],
  "nbMotsTrouves": 1,              // (regex seulement)
  "motsTrouves": ["whale"],        // (regex seulement)
  "motEntier": true                // (regex seulement)
}


```



## Livre

```json
{
  "id": 15,
  "titre": "Moby-Dick; or, The Whale",
  "auteur": "Melville, Herman, 1819-1891",
  "urlSource": "https://www.gutenberg.org/ebooks/15",
  "extrait": "…",
  "voisins": [
    {
      "id": 1356,
      "titre": "The Cruise of the \"Cachalot\" Round the World After Sperm Whales",
      "auteur": "Bullen, Frank Thomas, 1857-1915"
    }
  ]
}

```

## Erreurs



| Code | Cas |
|---|---|
| 400 | `q` ou `regex` manquant, ou aucun mot à chercher |
| 400 | regex mal écrite, trop longue ou trop complexe |
| 400 | `motEntier=false` avec une regex qui accepte le mot vide (`a*`) |
| 400 | `tri` ou `motEntier` inconnu, `{id}` qui n'est pas un nombre |
| 404 | Aucun livre de la bibliothèque n'a ce numéro |
