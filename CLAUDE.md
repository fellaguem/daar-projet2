# Projet DAAR 2 — Moteur de recherche d'une bibliothèque

Binôme : Moustafa FELLAGUE et Younes MANSOUS (Master 2 STL, Sorbonne Université).
Rendu : 22 novembre 2026, 23h59, sur Moodle (vidéo pitch de 5 min avec démo, pas de présentation orale).

## Objectif

Application web de recherche de livres dans une bibliothèque de documents au format texte.

## Exigences de l'énoncé (non négociables)

- Bibliothèque d'au moins 1 664 livres, chacun d'au moins 10 000 mots.
- Recherche : à partir d'un mot-clef S, renvoyer tous les livres dont l'index contient S.
- Recherche avancée : à partir d'une RegEx, renvoyer les livres dont l'index contient un mot qui vérifie la RegEx
  (variante sur le texte complet possible, mais plus lente).
- Classement des résultats des deux recherches, avec au moins un indice de centralité
  (closeness, betweenness ou PageRank) calculé sur le graphe de Jaccard. Choix retenu : PageRank.
- Suggestions : après une recherche, proposer les voisins dans le graphe de Jaccard des 2-3 meilleurs résultats.
- L'application doit fonctionner sur au moins 2 machines clientes en même temps (démo dans la vidéo).
- Archive finale de moins de 30 Mo, nommée `daar-projet2-FELLAGUE-MANSOUS.zip` :
  le corpus n'y entre pas, on fournit le script qui le télécharge et la provenance des fichiers.

## Stack

- Serveur : Java 17 + Spring Boot, build Maven (réutilise directement le code du projet 1).
- Front : React + Vite. Le build du front va dans `backend/src/main/resources/static` : Spring Boot le sert,
  donc pour la démo chaque machine ouvre simplement `http://IP-du-serveur:8080`.
- Stockage : fichiers dans `data/`, chargés en mémoire au démarrage du serveur. Pas de base de données.
  Exception possible : SQLite pour compter les clics, seulement si on fait ce bonus.
- Interdit : Lucene, Elasticsearch ou toute bibliothèque qui ferait la recherche à notre place.

## Organisation des dossiers

```
daar-projet2/
├── CLAUDE.md
├── backend/                Java 17 + Spring Boot (Maven)
│   └── src/main/java/
│       ├── parser/ ast/ automate/ kmp/ experimentation/   projet 1 (copie modifiable, voir plus bas)
│       ├── corpus/         téléchargement + nettoyage
│       ├── index/          découpage en mots + index inversé
│       ├── graphe/         Jaccard + PageRank
│       ├── recherche/      mot-clef, RegEx, classement, suggestions
│       └── api/            routes REST
├── frontend/               React + Vite
│   └── src/
│       ├── pages/          Accueil, Résultats, Livre
│       ├── components/     BarreRecherche, ListeResultats, Suggestions
│       └── api.js          appels à l'API, avec un mode données factices
├── data/                   livres, index, graphe, scores (non commité)
└── docs/
    ├── api.md              contrat d'API
    └── notes.md            choix et mesures pour le rapport
```

## Contrat d'API

```
GET /api/search?q=whale                               recherche par mot-clef
GET /api/search/advanced?regex=wh(a|e)le&mode=index   mode = index | texte
GET /api/books/{id}                                   page d'un livre
```

Réponse d'une recherche (les suggestions sont incluses, le front ne fait qu'un appel) :

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

`type` vaut `motcle` ou `regex`. `GET /api/books/{id}` renvoie
`{ id, titre, auteur, urlSource, extrait, voisins: [{ id, titre, auteur }] }`.

## Réutilisation du projet 1

Le projet 1 (clone d'`egrep` en Java) fournit les packages `parser`, `ast`, `automate`, `kmp` et `experimentation`.

- Recherche avancée : l'AFD minimal du projet 1 (`MatcherUnePasse`) est testé sur chaque mot de l'index.
- Recherche par mot-clef : recherche exacte dans l'index (dichotomie sur le vocabulaire), pas `Kmp`.
  `Kmp` sert pour les extraits et pour la variante « texte complet » de la recherche avancée.
- Le projet 1 copié dans `backend/` peut être modifié (compter les occurrences, étendre le parseur avec
  `+ ? [..]`), mais les tests du projet 1 (`backend/src/test/java/projet1`) doivent rester verts, et chaque
  changement est noté dans `docs/notes.md` (« projet 1 étendu »).
- Limite connue : la regex ne gère que `( ) | * .` et les lettres.

## Pipeline de données (hors ligne, lancé une fois)

1. Corpus : téléchargement depuis Project Gutenberg (texte brut). Enlever l'en-tête et la licence Gutenberg,
   garder les livres d'au moins 10 000 mots, enregistrer les métadonnées (id, titre, auteur, URL source).
   Passer par le catalogue officiel et un miroir, avec une pause entre deux téléchargements.
2. Index inversé : mot → (livre, nombre d'occurrences), sauvegardé sur disque. Les mots vides restent dans
   l'index (chercher « the » doit marcher), seuls les mots d'une lettre sont ignorés. Les mots vides (mots
   présents dans une grande part des livres) sont retirés du graphe de Jaccard et du classement.
   Recherche à plusieurs mots : les livres qui contiennent tous les mots (ET), score = somme des occurrences.
3. Graphe de Jaccard : distance de Jaccard entre les ensembles de mots des livres, arête si la distance est
   sous un seuil. Environ 1,4 million de paires : tester d'abord sur 100 livres. Sauvegarder la liste d'arêtes.
4. PageRank sur ce graphe, précalculé et sauvegardé.

## Règles

- Une seule classe découpe le texte en mots (`index.Tokeniseur` : suite de lettres, en minuscules) : elle sert à la
  fois pour construire l'index et pour lire les requêtes. Les apostrophes et tirets coupent les mots
  (« don't » donne « don » et « t »), limite à noter dans le rapport. Les mots d'une lettre sont écartés
  par `Index.pourChaqueMotIndexable`, la même règle pour les livres et pour les requêtes.
- Index stocké en tableaux d'entiers (ids de livres, nombres d'occurrences), pas une `HashMap` par livre.
- Code et commentaires en français, simples, comme dans le projet 1.
- Les données générées vont dans `data/` et ne sont pas commitées (`.gitignore`).
- Mesurer les temps (moyenne + écart type) : le rapport doit contenir des courbes de performance.
- Noter au fil de l'eau dans `docs/notes.md` les choix et les chiffres utiles pour le rapport.

## Répartition

- Moustafa : corpus, index, algorithmes de recherche, classement, suggestions, tests de performance.
- Younes : serveur et API, front, démo multi-machines.
- Les deux : graphe de Jaccard et PageRank.
