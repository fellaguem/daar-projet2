// Place dans la page Résultats, pour chaque recherche : défilement, nombre de résultats affichés
// (« Voir plus ») et lien cliqué. Gardée le temps de l'onglet (sessionStorage) : au retour d'une
// fiche, la liste la retrouve. Sans stockage, le retour repart du haut.
import { ecrireJSON, lireJSON } from '../../outils/stockage.js'

const CLE = 'bibliotheque.places'
const MAX = 20

function lireTout() {
  const tout = lireJSON('session', CLE, {})
  return tout && typeof tout === 'object' && !Array.isArray(tout) ? tout : {}
}

// recherche : la clé de la recherche (type:requête:tri) ; place : { y, n, ancre } ou null
export function lirePlace(recherche) {
  const place = lireTout()[recherche]
  return place && typeof place.y === 'number' && typeof place.n === 'number' ? place : null
}

// la plus récente en dernier ; on ne garde que les MAX dernières recherches
export function sauverPlace(recherche, place) {
  const tout = lireTout()
  delete tout[recherche]
  tout[recherche] = place
  const cles = Object.keys(tout)
  for (const c of cles.slice(0, Math.max(0, cles.length - MAX))) delete tout[c]
  ecrireJSON('session', CLE, tout)
}
