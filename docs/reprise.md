# Reprise du travail

À lire au début de chaque session. Rédigé le 13/11/2026.

## Décisions prises

- Corpus : anglais seulement. Filtres de doublons (titre + auteur) et de contenu
  (Factbook, Census, Thesaurus, Dictionary, Gazetteer) avant le comptage de `--max`.
- Jaccard : mots présents dans plus de 50 % des livres retirés, mots d'un seul livre comptés
  dans la taille mais sans bit. Seuil déduit d'un degré moyen de 10 (aussi réglable à la main).
- PageRank : version classique d'abord, version pondérée par la similarité en bonus si on
  a le temps, avec comparaison au degré pour le rapport.
- Mots vides : gardés dans l'index, ignorés dans le classement.
- Le téléchargement du corpus tourne depuis `tools/corpus-run` : ne jamais supprimer ce dossier,
  ne rien modifier dans `data/livres/` pendant qu'il tourne.

## Pas encore décidé

- Point 5 : que faire des livres isolés dans le graphe (les laisser, ou les relier à leur
  plus proche voisin). On décide après l'histogramme sur le vrai corpus.
- Le seuil de distance et la proportion de mots vides se calibrent sur le vrai corpus,
  pas sur `data/test20`.

## Plan de demain

Dans cet ordre, quand le téléchargement est terminé :

1. Compter les livres gardés dans `data/livres/` (il en faut au moins 1 664) ; relancer le
   script s'il en manque.
2. Construire l'index sur `data/livres/` ; noter taille du fichier, temps de construction et
   temps de chargement dans `docs/notes.md`.
3. Lancer `ConstructionGraphe` ; regarder l'histogramme des distances ; lister les paires à
   distance très faible (sous 0,05) pour supprimer les doublons à la main.
4. Calibrer le seuil, puis discuter du point 5 avec le nombre réel de livres isolés.
5. Brancher PageRank dans le classement de l'API.
