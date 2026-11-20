"""Courbes du rapport à partir des CSV de recherche.MesureCorpus.

Usage (depuis la racine du dépôt) :
    python docs/mesures/courbes.py
    python docs/mesures/courbes.py --donnees docs/mesures --sortie docs/mesures/figures

Lit sous_corpus.csv, construction.csv, requetes.csv et index_texte.csv,
écrit 5 images PNG : construction_index, distances_jaccard, requetes, index_texte, heaps.
Seule dépendance : matplotlib.
"""
import argparse
import csv
import math
import os

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

# ---------- couleurs et style ----------
SERIE = ["#2a78d6", "#eb6834", "#1baf7a"]   # bleu, orange, vert d'eau (palette validée, ordre fixe)
ENCRE = "#0b0b0b"
ENCRE_2 = "#52514e"
DISCRET = "#898781"
GRILLE = "#e1e0d9"
AXE = "#c3c2b7"
MARQUES = ["o", "s", "^"]

plt.rcParams.update({
    "font.size": 10,
    "axes.titlesize": 11,
    "axes.titleweight": "bold",
    "axes.titlecolor": ENCRE,
    "axes.labelcolor": ENCRE_2,
    "axes.edgecolor": AXE,
    "axes.linewidth": 0.8,
    "axes.spines.top": False,
    "axes.spines.right": False,
    "axes.grid": True,
    "grid.color": GRILLE,
    "grid.linewidth": 0.6,
    "xtick.color": DISCRET,
    "ytick.color": DISCRET,
    "xtick.labelcolor": ENCRE_2,
    "ytick.labelcolor": ENCRE_2,
    "legend.frameon": False,
    "legend.labelcolor": ENCRE_2,
    "lines.linewidth": 2,
    "lines.markersize": 5.5,
    "figure.dpi": 100,
    "savefig.dpi": 200,
    "savefig.bbox": "tight",
    "figure.facecolor": "white",
})


def nombre_fr(x, decimales=None):
    """Nombre à la française : virgule décimale, espace fine pour les milliers."""
    if decimales is None:
        if x == 0:
            decimales = 0
        elif abs(x) >= 100:
            decimales = 0
        elif abs(x) >= 10:
            decimales = 0 if float(x).is_integer() else 1
        elif abs(x) >= 1:
            decimales = 0 if float(x).is_integer() else 1
        else:
            decimales = max(0, -int(math.floor(math.log10(abs(x)))) + 1)
    texte = f"{x:,.{decimales}f}"
    return texte.replace(",", "\u00a0").replace(".", ",")


FORMAT_FR = FuncFormatter(lambda x, _pos: nombre_fr(x))


def format_log(x, _pos):
    if x <= 0:
        return ""
    return nombre_fr(x)


def axes_fr(ax, logy=False, logx=False):
    if logy:
        ax.yaxis.set_major_formatter(FuncFormatter(format_log))
    else:
        ax.yaxis.set_major_formatter(FORMAT_FR)
    if logx:
        ax.xaxis.set_major_formatter(FuncFormatter(format_log))
    else:
        ax.xaxis.set_major_formatter(FORMAT_FR)
    ax.tick_params(length=3)
    ax.set_axisbelow(True)


