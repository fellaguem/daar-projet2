// Appels à l'API du serveur (contrat : docs/api.md). Avec VITE_DONNEES_FACTICES=true
// (npm run dev:mock), on répond sans serveur avec les données de factices.js.
import { livreFactice, reponseFactice } from './factices.js'

const FACTICES = import.meta.env.VITE_DONNEES_FACTICES === 'true'

// chemin + paramètres non vides, encodés
function adresse(chemin, params) {
  const p = new URLSearchParams()
  for (const [cle, valeur] of Object.entries(params)) {
    if (valeur) p.set(cle, valeur)
  }
  return chemin + '?' + p.toString()
}

async function appeler(url) {
  let rep
  try {
    rep = await fetch(url)
  } catch {
    throw new Error('Serveur injoignable')
  }
  if (!rep.ok) {
    // le serveur explique l'erreur dans { "erreur": "..." } (400 ou 404)
    const corps = await rep.json().catch(() => null)
    const erreur = new Error(corps && corps.erreur ? corps.erreur : 'Erreur ' + rep.status)
    // statut : 400 = requête refusée (par exemple une regex invalide), 404 = livre inconnu
    erreur.statut = rep.status
    throw erreur
  }
  return rep.json()
}

// tri = produit (pertinence, par défaut), occurrences ou pagerank
export async function rechercher(q, tri) {
  if (FACTICES) return reponseFactice(q, 'motcle', tri)
  return appeler(adresse('/api/search', { q, tri }))
}

// regex testée sur les mots de l'index (mode=index, celui par défaut du serveur)
export async function rechercherRegex(regex, tri) {
  if (FACTICES) return reponseFactice(regex, 'regex', tri)
  return appeler(adresse('/api/search/advanced', { regex, tri }))
}

export async function livre(id) {
  if (FACTICES) return livreFactice(id)
  return appeler('/api/books/' + id)
}
