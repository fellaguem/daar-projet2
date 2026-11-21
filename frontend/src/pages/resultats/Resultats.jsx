import { useEffect, useLayoutEffect, useState } from 'react'
import { Link, useNavigationType, useSearchParams } from 'react-router-dom'
import BarreRecherche from '../../components/BarreRecherche.jsx'
import ListeResultats, { PAR_PAGE } from '../../components/ListeResultats.jsx'
import Suggestions from '../../components/Suggestions.jsx'
import Repliable from '../../components/Repliable.jsx'
import { rechercher, rechercherRegex } from '../../api/client.js'
import { ajouterHistorique } from '../../outils/historique.js'
import { depuisResultats } from '../../outils/navigation.js'
import { EXEMPLES, TRIS, adresseRecherche } from '../../outils/recherches.js'
import { lirePlace, sauverPlace } from './place.js'

// nombre de mots trouvés par la regex affichés (le serveur en renvoie 50 au plus)
const MOTS_AFFICHES = 20

// ids du message d'erreur et de la ligne de syntaxe (aria-describedby du champ)
const ID_ERREUR = 'erreur-recherche'
const ID_SYNTAXE = 'syntaxe-regex'

// ---------- la page ----------

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
      .then((r) => {
        if (perimee) return
        setReponse(r)
        // seulement une recherche qui a donné au moins un résultat (pas une regex invalide)
        if (r.resultats.length > 0) ajouterHistorique(regex || q, regex ? 'regex' : 'motcle')
      })
      .catch((e) => { if (!perimee) setErreur({ message: e.message, statut: e.statut }) })
    return () => { perimee = true }
  }, [q, regex, tri])

  function changerTri(e) {
    const p = new URLSearchParams(params)
    if (e.target.value === 'produit') p.delete('tri')
    else p.set('tri', e.target.value)
    setParams(p)
  }

  // regex refusée par le serveur (400) : champ en rouge, et rappel de la syntaxe acceptée
  const regexInvalide = !!regex && !!erreur && erreur.statut === 400

  return (
    <>
      <BarreRecherche
        valeur={regex || q || ''}
        avance={!!regex}
        invalide={regexInvalide}
        decritPar={ID_ERREUR + ' ' + ID_SYNTAXE}
      />
      {/* zone toujours présente : un lecteur d'écran annonce le message quand il apparaît */}
      <div aria-live="polite">
        {erreur && <p className="erreur" id={ID_ERREUR}>{erreur.message}</p>}
        {regexInvalide && <p className="syntaxe" id={ID_SYNTAXE}>. ( ) | * + ? [abc] [a-z]</p>}
      </div>
      {!erreur && !reponse && <p>Recherche en cours…</p>}
      {/* la clé change à chaque recherche ou tri : la liste repart à 20 résultats */}
      {!erreur && reponse && (
        <Contenu key={cleRecherche(reponse, tri)} reponse={reponse} tri={tri} changerTri={changerTri} />
      )}
    </>
  )
}

// ---------- morceaux de la page ----------

// une recherche = son type, sa requête et son tri (clé de la liste et de la place gardée)
function cleRecherche(reponse, tri) {
  return reponse.type + ':' + reponse.requete + ':' + tri
}

