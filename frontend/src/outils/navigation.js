// État transmis à la fiche d'un livre quand on l'ouvre (le « state » de React Router).
// Il sert au libellé du bouton retour (« ← Résultats « whale » », « ← Moby-Dick… ») et au rappel
// de la recherche (la requête et les chiffres du livre).
// Il est absent quand la fiche est ouverte par un lien direct : chaque lecteur doit le prévoir.
//
// Formes possibles :
//   { depuis: 'resultats', requete, type, chiffres? }   type : 'motcle' ou 'regex'
//   { depuis: 'livre', titre }
// chiffres (seulement depuis la liste des résultats) : { occurrences, pagerank, pertinence },
// déjà mis en forme pour l'affichage.

export function depuisResultats(requete, type, chiffres) {
  return chiffres ? { depuis: 'resultats', requete, type, chiffres } : { depuis: 'resultats', requete, type }
}

export function depuisLivre(titre) {
  return { depuis: 'livre', titre }
}

export function vientDesResultats(etat) {
  return !!etat && etat.depuis === 'resultats' && typeof etat.requete === 'string'
}

export function vientDunLivre(etat) {
  return !!etat && etat.depuis === 'livre' && typeof etat.titre === 'string'
}
