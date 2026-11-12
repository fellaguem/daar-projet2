import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Suggestions from '../components/Suggestions.jsx'
import { livre as chargerLivre } from '../api.js'

export default function Livre() {
  const { id } = useParams()
  const [livre, setLivre] = useState(null)
  const [erreur, setErreur] = useState(null)

  useEffect(() => {
    setLivre(null)
    setErreur(null)
    chargerLivre(id).then(setLivre).catch((e) => setErreur(e.message))
  }, [id])

  if (erreur) return <p className="erreur">{erreur}</p>
  if (!livre) return <p>Chargement…</p>

  return (
    <>
      <h1>{livre.titre}</h1>
      <p className="auteur">{livre.auteur}</p>
      <p>
        <a href={livre.urlSource} target="_blank" rel="noreferrer">Source Gutenberg</a>
      </p>
      <blockquote>{livre.extrait}</blockquote>
      <Suggestions titre="Livres proches" livres={livre.voisins} />
    </>
  )
}
