import { Link } from 'react-router-dom'
import { depuisResultats } from '../outils/navigation.js'

// nombre de résultats ajoutés à chaque clic sur « Voir plus »
export const PAR_PAGE = 20

// Pertinence relative : score du livre / meilleur score de la recherche, en %.
// Le score brut est trop petit pour être lu (de 1e-5 à 1e-10).
function pertinence(score, meilleur) {
  if (!(meilleur > 0)) return '—'
  const p = (score / meilleur) * 100
  return p < 1 ? '< 1 %' : Math.round(p) + ' %'
}

function pageRank(valeur) {
  return valeur.toLocaleString('fr-FR', { minimumFractionDigits: 4, maximumFractionDigits: 4 })
}

// Liste classée des livres trouvés, affichée 20 par 20.
// nbAffiches est gardé par la page Résultats : au retour d'une fiche, la liste retrouve sa longueur.
// Chaque lien transmet à la fiche la recherche et les chiffres du livre (rappel de la recherche) ;
// data-ancre sert à retrouver le lien cliqué au retour.
export default function ListeResultats({ resultats, requete, type, nbAffiches, onVoirPlus }) {
  const n = resultats.length
  // meilleur score de TOUS les résultats, pas seulement des livres affichés :
  // la pertinence reste juste quand on trie par occurrences ou par PageRank
  const meilleur = resultats.reduce((max, r) => Math.max(max, r.score), 0)

  return (
    <section className="carte liste">
      <ol className="resultats">
        {resultats.slice(0, nbAffiches).map((r, i) => {
          const chiffres = {
            occurrences: r.occurrences.toLocaleString('fr-FR'),
            pagerank: pageRank(r.pagerank),
            pertinence: pertinence(r.score, meilleur),
          }
          return (
            <li key={r.id} className="resultat">
              {/* rang dans sa propre colonne, pour l'afficher dans la marge gauche */}
              <span className="rang">{i + 1}.</span>
              <div className="livre">
                {/* titre limité à 2 lignes (certains dépassent 250 caractères), complet en infobulle */}
                <div className="ligne-titre">
                  <Link
                    className="titre"
                    to={'/livre/' + r.id}
                    title={r.titre}
                    data-ancre={'r-' + r.id}
                    state={depuisResultats(requete, type, chiffres)}
                  >
                    {r.titre}
                  </Link>
                </div>
                {r.auteur && <div className="auteur">{r.auteur}</div>}
              </div>
              <div className="chiffres">
                <div>Occurrences : {chiffres.occurrences}</div>
                <div>PageRank : {chiffres.pagerank}</div>
                <div className="pertinence">Pertinence : <strong>{chiffres.pertinence}</strong></div>
              </div>
            </li>
          )
        })}
      </ol>
      {nbAffiches < n && (
        <div className="voir-plus">
          <button type="button" onClick={onVoirPlus}>
            Voir plus ({nbAffiches} / {n.toLocaleString('fr-FR')})
          </button>
        </div>
      )}
    </section>
  )
}
