package recherche;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import index.Index;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import projet1.Fixtures;
import recherche.RechercheRegex.RegexInvalide;
import recherche.RechercheRegex.Reponse;

class RechercheRegexTest {

    // livre 0 : whale whaleboat whaler sea ; livre 1 : whale whale white ; livre 2 : sea sailor
    private static RechercheRegex moteur() {
        Index.Constructeur c = new Index.Constructeur();
        c.ajouterLivre(100, "whale whaleboat whaler sea");
        c.ajouterLivre(200, "whale whale white");
        c.ajouterLivre(300, "sea sailor");
        return new RechercheRegex(c.construire());
    }

    @Test
    void unMotVerifieLaRegexSIlLaContient() {
        Reponse r = moteur().chercher("whal");

        // whale, whaleboat, whaler : livre 0 les a tous les trois, livre 1 a "whale" deux fois
        assertEquals(List.of(new Resultat(0, 3), new Resultat(1, 2)), r.resultats());
        assertEquals(3, r.nbMotsTrouves());
        assertEquals(List.of("whale", "whaleboat", "whaler"), r.motsTrouves());
    }

    @Test
    void plusieursMotsTrouvesFontUnOUAvecSommeDesOccurrences() {
        Reponse r = moteur().chercher("whale|sea");

        // livre 0 : whale 1 + whaleboat 1 + whaler 1 + sea 1 ; livre 1 : whale 2 ; livre 2 : sea 1
        assertEquals(List.of(new Resultat(0, 4), new Resultat(1, 2), new Resultat(2, 1)), r.resultats());
    }

    @Test
    void operateursDeLaRegex() {
        assertEquals(List.of("white", "whale", "whaleboat", "whaler").stream().sorted().toList(),
            moteur().chercher("wh(a|i)(l|t)e").motsTrouves().stream().sorted().toList());
        assertEquals(List.of("sailor"), moteur().chercher("s.*r").motsTrouves());
        assertEquals(List.of("whale", "whaleboat", "whaler"), moteur().chercher("whal(e)*").motsTrouves());
    }

    @Test
    void majusculesSansEffet() {
        assertEquals(moteur().chercher("whal"), moteur().chercher("WHAL"));
        assertEquals(moteur().chercher("whale|sea"), moteur().chercher("Whale|SEA"));
    }

    @Test
    void aucunMotTrouve() {
        Reponse r = moteur().chercher("xyz");
        assertEquals(List.of(), r.resultats());
        assertEquals(0, r.nbMotsTrouves());
        assertEquals(List.of(), r.motsTrouves());
    }

    @Test
    void renvoieAuPlusCinquanteMots() {
        // 60 mots de deux lettres : aa, ab, ..., cg
        Index.Constructeur c = new Index.Constructeur();
        StringBuilder texte = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            texte.append((char) ('a' + i / 26)).append((char) ('a' + i % 26)).append(' ');
        }
        c.ajouterLivre(1, texte);
        Reponse r = new RechercheRegex(c.construire()).chercher("a|b|c");

        assertEquals(60, r.nbMotsTrouves());
        assertEquals(RechercheRegex.MOTS_RENVOYES, r.motsTrouves().size());
        assertEquals("aa", r.motsTrouves().get(0));
        assertEquals(List.of(new Resultat(0, 60)), r.resultats());
    }

    @Test
    void refuseLesRegexInvalides() {
        RechercheRegex m = moteur();
        assertThrows(RegexInvalide.class, () -> m.chercher(""));
        assertThrows(RegexInvalide.class, () -> m.chercher("   "));
        assertThrows(RegexInvalide.class, () -> m.chercher(null));
        assertThrows(RegexInvalide.class, () -> m.chercher("(whal"));
        assertThrows(RegexInvalide.class, () -> m.chercher("whal)"));
        assertThrows(RegexInvalide.class, () -> m.chercher("*whal"));
        assertThrows(RegexInvalide.class, () -> m.chercher("a".repeat(RechercheRegex.LONGUEUR_MAX_REGEX + 1)));
    }

    @Test
    void refuseLesRegexQuiFontExploserLAutomate() {
        RechercheRegex m = moteur();

        // 9 points : 1 025 états, accepté
        assertEquals(0, m.chercher("x" + ".".repeat(9)).nbMotsTrouves());

        // 11 points : 4 097 états, refusé
        RegexInvalide e = assertThrows(RegexInvalide.class, () -> m.chercher("x" + ".".repeat(11)));
        assertTrue(e.getMessage().contains("trop complexe"));

        // 99 points : 2^100 états sans limite ; avec la limite la réponse est immédiate
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
            () -> assertThrows(RegexInvalide.class, () -> m.chercher("x" + ".".repeat(99))));
    }

    @Test
    void refuseLesRegexQuiAcceptentLeMotVide() {
        RechercheRegex m = moteur();
        RegexInvalide e = assertThrows(RegexInvalide.class, () -> m.chercher("a*"));
        assertTrue(e.getMessage().contains("mot vide"));
        assertThrows(RegexInvalide.class, () -> m.chercher("whal|"));
        assertThrows(RegexInvalide.class, () -> m.chercher("(a|b)*"));
    }

    // Comparaison avec java.util.regex : un index construit sur les textes de test du projet 1,
    // pour chaque regex des fichiers input, les mêmes mots et les mêmes totaux par livre.
    @Test
    void memesMotsEtMemesTotauxQueJavaRegex() throws Exception {
        Index.Constructeur c = new Index.Constructeur();
        int id = 1;
        for (Path fichier : Fixtures.textes()) {
            c.ajouterLivre(id++, Files.readString(fichier, StandardCharsets.UTF_8));
        }
        Index index = c.construire();
        RechercheRegex moteur = new RechercheRegex(index);

        int regexTestees = 0;
        for (String regex : Fixtures.regexs()) {
            String bas = regex.toLowerCase(java.util.Locale.ROOT);
            Pattern modele = Pattern.compile(bas);

            // une regex qui accepte le mot vide est refusée par notre recherche
            if (modele.matcher("").find()) {
                assertThrows(RegexInvalide.class, () -> moteur.chercher(regex), regex);
                continue;
            }

            // référence : les mots du vocabulaire où java trouve la regex, et leurs totaux par livre
            List<String> attendus = new ArrayList<>();
            Map<Integer, Integer> totauxAttendus = new HashMap<>();
            for (int m = 0; m < index.nbMots(); m++) {
                if (modele.matcher(index.mot(m)).find()) {
                    attendus.add(index.mot(m));
                    for (int p = index.debut(m); p < index.fin(m); p++) {
                        totauxAttendus.merge(index.livreDuPosting(p), index.occurrencesDuPosting(p), Integer::sum);
                    }
                }
            }

            Reponse r = moteur.chercher(regex);
            Map<Integer, Integer> totaux = new HashMap<>();
            r.resultats().forEach(x -> totaux.put(x.livre(), x.occurrences()));

            assertEquals(attendus.size(), r.nbMotsTrouves(), "nombre de mots, regex " + regex);
            assertEquals(attendus.subList(0, Math.min(attendus.size(), RechercheRegex.MOTS_RENVOYES)),
                r.motsTrouves(), "mots, regex " + regex);
            assertEquals(totauxAttendus, totaux, "totaux par livre, regex " + regex);
            regexTestees++;
        }
        System.out.println("RechercheRegexTest : " + regexTestees + " regex comparées à java.util.regex");
        assertTrue(regexTestees > 20);
    }
}
