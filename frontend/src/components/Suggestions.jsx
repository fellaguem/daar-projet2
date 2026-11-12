import { Link } from 'react-router-dom'

// Livres proches (voisins dans le graphe de Jaccard)
export default function Suggestions({ titre = 'Suggestions', livres }) {
  if (!livres || livres.length === 0) return null
  return (
    <section>
      <h2>{titre}</h2>
      <ul>
        {livres.map((l) => (
          <li key={l.id}>
            <Link to={'/livre/' + l.id}>{l.titre}</Link>
            <span className="auteur"> — {l.auteur}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
