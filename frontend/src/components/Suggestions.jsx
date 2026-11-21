import { Link } from 'react-router-dom'
import Repliable from './Repliable.jsx'
import { formaterAuteur } from '../outils/auteur.js'
import { couper } from '../outils/texte.js'

// au-delà, le titre d'une pastille est coupé avec « … » (titre complet et auteur dans l'infobulle) ;
// certains titres Gutenberg dépassent 250 caractères
const LONGUEUR_MAX = 60

// Livres proches (voisins dans le graphe de Jaccard).
// Sert pour les suggestions de la vue Résultats (pastilles avec le titre seul) et pour les livres
// proches de la vue Livre (liste : titre et auteur, pour reconnaître un voisin par son auteur).
// Repliable : le titre donne le nombre de livres ; la ligne d'explication s'affiche avec la liste.
// etat : transmis à la fiche ouverte (d'où l'on vient, voir outils/navigation.js) ;
// data-ancre sert à retrouver le lien cliqué au retour sur la page Résultats.
export default function Suggestions({ titre = 'Suggestions', sousTitre, livres, liste = false, ouvert, etat }) {
  if (!livres || livres.length === 0) return null
  return (
    <section className="carte suggestions">
      <Repliable titreH2 titre={titre + ' (' + livres.length + ')'} ouvertParDefaut={ouvert}>
        {sousTitre && <p className="sous-titre">{sousTitre}</p>}
        {liste ? (
          <ul className="voisins">
            {livres.map((l) => (
              <li key={l.id}>
                <Link className="voisin" to={'/livre/' + l.id} state={etat} data-ancre={'s-' + l.id} title={l.titre}>
                  <span className="voisin-titre">{l.titre}</span>
                  {l.auteur && <span className="voisin-auteur">{formaterAuteur(l.auteur, false)}</span>}
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <ul className="pastilles">
            {livres.map((l) => (
              <li key={l.id}>
                <Link
                  className="pastille"
                  to={'/livre/' + l.id}
                  state={etat}
                  data-ancre={'s-' + l.id}
                  title={l.auteur ? l.titre + ' — ' + l.auteur : l.titre}
                >
                  {couper(l.titre, LONGUEUR_MAX)}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Repliable>
    </section>
  )
}
