import { Link, Route, Routes } from 'react-router-dom'
import Accueil from './pages/Accueil.jsx'
import Resultats from './pages/Resultats.jsx'
import Livre from './pages/Livre.jsx'

export default function App() {
  return (
    <>
      <header>
        <Link to="/">Bibliothèque</Link>
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
