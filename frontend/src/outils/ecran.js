// Largeur « téléphone » : la même valeur que le @media (max-width: 600px) de style.css.
// Si on la change, changer les deux.
const TELEPHONE = '(max-width: 600px)'

// lu au moment de l'appel (au chargement d'une page ou d'un composant), pas à chaque redimensionnement
export function surTelephone() {
  return window.matchMedia(TELEPHONE).matches
}
