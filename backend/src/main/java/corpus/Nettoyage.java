package corpus;

import java.util.regex.Pattern;

// Retire l'en-tête et la licence que Project Gutenberg ajoute à chaque livre
public final class Nettoyage {

    private Nettoyage() {}

    // "*** START OF THE PROJECT GUTENBERG EBOOK ... ***" (ou THIS PROJECT GUTENBERG EBOOK)
    private static final Pattern DEBUT =
        Pattern.compile("^\\s*\\*\\*\\*\\s*START OF (THE|THIS) PROJECT GUTENBERG.*$", Pattern.MULTILINE);
    private static final Pattern FIN =
        Pattern.compile("^\\s*\\*\\*\\*\\s*END OF (THE|THIS) PROJECT GUTENBERG.*$", Pattern.MULTILINE);

    // Renvoie le texte du livre entre les deux marqueurs, ou null si le marqueur de début
    // est absent (on ne sait alors pas où commence le livre : on écarte le livre).
    // S'il n'y a pas de marqueur de fin, on garde jusqu'à la fin du fichier.
    public static String retirerEnteteEtLicence(String brut) {
        if (brut.startsWith("﻿")) {
            brut = brut.substring(1); // BOM
        }

        var debut = DEBUT.matcher(brut);
        if (!debut.find()) {
            return null;
        }
        int depart = debut.end();

        var fin = FIN.matcher(brut);
        int arrivee = fin.find(depart) ? fin.start() : brut.length();

        return brut.substring(depart, arrivee).strip() + "\n";
    }
}
