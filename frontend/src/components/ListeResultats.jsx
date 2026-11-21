import { useState } from 'react'
import { Link } from 'react-router-dom'

// nombre de résultats ajoutés à chaque clic sur « Voir plus »
const PAR_PAGE = 20

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

// Liste classée des livres trouvés, affichée 20 par 20
export default function ListeResultats({ resultats }) {
  const [nbAffiches, setNbAffiches] = useState(PAR_PAGE)
  const n = resultats.length
  // meilleur score de TOUS les résultats, pas seulement des livres affichés :
  // la pertinence reste juste quand on trie par occurrences ou par PageRank
  const meilleur = resultats.reduce((max, r) => Math.max(max, r.score), 0)

  return (
    <section className="carte liste">
      <ol className="resultats">
        {resultats.slice(0, nbAffiches).map((r, i) => (
          <li key={r.id} className="resultat">
            {/* rang dans sa propre colonne, pour l'afficher dans la marge gauche */}
            <span className="rang">{i + 1}.</span>
            <div className="livre">
              {/* titre limité à 2 lignes (certains dépassent 250 caractères), complet en infobulle */}
              <div className="ligne-titre">
                <Link className="titre" to={'/livre/' + r.id} title={r.titre}>{r.titre}</Link>
              </div>
              {r.auteur && <div className="auteur">{r.auteur}</div>}
            </div>
            <div className="chiffres">
              <div>Occurrences : {r.occurrences.toLocaleString('fr-FR')}</div>
              <div>PageRank : {pageRank(r.pagerank)}</div>
              <div className="pertinence">Pertinence : <strong>{pertinence(r.score, meilleur)}</strong></div>
            </div>
          </li>
        ))}
      </ol>
      {nbAffiches < n && (
        <div className="voir-plus">
          <button type="button" onClick={() => setNbAffiches(nbAffiches + PAR_PAGE)}>
            Voir plus ({nbAffiches} / {n.toLocaleString('fr-FR')})
          </button>
        </div>
      )}
    </section>
  )
}
