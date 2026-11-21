package corpus;

import java.util.regex.Pattern;


/*
 * Retire l'en-tete et la licence que Project Gutenberg ajoute a chaque livre.
 *
 * texte brut -> on garde ce qui est entre "*** START OF ..." et "*** END OF ..."
 */

public final class Nettoyage {


    private Nettoyage() {}

    // "*** START OF THE PROJECT GUTENBERG EBOOK ... ***" (ou THIS PROJECT GUTENBERG EBOOK)
    private static final Pattern DEBUT =
        Pattern.compile("^\\s*\\*\\*\\*\\s*START OF (THE|THIS) PROJECT GUTENBERG.*$", Pattern.MULTILINE);

    private static final Pattern FIN =
        Pattern.compile("^\\s*\\*\\*\\*\\s*END OF (THE|THIS) PROJECT GUTENBERG.*$", Pattern.MULTILINE);

    // renvoie le texte entre les deux marqueurs
    // sans marqueur de debut : null (on ne sait pas ou commence le livre, il est ecarte)
    // sans marqueur de fin : on garde jusqu'a la fin du fichier
    public static String retirerEnteteEtLicence(String brut) {

        if (brut.startsWith("﻿")) {
            brut = brut.substring(1); // BOM : caractere invisible au debut de certains fichiers
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
