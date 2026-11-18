import { Link, Route, Routes } from 'react-router-dom'
import Accueil from './pages/Accueil.jsx'
import Resultats from './pages/Resultats.jsx'
import Livre from './pages/Livre.jsx'

// icône de livre (trait simple), décorative
function IconeLivre() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
    </svg>
  )
}

export default function App() {
  return (
    <>
      <header className="entete">
        <Link to="/" className="marque">
          <span className="logo"><IconeLivre /></span>
          Bibliothèque Gutenberg
        </Link>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Accueil />} />
          <Route path="/resultats" element={<Resultats />} />
          <Route path="/livre/:id" element={<Livre />} />
        </Routes>
      </main>
    </>
  )
}
