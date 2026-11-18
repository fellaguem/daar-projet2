import { Link } from 'react-router-dom'

// au-delà, le titre est coupé avec « … » (titre complet et auteur dans l'infobulle) ;
// certains titres Gutenberg dépassent 250 caractères
const LONGUEUR_MAX = 60

function couper(titre) {
  return titre.length > LONGUEUR_MAX ? titre.slice(0, LONGUEUR_MAX - 1).trimEnd() + '…' : titre
}

// Livres proches (voisins dans le graphe de Jaccard), en pastilles avec le titre seul.
// Sert pour les suggestions de la vue Résultats et pour les livres proches de la vue Livre.
export default function Suggestions({ titre = 'Suggestions', livres }) {
  if (!livres || livres.length === 0) return null
  return (
    <section className="carte suggestions">
      <h2>{titre}</h2>
      <ul className="pastilles">
        {livres.map((l) => (
          <li key={l.id}>
            <Link className="pastille" to={'/livre/' + l.id} title={l.auteur ? l.titre + ' — ' + l.auteur : l.titre}>
              {couper(l.titre)}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
