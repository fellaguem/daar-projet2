# Notes pour le rapport

Choix et chiffres à reprendre dans le rapport. On complète au fil de l'eau.

## Choix de conception

### Langue du corpus : anglais seulement (décidé le 12/11/2026)

Tous les livres de la bibliothèque sont en anglais.

Raison : le graphe de Jaccard compare les ensembles de mots de deux livres.
Deux livres de langues différentes n'ont presque aucun mot en commun, donc leur
distance de Jaccard est proche de 1. Avec un corpus multilingue, le graphe se
découperait en îlots, un par langue : les voisins et le PageRank refléteraient
la langue du livre et non son contenu. Les suggestions seraient alors
trompeuses.

Mise en œuvre : le catalogue officiel de Project Gutenberg indique la langue de
chaque livre. Le script de téléchargement ne garde que les livres dont la
langue est `en`. On le dit dans le rapport avec la provenance des fichiers.

### Recherche par mot-clef : recherche exacte dans l'index

L'énoncé demande les livres dont la table d'indexage contient la chaîne S. C'est
une recherche dans l'index (mot exact), pas un balayage de texte : KMP n'est
pas utilisé ici. KMP reste utile pour l'extrait d'un livre et pour la variante
« texte complet » de la recherche avancée.

### Classement : occurrences normalisées × PageRank (point de départ)

- Occurrences normalisées : nombre d'occurrences du mot divisé par la longueur
  du livre. Sinon les gros livres gagnent toujours.
- PageRank : importance du livre dans le graphe de Jaccard, calculée une fois
  hors ligne, indépendante de la requête.
- Le produit des deux donne le score. À comparer dans le rapport avec
  « occurrences seules » et « PageRank seul ».

### Suggestions

Voisins dans le graphe de Jaccard des 2-3 meilleurs résultats, sans les
doublons avec la liste des résultats. Bonus possible plus tard : classer ces
voisins par nombre de clics (stratégie citée dans l'énoncé).

## Questions encore ouvertes

- Mots vides : comment les traiter (piste : les garder dans l'index mais les
  ignorer dans le classement).
- Regex : mot entier ou sous-chaîne ? Traducteur pour `+ ? [ ]` ?
- Build du front dans l'archive de 30 Mo : commité ou reconstruit ?

## Chiffres et mesures

(rien pour l'instant)

## Environnement de développement

- JDK 17 : Temurin 17.0.21 (installé avec winget).
- Maven : 3.9.9, fourni par le Maven Wrapper (`backend/mvnw`), rien à installer.
- Node : v26, npm 11.
- Le projet 1 (packages parser, ast, automate, kmp, experimentation) est copié
  tel quel dans `backend/src/main/java` ; il compile avec Java 17.
- Le build du front va dans `backend/src/main/resources/static`, ignoré par git
  (régénéré par `npm run build`). À décider : l'inclure ou non dans l'archive.
