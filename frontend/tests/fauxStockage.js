// Faux localStorage / sessionStorage en mémoire, pour tester outils/ sans navigateur.
export class FauxStockage {
  constructor() {
    this.donnees = new Map()
  }
  getItem(cle) {
    return this.donnees.has(cle) ? this.donnees.get(cle) : null
  }
  setItem(cle, valeur) {
    this.donnees.set(cle, String(valeur))
  }
  removeItem(cle) {
    this.donnees.delete(cle)
  }
}

// un stockage qui refuse tout accès (navigation privée stricte, stockage bloqué)
export class StockageBloque {
  getItem() {
    throw new Error('SecurityError')
  }
  setItem() {
    throw new Error('SecurityError')
  }
  removeItem() {
    throw new Error('SecurityError')
  }
}

// installe un faux « window » avec les deux stockages ; renvoie le window installé
export function installerFenetre(local = new FauxStockage(), session = new FauxStockage()) {
  globalThis.window = { localStorage: local, sessionStorage: session }
  return globalThis.window
}

export function retirerFenetre() {
  delete globalThis.window
}
