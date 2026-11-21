// Icônes du site, en trait simple (SVG), toutes décoratives (aria-hidden) :
// le nom accessible est porté par le bouton ou le lien qui les contient.

const TRAIT = { fill: 'none', stroke: 'currentColor', strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': 'true' }

// livre (logo de l'en-tête)
export function IconeLivre() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" strokeWidth="1.8" {...TRAIT}>
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
    </svg>
  )
}

// horloge (devant les dernières recherches)
export function IconeHorloge() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" strokeWidth="1.8" {...TRAIT}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </svg>
  )
}

// croix (effacer les dernières recherches)
export function IconeCroix() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" strokeWidth="2" {...TRAIT}>
      <path d="M6 6l12 12M18 6L6 18" />
    </svg>
  )
}

// flèche vers le bas (listes repliables ; tournée vers le haut par le CSS quand la liste est ouverte)
export function Chevron() {
  return (
    <svg className="chevron" width="16" height="16" viewBox="0 0 24 24" strokeWidth="2" {...TRAIT}>
      <path d="M6 9l6 6 6-6" />
    </svg>
  )
}
