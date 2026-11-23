// Ce que partagent la barre de recherche, l'accueil et la page Résultats.

// occurrences ou pagerank
export const TRIS = [
  { valeur: 'produit', libelle: 'Pertinence' },
  { valeur: 'occurrences', libelle: 'Occurrences' },
  { valeur: 'pagerank', libelle: 'PageRank' },
]

// exemples proposés selon le mode
export const EXEMPLES = {
  motcle: ['whale', 'white whale'],
  regex: ['wh(a|e)le', '[a-z]*ology'],
}

// adresse de la page Résultats pour une recherche.
export function adresseRecherche(texte, mode) {
  const params = new URLSearchParams()
  params.set(mode === 'regex' ? 'regex' : 'q', texte)
  return '/resultats?' + params.toString()
}

// une saisie qui contient un caractère de regex : * ( ) | [ ] + ? .
export function ressembleRegex(texte) {
  return /[*()|[\]+?.]/.test(texte)
}
