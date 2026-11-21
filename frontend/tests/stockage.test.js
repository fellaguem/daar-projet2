import { afterEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { ajouterHistorique, effacerHistorique, lireHistorique } from '../src/outils/historique.js'
import { ecrireJSON, lireJSON } from '../src/outils/stockage.js'
import { StockageBloque, installerFenetre, retirerFenetre } from './fauxStockage.js'

afterEach(retirerFenetre)

test('historique : la plus récente en tête, sans doublon, 5 au plus', () => {
  installerFenetre()
  for (const t of ['a', 'b', 'c', 'd', 'e', 'f']) ajouterHistorique(t, 'motcle')
  ajouterHistorique('c', 'motcle')
  assert.deepEqual(lireHistorique().map((r) => r.texte), ['c', 'f', 'e', 'd', 'b'])
})

test('historique : le même texte dans les deux modes compte pour deux recherches', () => {
  installerFenetre()
  ajouterHistorique('whale', 'motcle')
  ajouterHistorique('whale', 'regex')
  assert.deepEqual(lireHistorique(), [{ texte: 'whale', mode: 'regex' }, { texte: 'whale', mode: 'motcle' }])
})

test('historique : les entrées abîmées sont ignorées, effacer vide la liste', () => {
  const fenetre = installerFenetre()
  fenetre.localStorage.setItem('bibliotheque.historique', JSON.stringify([{ texte: 'ok', mode: 'motcle' }, { texte: 3 }, null, { texte: 'x', mode: 'autre' }]))
  assert.deepEqual(lireHistorique(), [{ texte: 'ok', mode: 'motcle' }])
  fenetre.localStorage.setItem('bibliotheque.historique', 'pas du JSON')
  assert.deepEqual(lireHistorique(), [])
  ajouterHistorique('whale', 'motcle')
  effacerHistorique()
  assert.deepEqual(lireHistorique(), [])
})

test('stockage : local et session sont séparés', () => {
  const fenetre = installerFenetre()
  ecrireJSON('session', 'cle', { y: 10 })
  assert.deepEqual(lireJSON('session', 'cle', null), { y: 10 })
  assert.equal(lireJSON('local', 'cle', null), null)
  assert.equal(fenetre.localStorage.getItem('cle'), null)
})

test('sans stockage, ou avec un stockage bloqué : valeur par défaut et pas d\'erreur', () => {
  // pas de window du tout
  assert.deepEqual(lireHistorique(), [])
  assert.doesNotThrow(() => ajouterHistorique('whale', 'motcle'))
  assert.equal(lireJSON('session', 'cle', 'défaut'), 'défaut')
  // stockage qui refuse tout accès
  installerFenetre(new StockageBloque(), new StockageBloque())
  assert.doesNotThrow(() => ajouterHistorique('whale', 'motcle'))
  assert.doesNotThrow(() => effacerHistorique())
  assert.deepEqual(lireHistorique(), [])
  assert.doesNotThrow(() => ecrireJSON('session', 'cle', 1))
  assert.equal(lireJSON('session', 'cle', 'défaut'), 'défaut')
})
