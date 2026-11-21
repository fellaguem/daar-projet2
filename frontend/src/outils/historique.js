// Les 5 dernières recherches, gardées dans le navigateur (localStorage), avec leur mode.
// Sans stockage (navigation privée, stockage bloqué), l'historique est simplement vide.
import { effacerCle, ecrireJSON, lireJSON } from './stockage.js'

const CLE = 'bibliotheque.historique'
const MAX = 5

// mode : 'motcle' ou 'regex' (les mêmes valeurs que le champ « type » de l'API)
export function lireHistorique() {
  const liste = lireJSON('local', CLE, [])
  if (!Array.isArray(liste)) return []
  return liste
    .filter((r) => r && typeof r.texte === 'string' && r.texte && (r.mode === 'motcle' || r.mode === 'regex'))
    .slice(0, MAX)
}

// vide la liste (petite croix sous la barre de l'accueil)
export function effacerHistorique() {
  effacerCle('local', CLE)
}

// la recherche passe en tête ; si elle y était déjà, elle n'apparaît qu'une fois
export function ajouterHistorique(texte, mode) {
  const autres = lireHistorique().filter((r) => r.texte !== texte || r.mode !== mode)
  ecrireJSON('local', CLE, [{ texte, mode }, ...autres].slice(0, MAX))
}
