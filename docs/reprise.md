# Reprise du travail

À lire au début de chaque session. Mis à jour le 13/11/2026 (après la construction sur le vrai corpus).

## État

- Corpus téléchargé (fin le 13/11 à 05:06) : 1 900 livres, puis 7 doublons retirés
  (`corpus.RetraitDoublons`, fichiers déplacés dans `data/doublons/`) : **1 893 livres**.
- `data/` contient l'index, le graphe et le PageRank du vrai corpus. Le serveur démarre dessus
  (`--moteur.dossier=data`, par défaut) en 7 s, 300 Mo. Chiffres dans `docs/notes.md`.
- Le téléchargement est terminé : `data/livres/` peut être lu, mais on n'y touche qu'avec
  `RetraitDoublons` (qui déplace, ne supprime pas).

## Décisions prises

- Corpus : anglais seulement. Filtres de doublons (titre + auteur) et de contenu
  (Factbook, Census, Thesaurus, Dictionary, Gazetteer) avant le comptage de `--max`.
  Doublons à titres différents retirés ensuite par Jaccard (distance < 0,13 sur tous les mots,
  on garde le plus petit identifiant).
- Jaccard (point 5, décidé le 13/11) : chaque livre est représenté par son **empreinte**, ses
  250 mots non vides les plus fréquents (`--mots-par-livre 250`). Sans cela, le graphe dépendait
  surtout de la longueur des livres (1 133 isolés, voisins de Moby-Dick = Dickens). Mots vides :
  plus de 50 % des livres. Seuil déduit d'un degré moyen de 10 (0,9154). Il reste 270 livres
  isolés (14 %) : gardés tels quels pour l'instant.
- PageRank : version classique, 200 itérations au plus. Version pondérée par la similarité en
  bonus si on a le temps, avec comparaison au degré pour le rapport.
- Mots vides : gardés dans l'index, ignorés dans le classement.
- Mode `texte` de la regex : seulement pour les mesures (benchmark), désactivé dans le front.

## Pas encore décidé

- Relier les 270 livres isolés à leur plus proche voisin (option possible, pas urgente).
- Contrat d'API des bonus (BM25, repli OU, clics, extrait surligné) à écrire dans `docs/api.md`
  avant que Younes construise les pages.

## Pistes ouvertes

- Regex sur le vrai index : 759 ms au premier appel pour `wh(a|e)le` (416 000 mots) ; à mesurer
  à chaud avant de conclure.
- Pour les courbes du 16/11 : il faut pouvoir construire index et graphe sur une partie du corpus
  (100, 400, 800 livres).
- Corrections du planning : `.gitignore` en UTF-8, README, répartition dans `CLAUDE.md`.

## Reconstruire les données

```
java -cp backend/target/classes corpus.TelechargeCorpus --max 1900 --pause 1500
java -cp backend/target/classes index.ConstructionIndex
java -cp backend/target/classes corpus.RetraitDoublons --appliquer
java -cp backend/target/classes index.ConstructionIndex
java -Xmx2g -cp backend/target/classes graphe.ConstructionGraphe --dossier data
```
