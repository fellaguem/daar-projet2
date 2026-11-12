package corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NettoyageTest {

    @Test
    void retireEnteteEtLicence() {
        String brut = "﻿The Project Gutenberg eBook of Moby Dick\r\nTitle: Moby Dick\r\n\r\n"
            + "*** START OF THE PROJECT GUTENBERG EBOOK MOBY DICK ***\r\n\r\n"
            + "Call me Ishmael.\r\nSome years ago.\r\n\r\n"
            + "*** END OF THE PROJECT GUTENBERG EBOOK MOBY DICK ***\r\n"
            + "Licence : blablabla Project Gutenberg\r\n";

        String texte = Nettoyage.retirerEnteteEtLicence(brut);

        assertEquals("Call me Ishmael.\r\nSome years ago.\n", texte);
        assertFalse(texte.contains("Licence"));
        assertFalse(texte.contains("Title:"));
    }

    @Test
    void marqueurTHISAccepte() {
        String brut = "en-tête\n*** START OF THIS PROJECT GUTENBERG EBOOK X ***\ncorps\n*** END OF THIS PROJECT GUTENBERG EBOOK X ***\nfin";
        assertEquals("corps\n", Nettoyage.retirerEnteteEtLicence(brut));
    }

    @Test
    void sansMarqueurDeFinOnGardeJusquALaFin() {
        String brut = "*** START OF THE PROJECT GUTENBERG EBOOK X ***\nun\ndeux\n";
        assertEquals("un\ndeux\n", Nettoyage.retirerEnteteEtLicence(brut));
    }

    @Test
    void sansMarqueurDeDebutOnEcarte() {
        assertNull(Nettoyage.retirerEnteteEtLicence("juste du texte\nsans marqueur\n"));
    }
}
