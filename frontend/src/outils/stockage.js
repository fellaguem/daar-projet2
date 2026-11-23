// Lecture et écriture de JSON dans le stockage du navigateur.

function stockage(type) {
  return type === 'session' ? window.sessionStorage : window.localStorage
}

export function lireJSON(type, cle, defaut) {
  try {
    const texte = stockage(type).getItem(cle)
    return texte === null ? defaut : JSON.parse(texte)
  } catch {
    return defaut
  }
}

export function ecrireJSON(type, cle, valeur) {
  try {
    stockage(type).setItem(cle, JSON.stringify(valeur))
  } catch {
    // stockage indisponible : rien n'est gardé
  }
}

export function effacerCle(type, cle) {
  try {
    stockage(type).removeItem(cle)
  } catch {
    // stockage indisponible : il n'y a rien à effacer
  }
}
