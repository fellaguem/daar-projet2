# Front : React + Vite

Interface web du moteur de recherche. Spring Boot sert le build : en démo, chaque machine ouvre
`http://IP-du-serveur:8080`. Le contrat d'API est dans [`docs/api.md`](../docs/api.md).

## Commandes

Depuis `frontend/` :

| Commande | Rôle |
|---|---|
| `npm ci` | installe les dépendances (une fois) |
| `npm run dev` | serveur de développement (port 5173) ; `/api` est envoyé au serveur Spring Boot sur 8080 |
| `npm run build` | build dans `backend/src/main/resources/static` ; reconstruire ensuite le jar (`.\mvnw.cmd package`) pour qu'il le contienne |

## Organisation

```
src/
├── main.jsx          point d'entrée : HashRouter, défilement géré par les pages
├── App.jsx           en-tête et routes (/, /resultats, /livre/:id)
├── style.css         jetons (:root : couleurs, polices), puis styles par composant, puis téléphone
├── pages/            une page par route : lire l'URL, charger les données, assembler les composants
│   ├── Accueil.jsx
│   ├── resultats/
│   │   ├── Resultats.jsx    la page, puis ses morceaux (Contenu, AutresPistes, MotsTrouves)
│   │   └── place.js         place dans les résultats au retour d'une fiche (sessionStorage)
│   └── livre/
│       ├── Livre.jsx        la page, puis ses morceaux (Retour, Rappel, DebutDuLivre)
│       └── extrait.js       début du livre : illustrations retirées, page de titre repérée
├── components/       morceaux d'interface réutilisables
│   ├── BarreRecherche.jsx   sélecteur Mot-clef | RegEx, champ, bouton (+ exemples et récentes sur l'accueil)
│   ├── ListeResultats.jsx   liste classée, « Voir plus »
│   ├── Suggestions.jsx      livres proches (pastilles, ou liste titre + auteur)
│   ├── Repliable.jsx        liste repliable (vrai bouton, aria-expanded)
│   └── icones.jsx           icônes SVG décoratives
├── api/
│   └── client.js     appels HTTP ; une erreur porte le message du serveur et son statut (400, 404)
└── outils/           fonctions sans React, partagées ou génériques
    │                 propres au projet, utilisées par plusieurs fichiers :
    ├── recherches.js exemples, tris, adresse d'une recherche, détection d'une regex
    ├── navigation.js état transmis à la fiche d'un livre (voir plus bas)
    ├── auteur.js     « Melville, Herman, 1819-1891 » → « Herman Melville (1819–1891) »
    ├── historique.js 5 dernières recherches (localStorage)
    │                 génériques, copiables dans un autre projet :
    ├── texte.js      couper un texte trop long
    ├── stockage.js   lecture et écriture protégées du stockage du navigateur
    └── ecran.js      largeur « téléphone » (600px)
```

Où ranger quoi :

- une page = un fichier, dans le même ordre : imports, constantes, `// ---------- la page ----------`
  (le composant exporté), puis `// ---------- morceaux de la page ----------` (ses petits composants) ;
- la logique sans React d'une seule page va dans un fichier `.js` à côté, dans le dossier de la page
  (`pages/resultats/place.js`) ;
- une fonction sans React utilisée par plusieurs fichiers, ou générique, va dans `outils/` ;
- un appel au serveur va dans `api/client.js` ;
- un morceau d'interface utilisé par plusieurs pages va dans `components/`.

## Règles

- Code, commentaires et noms en français, comme le reste du projet.
- Aucune ressource externe (polices, CDN) : la démo doit marcher sur un réseau local sans Internet.
  Les polices sont des polices système (Sitka et Bahnschrift sous Windows).
- Couleurs et polices : seulement les variables de `:root` dans `style.css`.
- Largeur téléphone : 600px, écrite à deux endroits (`outils/ecran.js` et le `@media` de `style.css`).
- Accessibilité : de vrais boutons et liens (pas de `div` cliquable), focus visible, cibles de 44px
  sur téléphone, contraste d'au moins 4,5:1.
- Pas de texte explicatif pour dire quoi faire : l'interface doit se comprendre seule.

## Contrats internes

**État transmis à la fiche d'un livre** (le `state` de React Router), construit et lu seulement par
les fonctions de `outils/navigation.js` :

```
{ depuis: 'resultats', requete, type, chiffres? }   depuis la page Résultats
{ depuis: 'livre', titre }                          depuis un livre proche
```

Il donne le libellé du bouton retour et le rappel de la recherche. Il est absent quand la fiche est
ouverte par un lien direct.

**Stockage du navigateur** : `bibliotheque.historique` (localStorage, les 5 dernières recherches qui
ont donné au moins un résultat, `outils/historique.js`) et `bibliotheque.places` (sessionStorage, la
place dans les résultats pour les 20 dernières recherches, `pages/resultats/place.js`).

**Défilement** : `history.scrollRestoration` est en `manual` (`main.jsx`). La fiche d'un livre
remonte en haut ; la page Résultats retrouve sa place au retour (navigation « POP »).