def lire(chemin):
    with open(chemin, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def regression(xs, ys):
    """Droite des moindres carrés y = a x + b."""
    n = len(xs)
    mx, my = sum(xs) / n, sum(ys) / n
    a = sum((x - mx) * (y - my) for x, y in zip(xs, ys)) / sum((x - mx) ** 2 for x in xs)
    return a, my - a * mx


def etiquette_fin(ax, x, y, texte, dy=0):
    ax.annotate(texte, (x, y), xytext=(6, dy), textcoords="offset points",
                va="center", ha="left", fontsize=9, color=ENCRE_2)


# ---------- figures ----------

def fig1_index(sc, cons, sortie):
    tailles = [int(l["n"]) for l in sc]
    mots = {int(l["n"]): int(l["mots_indexes"]) / 1e6 for l in sc}
    idx = {int(l["n"]): l for l in cons if l["etape"] == "index"}
    xs = [mots[n] for n in tailles]
    ys = [float(idx[n]["moyenne_ms"]) / 1000 for n in tailles]
    es = [float(idx[n]["ecart_type_ms"]) / 1000 for n in tailles]
    a, b = regression(xs, ys)

    fig, ax = plt.subplots(figsize=(6.4, 3.6))
    xr = [0, max(xs) * 1.05]
    ax.plot(xr, [a * x + b for x in xr], ls="--", lw=1.2, color=DISCRET, zorder=1)
    ax.errorbar(xs, ys, yerr=es, fmt="o", color=SERIE[0], ecolor=SERIE[0], capsize=3,
                mec="white", mew=1, zorder=3)
    for n, x, y in zip(tailles, xs, ys):
        if n == tailles[0]:
            continue   # les deux premiers points sont trop proches : une seule étiquette pour les deux
        texte = f"N = {nombre_fr(tailles[0])} et {nombre_fr(n)}" if n == tailles[1] else f"N = {nombre_fr(n)}"
        ax.annotate(texte, (x, y), xytext=(7, -11), textcoords="offset points",
                    ha="left", va="center", fontsize=8.5, color=ENCRE_2)
    ax.text(0.02, 0.94, f"droite ajustée : {nombre_fr(a * 1000, 0)} ms par million de mots",
            transform=ax.transAxes, ha="left", va="top", fontsize=9, color=ENCRE_2)
    ax.set_xlim(0, max(xs) * 1.05)
    ax.set_ylim(0, max(ys) * 1.12)
    ax.set_xlabel("mots indexés (millions)")
    ax.set_ylabel("temps (s)")
    ax.set_title("Construction de l'index : linéaire en nombre de mots", loc="left")
    axes_fr(ax)
    fig.savefig(os.path.join(sortie, "construction_index.png"))
    plt.close(fig)


def fig2_distances(sc, cons, sortie):
    tailles = [int(l["n"]) for l in sc]
    paires = {int(l["n"]): int(l["paires"]) for l in sc}
    dist = {int(l["n"]): l for l in cons if l["etape"] == "distances"}
    ys = [float(dist[n]["moyenne_ms"]) for n in tailles]
    es = [float(dist[n]["ecart_type_ms"]) for n in tailles]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10, 3.6))

    # (a) temps total, avec une parabole calée sur N = 800
    ref = 800 if 800 in dist else tailles[len(tailles) // 2]
    c = float(dist[ref]["moyenne_ms"]) / ref ** 2
    xr = [i * max(tailles) / 100 for i in range(101)]
    ax1.plot(xr, [c * x * x for x in xr], ls="--", lw=1.2, color=DISCRET, zorder=1)
    ax1.errorbar(tailles, ys, yerr=es, fmt="o-", color=SERIE[0], ecolor=SERIE[0], capsize=3,
                 mec="white", mew=1, zorder=3)
    etiquette_fin(ax1, tailles[-1], ys[-1], "mesuré")
    ax1.annotate(f"N² pur, calé sur N = {nombre_fr(ref)}", (tailles[-1], c * tailles[-1] ** 2),
                 xytext=(6, 0), textcoords="offset points", va="center", fontsize=9, color=DISCRET)
    ax1.set_xlim(0, max(tailles) * 1.02)
    ax1.set_ylim(0, max(ys) * 1.12)
    ax1.set_xlabel("nombre de livres N")
    ax1.set_ylabel("temps (ms)")
    ax1.set_title("(a) Distances de Jaccard, N(N−1)/2 paires", loc="left")
    axes_fr(ax1)

    # (b) coût d'une paire
    par_paire = [float(dist[n]["moyenne_ms"]) * 1e6 / paires[n] for n in tailles]
    ax2.plot(tailles, par_paire, "o-", color=SERIE[0], mec="white", mew=1)
    for n, v in zip(tailles, par_paire):
        ax2.annotate(nombre_fr(v, 0), (n, v), xytext=(0, 7), textcoords="offset points",
                     ha="center", fontsize=8.5, color=ENCRE_2)
    ax2.set_xlim(0, max(tailles) * 1.05)
    ax2.set_ylim(0, max(par_paire) * 1.2)
    ax2.set_xlabel("nombre de livres N")
    ax2.set_ylabel("temps par paire (ns)")
    ax2.set_title("(b) Coût d'une paire", loc="left")
    axes_fr(ax2)

    fig.tight_layout(w_pad=4)
    fig.savefig(os.path.join(sortie, "distances_jaccard.png"))
    plt.close(fig)


# points touchés par une pause du ramasse-miettes (écart type très supérieur à la moyenne)
def est_pause(ligne):
    return float(ligne["ecart_type_ms"]) > float(ligne["moyenne_ms"])


def panneau_requetes(ax, req, type_, requetes, noms, titre, decalages=None):
    pauses = []
    for i, (q, nom) in enumerate(zip(requetes, noms)):
        lignes = sorted((l for l in req if l["type"] == type_ and l["requete"] == q),
                        key=lambda l: int(l["n"]))
        ns = [int(l["n"]) for l in lignes]
        ys = [float(l["moyenne_ms"]) for l in lignes]
        ax.plot(ns, ys, "-", color=SERIE[i], zorder=2)
        for l, n, y in zip(lignes, ns, ys):
            if est_pause(l):
                ax.plot([n], [y], MARQUES[i], ms=7, mfc="white", mec=SERIE[i], mew=1.5, zorder=4)
                ax.annotate("*", (n, y), xytext=(5, 4), textcoords="offset points",
                            fontsize=11, color=ENCRE_2)
                pauses.append(nom)
            else:
                e = float(l["ecart_type_ms"])
                ax.errorbar([n], [y], yerr=[e], fmt=MARQUES[i], color=SERIE[i], ecolor=SERIE[i],
                            capsize=2.5, mec="white", mew=1, zorder=3)
        ax.plot([], [], "-" + MARQUES[i], color=SERIE[i], label=nom)
        etiquette_fin(ax, ns[-1], ys[-1], nom, dy=(decalages or {}).get(nom, 0))
    ax.set_xlim(0, 2350)
    ax.set_ylim(0, None)
    ax.set_xlabel("nombre de livres N")
    ax.set_ylabel("temps (ms)")
    ax.set_title(titre, loc="left")
    ax.legend(loc="upper left", fontsize=9)
    axes_fr(ax)
    return pauses


def fig3_requetes(req, sortie):
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10, 3.8))
    panneau_requetes(ax1, req, "motcle", ["whale", "love", "the"],
                     ["whale", "love", "the"], "(a) Mot-clef",
                     decalages={"the": 6, "love": -6})
    panneau_requetes(ax2, req, "regex", ["whale", "s(a|g|r)*on", ".*ing"],
                     ["whale", "s(a|g|r)*on", ".*ing"], "(b) Regex dans l'index (mot entier)",
                     decalages={"whale": -7, "s(a|g|r)*on": 7})
    fig.tight_layout(w_pad=4)
    fig.savefig(os.path.join(sortie, "requetes.png"))
    plt.close(fig)


