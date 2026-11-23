 # Moteur de recherche d'une bibliothèque — DAAR, projet 2

Moustafa FELLAGUE et Younes MANSOUS, Master 2 STL, Sorbonne Université.

Application web de recherche dans 1 893 livres anglais de Project Gutenberg : recherche par mot-clef et
par expression régulière dans un index inversé, classement avec le PageRank du graphe de Jaccard,
suggestions de livres proches. Serveur Java 17 + Spring Boot, front React + Vite, sans base de données
ni bibliothèque de recherche.


Application web pour chercher des livres parmi 1 893 livres anglais de Project Gutenberg.

- Recherche par mot-clef
- Recherche par expression régulière (RegEx)
- Résultats classés avec le PageRank
- Suggestions de livres proches
- Serveur en Java 17 + Spring Boot, front en React + Vite
- Pas de base de données

Le corpus n'est pas fourni : les commandes ci-dessous le téléchargent et construisent les données
dans `data/`.

## Prérequis

- JDK 17. Maven n'est pas à installer : le wrapper `backend/mvnw` le télécharge.
- Node.js et npm, pour le front.
- Python 3 avec matplotlib, seulement pour les courbes.
- Environ 1 Go de disque pour le corpus et 2 Go de mémoire pour le serveur.

Les commandes se lancent depuis la racine du dépôt, dans PowerShell sous Windows.
Sous Linux ou macOS, remplacer `.\mvnw.cmd` par `./mvnw`.

## 1. Construire le front et le jar

```
cd frontend
npm ci
npm run build
cd ../backend
.\mvnw.cmd clean package
cd ..
```

Ces commandes fabriquent l'application :

- `npm run build` prépare le site (le front) ;
- `mvnw clean package` compile le serveur, lance les tests et crée le fichier
  `backend/target/moteur-recherche-0.1.0.jar`, qui contient tout : le serveur et le site.

L'ordre compte : il faut préparer le site avant le jar, sinon le jar ne le contient pas.

## 2. Télécharger le corpus (une fois, environ 4 h)

```
java -cp backend/target/classes corpus.TelechargeCorpus --max 1900 --pause 1500

```


Le téléchargement dure environ 4 h ; on peut l'arrêter et le relancer, il reprend là où il s'était arrêté.
Les livres arrivent dans `data/livres/`, et leur liste (titre, auteur, lien Gutenberg) dans `data/metadonnees.tsv`.

## 3. Construire l'index et le graphe (une fois, quelques minutes)

```

java -cp backend/target/classes index.ConstructionIndex
java -cp backend/target/classes corpus.RetraitDoublons --appliquer
java -cp backend/target/classes index.ConstructionIndex
java -Xmx2g -cp backend/target/classes graphe.ConstructionGraphe --dossier data
```

1. On construit l'index des mots.
2. On retire les livres en double (ils sont mis de côté dans `data/doublons/`, pas supprimés).
3. On reconstruit l'index sans ces doublons.
4. On construit le graphe de Jaccard et on calcule le PageRank.



## 4. Lancer le serveur


```
java -Xmx2g -jar backend/target/moteur-recherche-0.1.0.jar
```

Attendre la ligne Serveur chauffé, puis ouvrir http://localhost:8080


## 5. Démo sur plusieurs machines

Depuis une autre machine du même réseau, ouvrir `http://IP-du-serveur:8080` (adresse donnée par `ipconfig`).

## 6. Tests

```
cd backend; .\mvnw.cmd test; cd ..
```

Quelques tests JUnit sur les parties principales : index, recherche par mot-clef et par regex,
distance de Jaccard et PageRank. Pas besoin du corpus pour les lancer.

## 7. Mesures et courbes

Serveur arrêté, pour des temps plus stables :

```
java -Xmx4g -cp backend/target/classes recherche.MesureCorpus --dossier data --sortie docs/mesures
pip install matplotlib
python docs/mesures/courbes.py
```

- `MesureCorpus` : temps de construction et de recherche, de 100 à 1 893 livres (environ 7 min).
- `courbes.py` : courbes du rapport, dans `docs/mesures/figures/`.

## Documentation

- `rapport.pdf` : le rapport.
- `docs/api.md` : le contrat d'API.

## Code

Serveur (`backend/src/main/java`) :
- `corpus` : téléchargement et nettoyage des livres
- `index` : découpage en mots et index inversé
- `graphe` : distance de Jaccard, graphe et PageRank
- `recherche` : mot-clef, regex, classement et suggestions
- `api` : routes REST
- `parser`, `ast`, `automate`, `kmp`, `experimentation` : repris du projet 1

Front (`frontend/src`) : `pages` (Accueil, Résultats, Livre), `components`, `api.js`.
