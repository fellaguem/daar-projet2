import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import Suggestions from '../components/Suggestions.jsx'
import { livre as chargerLivre } from '../api.js'

// Paragraphes du début du livre, sans les illustrations.
// Une illustration Gutenberg va de « [Illustration » au « ] » qui la ferme,
// parfois sur plusieurs paragraphes. Les _..._ (italique en texte brut) sont retirés.
function paragraphesSansIllustrations(extrait) {
  const garde = []
  let dansIllustration = false
  for (const p of extrait.split('\n\n')) {
    if (!dansIllustration && p.startsWith('[Illustration')) {
      dansIllustration = !p.includes(']')
    } else if (dansIllustration) {
      if (p.includes(']')) dansIllustration = false
    } else if (p.trim()) {
      garde.push(p.replace(/_([^_]+)_/g, '$1'))
    }
  }
  return garde
}

// « Début du livre » : environ 8 lignes, puis « Afficher plus »
function DebutDuLivre({ extrait }) {
  const paragraphes = paragraphesSansIllustrations(extrait)
  const [ouvert, setOuvert] = useState(false)
  const [depasse, setDepasse] = useState(false)
  const bloc = useRef(null)

  // le bouton n'apparaît que si le texte dépasse vraiment la hauteur limite
  useLayoutEffect(() => {
    if (bloc.current) setDepasse(bloc.current.scrollHeight > bloc.current.clientHeight + 1)
  }, [extrait])

  if (paragraphes.length === 0) return null
  return (
    <section>
      <h2>Début du livre</h2>
      <div ref={bloc} className={'debut' + (ouvert ? ' ouvert' : depasse ? ' coupe' : '')}>
        {paragraphes.map((p, i) => <p key={i}>{p}</p>)}
      </div>
      {depasse && (
        <button type="button" className="plus" onClick={() => setOuvert(!ouvert)}>
          {ouvert ? 'Afficher moins' : 'Afficher plus'}
        </button>
      )}
    </section>
  )
}

// Retour à la page précédente ; sans page précédente (lien ouvert directement,
// page rechargée), on va à l'accueil au lieu de quitter le site
function Retour() {
  const location = useLocation()
  const aller = useNavigate()
  if (location.key === 'default') {
    return <Link className="retour" to="/">← Accueil</Link>
  }
  return <button type="button" className="retour" onClick={() => aller(-1)}>← Retour</button>
}

// Vue Livre : fiche du livre et ses voisins dans le graphe de Jaccard
export default function Livre() {
  const { id } = useParams()
  const [livre, setLivre] = useState(null)
  const [erreur, setErreur] = useState(null)

  useEffect(() => {
    setLivre(null)
    setErreur(null)
    chargerLivre(id).then(setLivre).catch((e) => setErreur(e.message))
  }, [id])

  return (
    <>
      <Retour />
      {erreur && <p className="erreur">{erreur}</p>}
      {!erreur && !livre && <p>Chargement…</p>}
      {livre && (
        <>
          <article className="carte fiche">
            <h1>{livre.titre}</h1>
            {livre.auteur && <p className="auteur">{livre.auteur}</p>}
            <a className="bouton-lien" href={livre.urlSource} target="_blank" rel="noreferrer">
              Lire le livre sur Project Gutenberg ↗
            </a>
            {/* la clé remet « Afficher plus » à zéro quand on passe à un autre livre */}
            <DebutDuLivre key={livre.id} extrait={livre.extrait} />
          </article>
          <Suggestions titre="Livres proches" livres={livre.voisins} />
        </>
      )}
    </>
  )
}
