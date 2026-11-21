import { useId, useState } from 'react'
import { surTelephone } from '../outils/ecran.js'
import { Chevron } from './icones.jsx'

// Liste repliable : un vrai bouton (aria-expanded, aria-controls, Tab puis Entrée ou Espace)
// montre ou cache le contenu. Repliée par défaut sur téléphone (elle ne repousse pas les
// résultats), ouverte sur ordinateur.
// titreH2 : le bouton est placé dans un titre h2 (titre de section).
// ouvertParDefaut : impose l'état de départ (par exemple ouvert quand rien n'est en dessous).
export default function Repliable({ titre, titreH2 = false, ouvertParDefaut, children }) {
  const [ouvert, setOuvert] = useState(() => ouvertParDefaut ?? !surTelephone())
  const id = useId()
  const bouton = (
    <button type="button" className="repli" aria-expanded={ouvert} aria-controls={id} onClick={() => setOuvert(!ouvert)}>
      {titre}
      <Chevron />
    </button>
  )
  return (
    <>
      {titreH2 ? <h2>{bouton}</h2> : bouton}
      <div id={id} className="repliable" hidden={!ouvert}>
        {children}
      </div>
    </>
  )
}
