// Coupe un texte trop long avec « … » (titres Gutenberg de plus de 250 caractères, requêtes longues).
// Le résultat fait au plus max caractères, « … » compris.
export function couper(texte, max) {
  return texte.length > max ? texte.slice(0, max - 1).trimEnd() + '…' : texte
}
