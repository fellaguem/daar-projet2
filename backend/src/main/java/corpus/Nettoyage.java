package corpus;

import java.util.regex.Pattern;

/* On garde juste le texte du livre, pas les infos Gutenberg autour. */
public final class Nettoyage {

    private Nettoyage() {
    }

    private static final Pattern DEBUT = Pattern.compile(
            "^\\s*\\*\\*\\*\\s*START OF (THE|THIS) PROJECT GUTENBERG.*$", Pattern.MULTILINE);

    private static final Pattern FIN = Pattern.compile("^\\s*\\*\\*\\*\\s*END OF (THE|THIS) PROJECT GUTENBERG.*$",
            Pattern.MULTILINE);

    public static String retirerEnteteEtLicence(String brut) {

        if (brut.startsWith("\uFEFF"))
            brut = brut.substring(1); // enlève le BOM s'il y en a un

        var d = DEBUT.matcher(brut);
        if (!d.find())
            return null;

        int a = d.end();

        var f = FIN.matcher(brut);
        int b = f.find(a) ? f.start() : brut.length(); // sans fin, on garde tout

        return brut.substring(a, b).strip() + "\n";
    }
}
