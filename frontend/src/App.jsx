import { Link, Route, Routes } from 'react-router-dom'
import Accueil from './pages/Accueil.jsx'
import Resultats from './pages/resultats/Resultats.jsx'
import Livre from './pages/livre/Livre.jsx'
import { IconeLivre } from './components/icones.jsx'

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
