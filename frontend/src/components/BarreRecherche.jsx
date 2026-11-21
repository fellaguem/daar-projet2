import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { effacerHistorique, lireHistorique } from '../outils/historique.js'
import { EXEMPLES, adresseRecherche, ressembleRegex } from '../outils/recherches.js'
import { IconeCroix, IconeHorloge } from './icones.jsx'

// les deux modes du sélecteur ; le placeholder sert d'exemple
const MODES = [
  { valeur: 'motcle', libelle: 'Mot-clef', placeholder: 'whale' },
  { valeur: 'regex', libelle: 'RegEx', placeholder: 'wh(a|e)le' },
]

// Barre de recherche : sélecteur « Mot-clef | RegEx » collé au champ, puis le bouton.
// Sur l'accueil (accueil = true), la barre est plus grande et suivie des exemples
// et des dernières recherches.
// invalide : la page signale une regex invalide (bordure rouge, aria-invalid) ;
// decritPar : ids du message d'erreur et de la ligne de syntaxe (aria-describedby).
export default function BarreRecherche({
  valeur = '', avance = false, autoFocus = false, accueil = false, invalide = false, decritPar,
}) {
  const [texte, setTexte] = useState(valeur)
  const [mode, setMode] = useState(avance ? 'regex' : 'motcle')
  // la bordure rouge disparaît dès que l'utilisateur modifie le champ
  const [rouge, setRouge] = useState(invalide)
  const champ = useRef(null)
  const aller = useNavigate()
  const { key } = useLocation()
  const vide = !texte.trim()

  // la barre suit l'adresse : après un retour arrière du navigateur, elle montre la bonne requête.
  // Changer de mode ne relance pas la recherche (« white whale » en regex serait une erreur).
  useEffect(() => {
    setTexte(valeur)
    setMode(avance ? 'regex' : 'motcle')
  }, [valeur, avance])

  // chaque navigation (même vers la même adresse) reprend l'état d'erreur donné par la page
  useEffect(() => {
    setRouge(invalide)
  }, [invalide, key])

  function envoyer(e) {
    e.preventDefault()
    if (vide) return
    aller(adresseRecherche(texte.trim(), mode))
  }

  // un mot-clef qui contient un caractère de regex : on propose de chercher en RegEx
  const proposerRegex = mode === 'motcle' && ressembleRegex(texte)
  function passerEnRegex() {
    setMode('regex')
    aller(adresseRecherche(texte.trim(), 'regex'))
  }

  return (
    <>
      <form onSubmit={envoyer} className={accueil ? 'carte barre grande' : 'carte barre'}>
        <div className="saisie">
          {/* de vrais boutons radio, cachés sous le style segmenté : une seule étape de Tab,
              les flèches changent le mode, sans code clavier à écrire.
              Le nom accessible de chaque bouton vient de son label : « Mot-clef », « RegEx ». */}
          <div className="modes" role="radiogroup" aria-label="Type de recherche">
            {MODES.map((m) => (
              <label key={m.valeur} className="mode">
                <input
                  type="radio"
                  name="mode"
                  value={m.valeur}
                  checked={mode === m.valeur}
                  onChange={() => setMode(m.valeur)}
                />
                <span>{m.libelle}</span>
              </label>
            ))}
          </div>
          <input
            ref={champ}
            // en RegEx, le champ passe en police à chasse fixe (une regex est une chaîne littérale)
            className={mode === 'regex' ? 'champ regex' : 'champ'}
            autoFocus={autoFocus}
            aria-label={mode === 'regex' ? 'Expression régulière' : 'Mot-clef'}
            aria-invalid={rouge ? 'true' : undefined}
            aria-describedby={rouge && decritPar ? decritPar : undefined}
            value={texte}
            onChange={(e) => {
              setTexte(e.target.value)
              setRouge(false)
            }}
            placeholder={MODES.find((m) => m.valeur === mode).placeholder}
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck="false"
          />
        </div>
        <button type="submit" className="bouton" disabled={vide}>Rechercher</button>
        {proposerRegex && (
          <div className="proposition">
            <button type="button" className="pastille" onClick={passerEnRegex}>Chercher en RegEx ?</button>
          </div>
        )}
      </form>
      {accueil && <Raccourcis mode={mode} apresEffacement={() => champ.current && champ.current.focus()} />}
    </>
  )
}

// Exemples du mode choisi, puis les 5 dernières recherches (chacune avec son mode).
// Un clic lance la recherche. Une recherche récente identique à un exemple affiché n'est pas
// répétée. La croix vide la liste, puis le focus revient au champ.
function Raccourcis({ mode, apresEffacement }) {
  // lu une fois, à l'affichage de l'accueil
  const [recentes, setRecentes] = useState(lireHistorique)
  const exemples = EXEMPLES[mode]
  const visibles = recentes.filter((r) => !(r.mode === mode && exemples.includes(r.texte)))

  function effacer() {
    effacerHistorique()
    setRecentes([])
    apresEffacement()
  }

  return (
    <div className="raccourcis">
      <ul className="pastilles" aria-label="Exemples">
        {exemples.map((t) => (
          <li key={t}>
            <Link className={mode === 'regex' ? 'pastille regex' : 'pastille'} to={adresseRecherche(t, mode)}>{t}</Link>
          </li>
        ))}
      </ul>
      {visibles.length > 0 && (
        <div className="recentes">
          <IconeHorloge />
          <ul className="pastilles" aria-label="Dernières recherches">
            {visibles.map((r) => (
              <li key={r.mode + ':' + r.texte}>
                <Link
                  className={r.mode === 'regex' ? 'pastille regex' : 'pastille'}
                  to={adresseRecherche(r.texte, r.mode)}
                  aria-label={r.mode === 'regex' ? r.texte + ' (RegEx)' : undefined}
                >
                  {r.texte}
                  {r.mode === 'regex' && <span className="etiquette">RegEx</span>}
                </Link>
              </li>
            ))}
          </ul>
          <button type="button" className="effacer" aria-label="Effacer les recherches récentes" onClick={effacer}>
            <IconeCroix />
          </button>
        </div>
      )}
    </div>
  )
}
