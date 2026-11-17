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
- Regex en **mot entier** par défaut (`motEntier=true`), sous-chaîne avec `motEntier=false`.
- Livres isolés : le graphe reste tel quel ; les suggestions prennent les 3 premiers résultats qui ont
  des voisins, parmi les 10 premiers (fait le 13/11).
- Démarrage à chaud : `Moteur.chauffer()` et `api.ChauffeHttp` (fait le 13/11).
- Contrat complet des ajouts écrit dans `docs/api.md` (13/11), avec les choix validés :
  - suggestions : tous les voisins des sources, les 10 qui ont le plus de clics, à égalité les plus
    proches ; champ `clics` sur chaque suggestion ;
  - repli OU : les mots vides ne comptent pas dans le OU (sauf requête de mots vides seulement) ;
  - BM25 : idf = ln(1 + (N - df + 0,5) / (df + 0,5)), k1 = 1,2, b = 0,75 ;
  - passages surlignés : texte mis en minuscules lettre par lettre (`Character.toLowerCase`), KMP ;
  - `clics.tsv` écrit toutes les 30 s et à l'arrêt du serveur.

## Planning des ajouts

- Samedi 14 : BM25 (`tri=bm25`, champ `bm25`), repli OU (`repli`, `motsPresents`).
- Dimanche 15 : « pourquoi ce résultat » (`occurrencesUtiles`, `longueur`, `motsVides`), passages
  surlignés, clics.

## Pistes ouvertes

- Pour les courbes du 16/11 : il faut pouvoir construire index et graphe sur une partie du corpus
  (100, 400, 800 livres), et chauffer la JVM plus longtemps que 3 appels (voir notes.md).
- Corrections du planning : `.gitignore` en UTF-8, README, répartition dans `CLAUDE.md`.

## Reconstruire les données

```
java -cp backend/target/classes corpus.TelechargeCorpus --max 1900 --pause 1500
java -cp backend/target/classes index.ConstructionIndex
java -cp backend/target/classes corpus.RetraitDoublons --appliquer
java -cp backend/target/classes index.ConstructionIndex
java -Xmx2g -cp backend/target/classes graphe.ConstructionGraphe --dossier data
```
