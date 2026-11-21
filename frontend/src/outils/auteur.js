// Auteur au format du catalogue Gutenberg (« Melville, Herman, 1819-1891 ») rendu lisible :
// « Herman Melville (1819–1891) ». Plusieurs auteurs sont séparés par « ; » dans le catalogue.
// Un format inattendu (« Various », un titre comme « Sir ») est rendu tel quel.
const FORMAT = /^([^,]+), ([^,]+?)(?:, (\d{1,4})?-(\d{1,4})?)?$/

function unAuteur(texte, avecDates) {
  const m = texte.trim().match(FORMAT)
  if (!m) return null
  // « A. C. (Albert Charles) » devient « A. C. »
  const prenoms = m[2].replace(/\s*\([^)]*\)/g, '').trim()
  const nom = prenoms + ' ' + m[1].trim()
  if (!avecDates || (!m[3] && !m[4])) return nom
  return nom + ' (' + (m[3] || '') + '–' + (m[4] || '') + ')'
}

// Les contributeurs avec un rôle (« [Editor] », « [Illustrator] », « [Author of introduction, etc.] »)
// sont retirés, sauf s'il n'y a qu'eux. Une partie au format inattendu est gardée telle quelle.
export function formaterAuteur(auteur, avecDates = true) {
  const parties = auteur.split(';').map((a) => a.trim()).filter(Boolean)
  const principaux = parties.filter((a) => !/\[[^\]]*\]$/.test(a))
  const gardes = principaux.length > 0 ? principaux : parties
  return gardes.map((a) => unAuteur(a, avecDates) || a).join(', ')
}
