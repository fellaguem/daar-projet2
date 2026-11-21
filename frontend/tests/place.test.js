import { afterEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { lirePlace, sauverPlace } from '../src/pages/resultats/place.js'
import { StockageBloque, installerFenetre, retirerFenetre } from './fauxStockage.js'

afterEach(retirerFenetre)

test('place : relue par recherche, gardée dans sessionStorage', () => {
  const fenetre = installerFenetre()
  sauverPlace('motcle:whale:produit', { y: 1060, n: 40, ancre: 'r-1194' })
  assert.deepEqual(lirePlace('motcle:whale:produit'), { y: 1060, n: 40, ancre: 'r-1194' })
  assert.equal(lirePlace('motcle:ship:produit'), null)
  assert.equal(fenetre.localStorage.getItem('bibliotheque.places'), null)
})

test('place : une place abîmée est ignorée, 20 recherches au plus', () => {
  const fenetre = installerFenetre()
  fenetre.sessionStorage.setItem('bibliotheque.places', JSON.stringify({ a: { y: 'haut', n: 20 } }))
  assert.equal(lirePlace('a'), null)
  for (let i = 0; i < 25; i++) sauverPlace('r' + i, { y: i, n: 20 })
  const gardees = Object.keys(JSON.parse(fenetre.sessionStorage.getItem('bibliotheque.places')))
  assert.equal(gardees.length, 20)
  assert.equal(lirePlace('r4'), null)
  assert.deepEqual(lirePlace('r24'), { y: 24, n: 20 })
})

test('place : sans stockage ou stockage bloqué, pas d\'erreur', () => {
  assert.equal(lirePlace('motcle:whale:produit'), null)
  installerFenetre(new StockageBloque(), new StockageBloque())
  assert.doesNotThrow(() => sauverPlace('motcle:whale:produit', { y: 10, n: 40 }))
  assert.equal(lirePlace('motcle:whale:produit'), null)
})
