// Coupe un texte trop long avec « … »
export function couper(texte, max) {
  return texte.length > max ? texte.slice(0, max - 1).trimEnd() + '…' : texte
}