def fig4_index_texte(it, sortie):
    regex = ["wh(a|e)le", ".*ing"]
    fig, ax = plt.subplots(figsize=(6.4, 4.0))
    for i, r in enumerate(regex):
        for mode, style, plein in (("index", "-", True), ("texte", "--", False)):
            lignes = sorted((l for l in it if l["regex"] == r and l["mode"] == mode),
                            key=lambda l: int(l["n"]))
            ns = [int(l["n"]) for l in lignes]
            ys = [float(l["moyenne_ms"]) for l in lignes]
            es = [float(l["ecart_type_ms"]) for l in lignes]
            ax.errorbar(ns, ys, yerr=es, fmt=MARQUES[i], ls=style, color=SERIE[i], ecolor=SERIE[i],
                        capsize=2.5, mfc=SERIE[i] if plein else "white", mec=SERIE[i], mew=1.3,
                        label=f"{r}, {'index' if mode == 'index' else 'texte complet'}")
    # rapports texte / index pour wh(a|e)le, avec un trait entre les deux points
    for n, cote in ((100, 1), (800, -1)):
        idx = next(l for l in it if l["regex"] == regex[0] and l["mode"] == "index" and int(l["n"]) == n)
        txt = next(l for l in it if l["regex"] == regex[0] and l["mode"] == "texte" and int(l["n"]) == n)
        yi, yt = float(idx["moyenne_ms"]), float(txt["moyenne_ms"])
        ax.plot([n, n], [yi * 1.25, yt / 1.25], ls=":", lw=1.2, color=DISCRET, zorder=1)
        ax.annotate(f"× {nombre_fr(round(yt / yi, -1), 0)}", (n, math.sqrt(yi * yt)), xytext=(8 * cote, 0),
                    textcoords="offset points", va="center", ha="left" if cote > 0 else "right",
                    fontsize=9.5, color=ENCRE, fontweight="bold")
    ax.set_yscale("log")
    ax.set_xlim(0, 900)
    ax.set_xlabel("nombre de livres N")
    ax.set_ylabel("temps par requête (ms, échelle log)")
    ax.set_title("Regex : index contre texte complet (mêmes livres trouvés)", loc="left")
    ax.legend(loc="center", bbox_to_anchor=(0.52, 0.47), fontsize=8.5)
    axes_fr(ax, logy=True)
    fig.savefig(os.path.join(sortie, "index_texte.png"))
    plt.close(fig)


