import BarreRecherche from '../components/BarreRecherche.jsx'
import { surTelephone } from '../outils/ecran.js'

// curseur placé dans le champ sur ordinateur seulement : sur téléphone,
// le clavier s'ouvrirait et cacherait la page
const SUR_ORDINATEUR = !surTelephone()

// Accueil : le titre et la grande barre au centre de l'écran, sans texte explicatif.
// Les exemples et les dernières recherches sont affichés par la barre (accueil).
export default function Accueil() {
  return (
    <div className="accueil">
      <h1>Rechercher dans la bibliothèque</h1>
      <BarreRecherche accueil autoFocus={SUR_ORDINATEUR} />
    </div>
  )
}
