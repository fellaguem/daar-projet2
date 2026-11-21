import { test } from 'node:test'
import assert from 'node:assert/strict'
import { enCapitales, paragraphesSansIllustrations } from '../src/pages/livre/extrait.js'

test('les illustrations sont retirées, même sur plusieurs paragraphes', () => {
  const extrait = [
    'CHAPTER I.',
    '[Illustration: The whale]',
    '[Illustration: A ship',
    'in a storm]',
    'Call me Ishmael.',
  ].join('\n\n')
  assert.deepEqual(paragraphesSansIllustrations(extrait), ['CHAPTER I.', 'Call me Ishmael.'])
})

test('l\'italique en texte brut (_..._) est retiré, les paragraphes vides aussi', () => {
  assert.deepEqual(paragraphesSansIllustrations('The _Pequod_ sailed.\n\n   \n\nEnd.'), ['The Pequod sailed.', 'End.'])
})

test('paragraphe en majuscules : page de titre, pas une phrase ni un chiffre', () => {
  assert.equal(enCapitales('MOBY-DICK; OR, THE WHALE.'), true)
  assert.equal(enCapitales('BY HERMAN MELVILLE,'), true)
  assert.equal(enCapitales('Call me Ishmael.'), false)
  assert.equal(enCapitales('CHAPTER 1. Loomings.'), false)
  assert.equal(enCapitales('1851.'), false)
  assert.equal(enCapitales('I.'), false)
})
