// État transmis à la fiche d'un livre quand on l'ouvr
// Il sert au libellé du bouton retour
// de la recherche (la requête et les chiffres du livre).
// Il est absent quand la fiche est ouverte par un lien direct : chaque lecteur doit le prévoir.

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
