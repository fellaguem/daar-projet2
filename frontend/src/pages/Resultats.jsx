import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import BarreRecherche from '../components/BarreRecherche.jsx'
import ListeResultats from '../components/ListeResultats.jsx'
import Suggestions from '../components/Suggestions.jsx'
import { rechercher, rechercherRegex, TRIS } from '../api.js'

// nombre de mots trouvés par la regex affichés (le serveur en renvoie 50 au plus)
const MOTS_AFFICHES = 20

// Vue Résultats : barre, compteur + tri, mots trouvés (regex), suggestions, liste
export default function Resultats() {
  const [params, setParams] = useSearchParams()
  const regex = params.get('regex')
  const q = params.get('q')
  // le tri est dans l'adresse (&tri=...) : le bouton retour du navigateur le garde
  const tri = params.get('tri') || 'produit'

  const [reponse, setReponse] = useState(null)
  const [erreur, setErreur] = useState(null)

  useEffect(() => {
    // une réponse plus lente d'une ancienne recherche ne doit pas écraser la nouvelle
    let perimee = false
    setErreur(null)
    const appel = regex ? rechercherRegex(regex, tri) : rechercher(q || '', tri)
    appel
      .then((r) => { if (!perimee) setReponse(r) })
      .catch((e) => { if (!perimee) setErreur(e.message) })
    return () => { perimee = true }
  }, [q, regex, tri])

  function changerTri(e) {
    const p = new URLSearchParams(params)
    if (e.target.value === 'produit') p.delete('tri')
    else p.set('tri', e.target.value)
    setParams(p)
  }

  return (
    <>
      <BarreRecherche valeur={regex || q || ''} avance={!!regex} />
      {erreur && <p className="erreur">{erreur}</p>}
      {!erreur && !reponse && <p>Recherche en cours…</p>}
      {!erreur && reponse && <Contenu reponse={reponse} tri={tri} changerTri={changerTri} />}
    </>
  )
}

function Contenu({ reponse, tri, changerTri }) {
  const n = reponse.resultats.length
  const estRegex = reponse.type === 'regex'

  return (
    <>
      <div className="entete-resultats">
        <p className="compteur">
          <strong>{n.toLocaleString('fr-FR')} résultat{n > 1 ? 's' : ''}</strong> pour « {reponse.requete} » ·{' '}
          {reponse.tempsMs.toLocaleString('fr-FR')} ms
        </p>
        {n > 1 && (
          <label className="tri">
            Trier par
            <select value={tri} onChange={changerTri}>
              {TRIS.map((t) => (
                <option key={t.valeur} value={t.valeur}>{t.libelle}</option>
              ))}
            </select>
          </label>
        )}
      </div>

      {estRegex && reponse.nbMotsTrouves > 0 && <MotsTrouves reponse={reponse} />}

      {n === 0 ? (
        <p className="aucun">
          {estRegex
            ? <>Aucun mot de l'index ne vérifie « {reponse.requete} ». Essayez une autre expression.</>
            : <>Aucun livre ne contient « {reponse.requete} ». Essayez un autre mot.</>}
        </p>
      ) : (
        <>
          <Suggestions
            titre="Suggestions — livres proches"
            sousTitre="Voisins des meilleurs résultats dans le graphe de Jaccard (livres au vocabulaire proche)"
            livres={reponse.suggestions}
          />
          {/* la clé change à chaque recherche ou tri : la liste repart à 20 résultats */}
          <ListeResultats key={reponse.type + ':' + reponse.requete + ':' + tri} resultats={reponse.resultats} />
        </>
      )}
    </>
  )
}

// mots de l'index qui vérifient la regex, en pastilles (le résultat clé d'une recherche avancée)
function MotsTrouves({ reponse }) {
  const mots = reponse.motsTrouves.slice(0, MOTS_AFFICHES)
  const reste = reponse.nbMotsTrouves - mots.length
  return (
    <div className="mots-trouves">
      <span>Mots trouvés ({reponse.nbMotsTrouves.toLocaleString('fr-FR')}) :</span>
      <ul className="pastilles">
        {mots.map((m) => (
          <li key={m}><span className="pastille mot">{m}</span></li>
        ))}
      </ul>
      {reste > 0 && <span>… et {reste.toLocaleString('fr-FR')} autre{reste > 1 ? 's' : ''}</span>}
    </div>
  )
}
