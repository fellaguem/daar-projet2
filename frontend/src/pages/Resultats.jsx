import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import BarreRecherche from '../components/BarreRecherche.jsx'
import ListeResultats from '../components/ListeResultats.jsx'
import Suggestions from '../components/Suggestions.jsx'
import { rechercher, rechercherRegex } from '../api.js'

export default function Resultats() {
  const [params] = useSearchParams()
  const regex = params.get('regex')
  const q = params.get('q')
  const mode = params.get('mode') || 'index'

  const [reponse, setReponse] = useState(null)
  const [erreur, setErreur] = useState(null)

  useEffect(() => {
    setReponse(null)
    setErreur(null)
    const appel = regex ? rechercherRegex(regex, mode) : rechercher(q || '')
    appel.then(setReponse).catch((e) => setErreur(e.message))
  }, [q, regex, mode])

  return (
    <>
      <BarreRecherche valeur={regex || q || ''} avance={!!regex} />
      {erreur && <p className="erreur">{erreur}</p>}
      {!reponse && !erreur && <p>Recherche en cours…</p>}
      {reponse && (
        <>
          <p>
            {reponse.resultats.length} résultat(s) pour « {reponse.requete} » en {reponse.tempsMs} ms
          </p>
          <ListeResultats resultats={reponse.resultats} />
          <Suggestions livres={reponse.suggestions} />
        </>
      )}
    </>
  )
}
