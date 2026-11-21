import { test } from 'node:test'
import assert from 'node:assert/strict'
import { formaterAuteur } from '../src/outils/auteur.js'

test('auteur au format du catalogue : prénom nom (dates)', () => {
  assert.equal(formaterAuteur('Melville, Herman, 1819-1891'), 'Herman Melville (1819–1891)')
  assert.equal(formaterAuteur('Melville, Herman, 1819-1891', false), 'Herman Melville')
})

test('le nom complet entre parenthèses est retiré des prénoms', () => {
  assert.equal(formaterAuteur('Seward, A. C. (Albert Charles), 1863-1941'), 'A. C. Seward (1863–1941)')
})

test('date de mort inconnue', () => {
  assert.equal(formaterAuteur('Daly, James, 1948-'), 'James Daly (1948–)')
})

test('les contributeurs avec un rôle sont retirés', () => {
  assert.equal(
    formaterAuteur('Melville, Herman, 1819-1891; Stedman, Arthur Griffin, 1859-1908 [Editor]'),
    'Herman Melville (1819–1891)',
  )
  assert.equal(formaterAuteur('Library of Congress; Daly, James, 1948- [Editor]'), 'Library of Congress')
})

test('plusieurs auteurs sans rôle', () => {
  assert.equal(formaterAuteur('Marx, Karl, 1818-1883; Engels, Friedrich, 1820-1895', false), 'Karl Marx, Friedrich Engels')
})

test('un format inattendu est rendu tel quel', () => {
  assert.equal(formaterAuteur('Various'), 'Various')
  assert.equal(formaterAuteur('Osler, William, Sir, 1849-1919'), 'Osler, William, Sir, 1849-1919')
  // seulement des contributeurs avec un rôle : on garde tout, au format brut
  assert.equal(formaterAuteur('Daly, James, 1948- [Editor]'), 'Daly, James, 1948- [Editor]')
})