// Ce qui suit la barre quand la réponse est arrivée : compteur et tri, mots trouvés,
// puis « aucun résultat » ou les suggestions et la liste. Retrouve sa place au retour d'une fiche.
function Contenu({ reponse, tri, changerTri }) {
  const n = reponse.resultats.length
  const estRegex = reponse.type === 'regex'
  const cle = cleRecherche(reponse, tri)

  // Retour d'une fiche (bouton retour : navigation « POP ») : la liste retrouve sa longueur,
  // puis le lien cliqué revient au milieu de l'écran avec le focus clavier.
  // Pour une nouvelle recherche ou un nouveau tri, on part du haut.
  const navigation = useNavigationType()
  const [place] = useState(() => (navigation === 'POP' ? lirePlace(cle) : null))
  const [nbAffiches, setNbAffiches] = useState(() => (place && place.n > PAR_PAGE ? place.n : PAR_PAGE))

  useLayoutEffect(() => {
    if (!place) return
    const lien = place.ancre && document.querySelector('[data-ancre="' + place.ancre + '"]')
    // un lien dans une liste repliée n'est pas visible (offsetParent null) : on reprend le défilement
    if (lien && lien.offsetParent !== null) {
      lien.scrollIntoView({ block: 'center' })
      lien.focus({ preventScroll: true })
    } else {
      window.scrollTo(0, place.y)
    }
  }, [place])

  // au clic sur un livre (résultat ou suggestion), on garde la place pour le retour
  function memoriser(e) {
    const lien = e.target.closest('a[data-ancre]')
    if (lien) sauverPlace(cle, { y: Math.round(window.scrollY), n: nbAffiches, ancre: lien.dataset.ancre })
  }

  return (
    <div onClickCapture={memoriser}>
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
        <>
          <p className="aucun">
            {estRegex
              ? <>Aucun mot de l'index ne vérifie « {reponse.requete} ». Essayez une autre expression.</>
              : <>Aucun livre ne contient « {reponse.requete} ». Essayez un autre mot.</>}
          </p>
          <AutresPistes reponse={reponse} />
        </>
      ) : (
        <>
          <Suggestions
            titre="Suggestions — livres proches"
            sousTitre="Voisins des meilleurs résultats dans le graphe de Jaccard (livres au vocabulaire proche)"
            livres={reponse.suggestions}
            etat={depuisResultats(reponse.requete, reponse.type)}
          />
          <ListeResultats
            resultats={reponse.resultats}
            requete={reponse.requete}
            type={reponse.type}
            nbAffiches={nbAffiches}
            onVoirPlus={() => setNbAffiches(nbAffiches + PAR_PAGE)}
          />
        </>
      )}
    </div>
  )
}

// Sous « aucun résultat » : les exemples de l'accueil (selon le mode) et, pour un mot-clef
// d'un seul mot, la même recherche en RegEx sur un morceau de mot (.*mot.*).
function AutresPistes({ reponse }) {
  const mode = reponse.type === 'regex' ? 'regex' : 'motcle'
  const mot = reponse.requete.trim().toLowerCase()
  const enRegex = mode === 'motcle' && /^[a-z]+$/.test(mot) ? '.*' + mot + '.*' : null
  return (
    <ul className="pastilles autres-pistes" aria-label="Autres recherches">
      {enRegex && (
        <li>
          <Link className="pastille" to={adresseRecherche(enRegex, 'regex')}>
            Essayer en RegEx : <span className="code">{enRegex}</span>
          </Link>
        </li>
      )}
      {EXEMPLES[mode].map((t) => (
        <li key={t}>
          <Link className={mode === 'regex' ? 'pastille regex' : 'pastille'} to={adresseRecherche(t, mode)}>{t}</Link>
        </li>
      ))}
    </ul>
  )
}

// mots de l'index qui vérifient la regex, en pastilles (le résultat clé d'une recherche avancée),
// dans une liste repliable comme les suggestions
function MotsTrouves({ reponse }) {
  const mots = reponse.motsTrouves.slice(0, MOTS_AFFICHES)
  const reste = reponse.nbMotsTrouves - mots.length
  return (
    <div className="mots-trouves">
      <Repliable titre={'Mots trouvés (' + reponse.nbMotsTrouves.toLocaleString('fr-FR') + ')'}>
        <ul className="pastilles">
          {mots.map((m) => (
            <li key={m}><span className="pastille mot">{m}</span></li>
          ))}
        </ul>
        {reste > 0 && <span>… et {reste.toLocaleString('fr-FR')} autre{reste > 1 ? 's' : ''}</span>}
      </Repliable>
    </div>
  )
}
