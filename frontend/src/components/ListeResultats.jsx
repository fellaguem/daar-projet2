import { Link } from 'react-router-dom'

export default function ListeResultats({ resultats }) {
  if (resultats.length === 0) return <p>Aucun livre trouvé.</p>
  return (
    <ol className="resultats">
      {resultats.map((r) => (
        <li key={r.id}>
          <Link to={'/livre/' + r.id}>{r.titre}</Link>
          <span className="auteur"> — {r.auteur}</span>
          <div className="details">
            {r.occurrences} occurrences · PageRank {r.pagerank}
          </div>
        </li>
      ))}
    </ol>
  )
}
