// Données factices du mode « npm run dev:mock » : le front fonctionne sans serveur.
// Mêmes formes de réponse que l'API (docs/api.md).

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

export function reponseFactice(requete, type, tri) {
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

export function livreFactice(id) {
  return {
    id: Number(id),
    titre: 'Moby-Dick; or, The Whale',
    auteur: 'Melville, Herman, 1819-1891',
    urlSource: 'https://www.gutenberg.org/ebooks/' + id,
    extrait: 'Call me Ishmael. Some years ago, never mind how long precisely…',
    voisins: SUGGESTIONS,
  }
}
