import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import Suggestions from '../../components/Suggestions.jsx'
import { livre as chargerLivre } from '../../api/client.js'
import { formaterAuteur } from '../../outils/auteur.js'
import { depuisLivre, vientDesResultats, vientDunLivre } from '../../outils/navigation.js'
import { couper } from '../../outils/texte.js'
import { enCapitales, paragraphesSansIllustrations } from './extrait.js'

// ---------- la page ----------

// Vue Livre : fiche du livre, puis ses voisins dans le graphe de Jaccard
export default function Livre() {
  const { id } = useParams()
  const location = useLocation()
  const [livre, setLivre] = useState(null)
  const [erreur, setErreur] = useState(null)
  const titre = useRef(null)

  useEffect(() => {
    // nouveau livre (par exemple un livre proche cliqué en bas de page) : on remonte en haut
    window.scrollTo(0, 0)
    setLivre(null)
    setErreur(null)
    chargerLivre(id).then(setLivre).catch((e) => setErreur(e.message))
  }, [id])

  // focus clavier sur le titre : un lecteur d'écran annonce le livre ouvert
  useEffect(() => {
    if (livre && titre.current) titre.current.focus({ preventScroll: true })
  }, [livre])

  return (
    <>
      <Retour />
      {erreur && <p className="erreur">{erreur}</p>}
      {!erreur && !livre && <p>Chargement…</p>}
      {livre && (
        <div className="page-livre">
          <article className="carte fiche">
            <h1 ref={titre} tabIndex={-1}>{livre.titre}</h1>
            {livre.auteur && <p className="auteur">{formaterAuteur(livre.auteur)}</p>}
            <Rappel etat={location.state} />
            <a className="bouton-lien" href={livre.urlSource} target="_blank" rel="noreferrer">
              Lire le livre sur Project Gutenberg ↗
            </a>
            {/* la clé remet « Afficher plus » à zéro quand on passe à un autre livre */}
            <DebutDuLivre key={livre.id} extrait={livre.extrait} urlSource={livre.urlSource} />
          </article>
          <Suggestions
            titre="Livres proches"
            sousTitre="Voisins de ce livre dans le graphe de Jaccard (livres au vocabulaire proche)"
            livres={livre.voisins}
            liste
            ouvert
            etat={depuisLivre(livre.titre)}
          />
        </div>
      )}
    </>
  )
}

// ---------- morceaux de la page ----------

// Retour à la page précédente, avec un libellé qui dit où il mène :
// « ← Résultats « whale » » depuis une recherche, « ← Moby-Dick… » depuis un livre proche.
// Sans page précédente (lien ouvert directement), on va à l'accueil au lieu de quitter le site.
function Retour() {
  const location = useLocation()
  const aller = useNavigate()
  const etat = location.state
  if (location.key === 'default') {
    return <Link className="retour" to="/">← Accueil</Link>
  }
  let libelle = '← Retour'
  if (vientDesResultats(etat)) {
    libelle = '← Résultats « ' + couper(etat.requete, 40) + ' »'
  } else if (vientDunLivre(etat)) {
    libelle = '← ' + couper(etat.titre, 50)
  }
  return <button type="button" className="retour" onClick={() => aller(-1)}>{libelle}</button>
}

// Rappel de la recherche quand on arrive d'un résultat : la requête et les chiffres du livre,
// les mêmes que dans la liste. Rien quand on arrive d'ailleurs (lien direct, livre proche).
function Rappel({ etat }) {
  if (!vientDesResultats(etat) || !etat.chiffres) return null
  const c = etat.chiffres
  return (
    <div className="rappel">
      <span className={etat.type === 'regex' ? 'pastille requete regex' : 'pastille requete'}>{etat.requete}</span>
      <span>Occurrences : {c.occurrences}</span>
      <span>PageRank : {c.pagerank}</span>
      <span>Pertinence : <strong>{c.pertinence}</strong></span>
    </div>
  )
}

// « Début du livre » : environ 8 lignes, puis « Afficher plus ».
// Les paragraphes en majuscules sont en petites capitales centrées, comme une page de titre imprimée.
// Une fois tout l'extrait visible, « Lire la suite » mène au livre sur Gutenberg.
function DebutDuLivre({ extrait, urlSource }) {
  const paragraphes = paragraphesSansIllustrations(extrait)
  const [ouvert, setOuvert] = useState(false)
  const [depasse, setDepasse] = useState(false)
  const bloc = useRef(null)

  // le bouton n'apparaît que si le texte dépasse vraiment la hauteur limite
  useLayoutEffect(() => {
    if (bloc.current) setDepasse(bloc.current.scrollHeight > bloc.current.clientHeight + 1)
  }, [extrait])

  if (paragraphes.length === 0) return null
  const toutVisible = ouvert || !depasse
  return (
    <section>
      <h2>Début du livre</h2>
      <div ref={bloc} className={'debut' + (ouvert ? ' ouvert' : depasse ? ' coupe' : '')}>
        {paragraphes.map((p, i) => <p key={i} className={enCapitales(p) ? 'capitales' : undefined}>{p}</p>)}
      </div>
      <div className="actions-debut">
        {toutVisible && (
          <a className="suite" href={urlSource} target="_blank" rel="noreferrer">
            Lire la suite sur Project Gutenberg ↗
          </a>
        )}
        {depasse && (
          <button type="button" className="plus" onClick={() => setOuvert(!ouvert)}>
            {ouvert ? 'Afficher moins' : 'Afficher plus'}
          </button>
        )}
      </div>
    </section>
  )
}
