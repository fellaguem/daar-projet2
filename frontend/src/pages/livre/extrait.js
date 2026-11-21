// Traitement du « Début du livre » (champ extrait de l'API) avant affichage.

// Paragraphes du début du livre, sans les illustrations.
// Une illustration Gutenberg va de « [Illustration » au « ] » qui la ferme,
// parfois sur plusieurs paragraphes. Les _..._ (italique en texte brut) sont retirés.
export function paragraphesSansIllustrations(extrait) {
  const garde = []
  let dansIllustration = false
  for (const p of extrait.split('\n\n')) {
    if (!dansIllustration && p.startsWith('[Illustration')) {
      dansIllustration = !p.includes(']')
    } else if (dansIllustration) {
      if (p.includes(']')) dansIllustration = false
    } else if (p.trim()) {
      garde.push(p.replace(/_([^_]+)_/g, '$1'))
    }
  }
  return garde
}

// un paragraphe tout en majuscules (page de titre, « ETYMOLOGY. ») : au moins deux capitales de suite
export function enCapitales(p) {
  return /[A-Z]{2}/.test(p) && p === p.toUpperCase()
}
