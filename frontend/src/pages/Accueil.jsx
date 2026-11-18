import { Link } from 'react-router-dom'
import BarreRecherche from '../components/BarreRecherche.jsx'

// adresse de la page Résultats pour un exemple
function adresse(cle, valeur) {
  return '/resultats?' + new URLSearchParams({ [cle]: valeur }).toString()
}

// exemples cliquables : un clic lance la recherche (plus rapide que de taper, surtout sur téléphone)
const EXEMPLES = [
  { texte: 'whale', vers: adresse('q', 'whale') },
  { texte: 'white whale', vers: adresse('q', 'white whale') },
  { texte: 'wh(a|e)le', vers: adresse('regex', 'wh(a|e)le'), regex: true },
]

// curseur placé dans le champ sur ordinateur seulement : sur téléphone,
// le clavier s'ouvrirait et cacherait la page
const SUR_ORDINATEUR = window.matchMedia('(min-width: 601px)').matches

export default function Accueil() {
  return (
    <div className="accueil">
      <h1>Rechercher dans la bibliothèque</h1>
      <p className="intro">
        Près de 1&nbsp;900 livres en anglais du projet Gutenberg. Cherchez un mot-clef, ou cochez
        «&nbsp;Recherche avancée&nbsp;» pour une expression régulière. Les livres sont classés par pertinence
        (occurrences du mot et PageRank), avec des suggestions de livres proches.
      </p>
      <BarreRecherche autoFocus={SUR_ORDINATEUR} />
      <div className="exemples">
        <span>Exemples :</span>
        <ul className="pastilles">
          {EXEMPLES.map((e) => (
            <li key={e.texte}>
              <Link className={e.regex ? 'pastille regex' : 'pastille'} to={e.vers}>
                {e.texte}
              </Link>
              {e.regex && <span className="type-exemple"> (RegEx)</span>}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
