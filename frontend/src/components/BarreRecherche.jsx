import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

// Champ de recherche : mot-clef ou RegEx (recherche avancée)
export default function BarreRecherche({ valeur = '', avance = false, autoFocus = false }) {
  const [texte, setTexte] = useState(valeur)
  const [regex, setRegex] = useState(avance)
  const aller = useNavigate()

  // la barre suit l'adresse : après un retour arrière du navigateur, elle montre la bonne requête
  useEffect(() => {
    setTexte(valeur)
    setRegex(avance)
  }, [valeur, avance])

  function envoyer(e) {
    e.preventDefault()
    if (!texte.trim()) return
    const params = new URLSearchParams()
    if (regex) {
      // regex testée sur les mots de l'index (le mode « texte complet » sert seulement aux mesures)
      params.set('regex', texte.trim())
    } else {
      params.set('q', texte.trim())
    }
    aller('/resultats?' + params.toString())
  }

  return (
    <form onSubmit={envoyer} className="carte barre">
      <input
        // en RegEx, le champ passe en police à chasse fixe (une regex est une chaîne littérale)
        className={regex ? 'champ regex' : 'champ'}
        autoFocus={autoFocus}
        aria-label={regex ? 'Expression régulière' : 'Mot-clef'}
        value={texte}
        onChange={(e) => setTexte(e.target.value)}
        placeholder={regex ? 'RegEx, ex. wh(a|e)le' : 'Mot-clef, ex. whale'}
      />
      <button type="submit" className="bouton">Rechercher</button>
      <label>
        <input type="checkbox" checked={regex} onChange={(e) => setRegex(e.target.checked)} />
        Recherche avancée (RegEx)
      </label>
    </form>
  )
}
