// Appels à l'API. Avec VITE_DONNEES_FACTICES=true (npm run dev:mock),
// on répond sans serveur avec des données factices.
const FACTICES = import.meta.env.VITE_DONNEES_FACTICES === 'true'

const SUGGESTIONS = [
  { id: 2694, titre: 'Redburn', auteur: 'Herman Melville' },
  { id: 10712, titre: 'The Cruise of the Cachalot', auteur: 'Frank T. Bullen' },
]

function reponseFactice(requete, type) {
  return {
    requete,
    type,
    tempsMs: 12,
    resultats: [
      { id: 2701, titre: 'Moby Dick', auteur: 'Herman Melville', occurrences: 1226, pagerank: 0.0031 },
      { id: 15, titre: 'Moby Dick; Or, The Whale', auteur: 'Herman Melville', occurrences: 980, pagerank: 0.0027 },
      { id: 2489, titre: 'Typee', auteur: 'Herman Melville', occurrences: 41, pagerank: 0.0019 },
    ],
    suggestions: SUGGESTIONS,
  }
}

async function appeler(url) {
  const rep = await fetch(url)
  if (!rep.ok) throw new Error('Erreur ' + rep.status)
  return rep.json()
}

export async function rechercher(q) {
  if (FACTICES) return reponseFactice(q, 'motcle')
  return appeler('/api/search?q=' + encodeURIComponent(q))
}

export async function rechercherRegex(regex, mode = 'index') {
  if (FACTICES) return reponseFactice(regex, 'regex')
  return appeler('/api/search/advanced?regex=' + encodeURIComponent(regex) + '&mode=' + mode)
}

export async function livre(id) {
  if (FACTICES) {
    return {
      id: Number(id),
      titre: 'Moby Dick',
      auteur: 'Herman Melville',
      urlSource: 'https://www.gutenberg.org/ebooks/' + id,
      extrait: 'Call me Ishmael. Some years ago, never mind how long precisely…',
      voisins: SUGGESTIONS,
    }
  }
  return appeler('/api/books/' + id)
}
