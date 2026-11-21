import React from 'react'
import ReactDOM from 'react-dom/client'
import { HashRouter } from 'react-router-dom'
import App from './App.jsx'
import './style.css'

// Le défilement est géré par les pages : la fiche remonte en haut, la page Résultats retrouve
// sa place au retour. Le navigateur, lui, restaurerait trop tôt (avant l'arrivée des résultats).
if ('scrollRestoration' in window.history) window.history.scrollRestoration = 'manual'

// HashRouter : les liens (#/livre/12) marchent aussi quand Spring Boot sert la page
ReactDOM.createRoot(document.getElementById('root')).render(
  <HashRouter>
    <App />
  </HashRouter>
)
