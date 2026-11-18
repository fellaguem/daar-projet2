# Reprise du travail

À lire au début de chaque session. Mis à jour le 18/11/2026 (front adapté à la maquette).

Gel des fonctionnalités : jeudi 19/11 à midi. Rendu : dimanche 22/11, 23h59.

## État

- **Toutes les exigences de l'énoncé sont faites** : recherche par mot-clef, recherche avancée par
  regex (mode index), classement avec PageRank, suggestions (voisins de Jaccard), 118 tests JUnit verts.
- **Front fait** (18/11) : adapté à la maquette de la vue Résultats (`docs/maquette/`) : pertinence en %,
  menu de tri, « Voir plus » par 20, suggestions avant la liste, mots trouvés, messages d'erreur, version
  téléphone. Choix et requêtes testées dans `docs/notes.md` (« Front adapté à la maquette »).
- Démo : essai réussi depuis un deuxième PC le 17/11 ; 20 recherches simultanées OK (depuis la machine
  serveur). Le pare-feu Windows autorisait déjà Java, aucun port à ouvrir.
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
- Pertinence affichée en % du meilleur score de la recherche (le score brut est illisible) (18/11).
- **Ajouts abandonnés (18/11), car ils ne sont pas demandés par l'énoncé** : BM25, repli OU,
  « pourquoi ce résultat », passages surlignés, clics. Leur contrat reste dans `docs/api.md`, marqué
  « abandonné (hors énoncé) » ; rien n'a été codé.

## Reste à faire

- Test sur téléphone (même réseau, ou partage de connexion si le wifi isole les appareils).
- Mesures sur sous-corpus (100, 400, 800, 1 893 livres) : construire index et graphe sur les N premiers
  livres, mesurer les temps (moyenne ± écart type) pour les courbes, chauffer la JVM plus longtemps que
  3 appels (voir notes.md).
- README avec les commandes.
- Rapport (12 pages environ).
- Vidéo de 5 min avec la démo sur 2 machines.
- Archive `daar-projet2-FELLAGUE-MANSOUS.zip` de moins de 30 Mo, sans le corpus.

## Reconstruire les données

```
java -cp backend/target/classes corpus.TelechargeCorpus --max 1900 --pause 1500
java -cp backend/target/classes index.ConstructionIndex
java -cp backend/target/classes corpus.RetraitDoublons --appliquer
java -cp backend/target/classes index.ConstructionIndex
java -Xmx2g -cp backend/target/classes graphe.ConstructionGraphe --dossier data
```
