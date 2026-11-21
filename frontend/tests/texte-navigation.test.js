import { test } from 'node:test'
import assert from 'node:assert/strict'
import { couper } from '../src/outils/texte.js'
import { depuisLivre, depuisResultats, vientDesResultats, vientDunLivre } from '../src/outils/navigation.js'

test('couper : un texte court est gardé, un texte long fait au plus max caractères avec « … »', () => {
  assert.equal(couper('Typee', 10), 'Typee')
  assert.equal(couper('Moby-Dick; or, The Whale', 10), 'Moby-Dick…')
  assert.equal(couper('Moby-Dick; or, The Whale', 10).length, 10)
  // pas d'espace avant « … »
  assert.equal(couper('The Sea Wolf and more', 8), 'The Sea…')
})

test('état transmis depuis les résultats, avec ou sans chiffres', () => {
  const chiffres = { occurrences: '1 244', pagerank: '0,0012', pertinence: '100 %' }
  assert.deepEqual(depuisResultats('whale', 'motcle', chiffres), { depuis: 'resultats', requete: 'whale', type: 'motcle', chiffres })
  assert.deepEqual(depuisResultats('whale', 'motcle'), { depuis: 'resultats', requete: 'whale', type: 'motcle' })
  assert.equal(vientDesResultats(depuisResultats('whale', 'motcle')), true)
  assert.equal(vientDunLivre(depuisResultats('whale', 'motcle')), false)
})

test('état transmis depuis un livre', () => {
  assert.deepEqual(depuisLivre('Moby-Dick'), { depuis: 'livre', titre: 'Moby-Dick' })
  assert.equal(vientDunLivre(depuisLivre('Moby-Dick')), true)
  assert.equal(vientDesResultats(depuisLivre('Moby-Dick')), false)
})

test('pas d\'état (lien direct) ou état inattendu', () => {
  for (const etat of [null, undefined, {}, { depuis: 'resultats' }, { depuis: 'livre', titre: 3 }]) {
    assert.equal(vientDesResultats(etat), false)
    assert.equal(vientDunLivre(etat), false)
  }
})
