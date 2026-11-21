import { test } from 'node:test'
import assert from 'node:assert/strict'
import { EXEMPLES, TRIS, adresseRecherche, ressembleRegex } from '../src/outils/recherches.js'

test('adresse d\'une recherche : q pour un mot-clef, regex pour une RegEx, encodée', () => {
  assert.equal(adresseRecherche('white whale', 'motcle'), '/resultats?q=white+whale')
  assert.equal(adresseRecherche('wh(a|e)le', 'regex'), '/resultats?regex=wh%28a%7Ce%29le')
  assert.equal(adresseRecherche('[a-z]*ology', 'regex'), '/resultats?regex=%5Ba-z%5D*ology')
})

test('une saisie avec un caractère de regex est reconnue', () => {
  for (const s of ['wh(a|e)le', 'a*', 'a+', 'a?', 's.a', '[abc]', 'a|b']) assert.equal(ressembleRegex(s), true, s)
  for (const s of ['whale', 'white whale', 'don\'t', 'moby-dick']) assert.equal(ressembleRegex(s), false, s)
})

test('les exemples d\'un mode sont du bon type', () => {
  for (const e of EXEMPLES.motcle) assert.equal(ressembleRegex(e), false, e)
  for (const e of EXEMPLES.regex) assert.equal(ressembleRegex(e), true, e)
})

test('les tris sont ceux du contrat d\'API, pertinence en premier', () => {
  assert.deepEqual(TRIS.map((t) => t.valeur), ['produit', 'occurrences', 'pagerank'])
})