def fig5_heaps(sc, sortie):
    xs = [int(l["mots_indexes"]) for l in sc]
    ys = [int(l["vocabulaire"]) for l in sc]
    beta, logk = regression([math.log(x) for x in xs], [math.log(y) for y in ys])
    k = math.exp(logk)

    fig, ax = plt.subplots(figsize=(6.4, 3.6))
    xr = [xs[0] * 0.8, xs[-1] * 1.25]
    # axes en millions de mots et en milliers de mots distincts
    ax.plot([x / 1e6 for x in xr], [k * x ** beta / 1e3 for x in xr], ls="--", lw=1.2, color=DISCRET, zorder=1)
    ax.plot([x / 1e6 for x in xs], [y / 1e3 for y in ys], "o", color=SERIE[0], mec="white", mew=1, zorder=3)
    for l, x, y in zip(sc, xs, ys):
        premier = l is sc[0]   # le premier point est contre l'axe : étiquette à droite
        ax.annotate(f"N = {nombre_fr(int(l['n']))}", (x / 1e6, y / 1e3), xytext=(7, -9) if premier else (-6, 7),
                    textcoords="offset points", ha="left" if premier else "right", fontsize=8.5, color=ENCRE_2)
    b = nombre_fr(beta, 2).replace(",", "{,}")
    ax.text(0.98, 0.07, f"$V \\approx {k:.0f}\\,n^{{{b}}}$   (loi de Heaps, n en mots)",
            transform=ax.transAxes, ha="right", fontsize=9.5, color=ENCRE_2)
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xticks([5, 10, 20, 50, 100, 150])
    ax.set_yticks([80, 100, 150, 200, 300, 400])
    ax.set_xlim(xr[0] / 1e6, xr[1] / 1e6)
    ax.set_xlabel("mots indexés n (millions, échelle log)")
    ax.set_ylabel("vocabulaire V (milliers, échelle log)")
    ax.set_title("Le vocabulaire grandit bien moins vite que le texte", loc="left")
    axes_fr(ax, logy=True, logx=True)
    ax.xaxis.set_minor_formatter(FuncFormatter(lambda x, p: ""))
    ax.yaxis.set_minor_formatter(FuncFormatter(lambda x, p: ""))
    fig.savefig(os.path.join(sortie, "heaps.png"))
    plt.close(fig)
    return beta, k


def main():
    ici = os.path.dirname(os.path.abspath(__file__))
    p = argparse.ArgumentParser()
    p.add_argument("--donnees", default=ici, help="dossier des CSV (par défaut : celui du script)")
    p.add_argument("--sortie", default=os.path.join(ici, "figures"), help="dossier des PNG")
    args = p.parse_args()
    os.makedirs(args.sortie, exist_ok=True)

    sc = sorted(lire(os.path.join(args.donnees, "sous_corpus.csv")), key=lambda l: int(l["n"]))
    cons = lire(os.path.join(args.donnees, "construction.csv"))
    req = lire(os.path.join(args.donnees, "requetes.csv"))
    it = lire(os.path.join(args.donnees, "index_texte.csv"))

    fig1_index(sc, cons, args.sortie)
    fig2_distances(sc, cons, args.sortie)
    fig3_requetes(req, args.sortie)
    fig4_index_texte(it, args.sortie)
    beta, k = fig5_heaps(sc, args.sortie)
    print(f"5 figures écrites dans {args.sortie} (Heaps : beta = {beta:.3f}, K = {k:.1f})")


if __name__ == "__main__":
    main()
