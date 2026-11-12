import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

// Champ de recherche : mot-clef ou RegEx (recherche avancée)
export default function BarreRecherche({ valeur = '', avance = false }) {
  const [texte, setTexte] = useState(valeur)
  const [regex, setRegex] = useState(avance)
  const [mode, setMode] = useState('index')
  const aller = useNavigate()

  function envoyer(e) {
    e.preventDefault()
    if (!texte.trim()) return
    const params = new URLSearchParams()
    if (regex) {
      params.set('regex', texte.trim())
      params.set('mode', mode)
    } else {
      params.set('q', texte.trim())
    }
    aller('/resultats?' + params.toString())
  }

  return (
    <form onSubmit={envoyer} className="barre">
      <input
        value={texte}
        onChange={(e) => setTexte(e.target.value)}
        placeholder={regex ? 'RegEx, ex. wh(a|e)le' : 'Mot-clef, ex. whale'}
      />
      <button type="submit">Rechercher</button>
      <label>
        <input type="checkbox" checked={regex} onChange={(e) => setRegex(e.target.checked)} />
        Recherche avancée (RegEx)
      </label>
      {regex && (
        <select value={mode} onChange={(e) => setMode(e.target.value)}>
          <option value="index">Dans l'index</option>
          <option value="texte">Dans le texte (lent)</option>
        </select>
      )}
    </form>
  )
}
