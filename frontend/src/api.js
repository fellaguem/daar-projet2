// Appels à l'API. Avec VITE_DONNEES_FACTICES=true (npm run dev:mock),
// on répond sans serveur avec des données factices.
const FACTICES = import.meta.env.VITE_DONNEES_FACTICES === 'true'

// tri = produit (pertinence, par défaut), occurrences ou pagerank
export const TRIS = [
  { valeur: 'produit', libelle: 'Pertinence' },
  { valeur: 'occurrences', libelle: 'Occurrences' },
  { valeur: 'pagerank', libelle: 'PageRank' },
]

const SUGGESTIONS = [
  { id: 2694, titre: 'Redburn', auteur: 'Herman Melville' },
  { id: 10712, titre: 'The Cruise of the Cachalot', auteur: 'Frank T. Bullen' },
  {
    id: 1,
    titre: 'A New Voyage to Carolina Containing the exact description and natural history of that country',
    auteur: 'Lawson, John, 1674-1711',
  },
]

const RESULTATS = [
  { id: 15, titre: 'Moby-Dick; or, The Whale', auteur: 'Melville, Herman, 1819-1891', occurrences: 1244, pagerank: 0.00118, score: 7.0e-6 },
  { id: 10712, titre: 'The Cruise of the "Cachalot" Round the World After Sperm Whales', auteur: 'Bullen, Frank Thomas, 1857-1915', occurrences: 342, pagerank: 0.00141, score: 4.3e-6 },
  { id: 1, titre: 'A New Voyage to Carolina Containing the exact description and natural history of that country; together with the present state thereof', auteur: 'Lawson, John, 1674-1711', occurrences: 22, pagerank: 0.00092, score: 4.1e-7 },
  { id: 2489, titre: 'Typee', auteur: 'Melville, Herman, 1819-1891', occurrences: 1, pagerank: 0.00009, score: 3.3e-10 },
]

function reponseFactice(requete, type, tri) {
  // pour une regex, l'API ajoute nbMotsTrouves et motsTrouves
  const regex = type === 'regex' ? { nbMotsTrouves: 3, motsTrouves: ['whale', 'whaleboat', 'whaler'] } : {}
  const cle = tri === 'occurrences' ? 'occurrences' : tri === 'pagerank' ? 'pagerank' : 'score'
  return {
    ...regex,
    requete,
    type,
    tempsMs: 12,
    resultats: [...RESULTATS].sort((a, b) => b[cle] - a[cle]),
    suggestions: SUGGESTIONS,
  }
}

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
    throw new Error(corps && corps.erreur ? corps.erreur : 'Erreur ' + rep.status)
  }
  return rep.json()
}

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
  if (FACTICES) {
    return {
      id: Number(id),
      titre: 'Moby-Dick; or, The Whale',
      auteur: 'Melville, Herman, 1819-1891',
      urlSource: 'https://www.gutenberg.org/ebooks/' + id,
      extrait: 'Call me Ishmael. Some years ago, never mind how long precisely…',
      voisins: SUGGESTIONS,
    }
  }
  return appeler('/api/books/' + id)
}
