package corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FiltresTest {

    // ---- filtre de contenu ----

    @Test
    void ecarteLesListesDeMotsEtDeChiffres() {
        assertTrue(Filtres.titreExclu("The 1990 CIA World Factbook"));
        assertTrue(Filtres.titreExclu("The 1990 United States Census [incomplete]"));
        assertTrue(Filtres.titreExclu("Roget's Thesaurus"));
        assertTrue(Filtres.titreExclu("Webster's Unabridged Dictionary"));
        assertTrue(Filtres.titreExclu("A Gazetteer of the World"));
    }

    @Test
    void ignoreLesMajuscules() {
        assertTrue(Filtres.titreExclu("THE WORLD FACTBOOK"));
        assertTrue(Filtres.titreExclu("a pocket dictionary"));
    }

    @Test
    void gardeLesVraisLivresEtLesEncyclopedies() {
        assertFalse(Filtres.titreExclu("Moby-Dick; or, The Whale"));
        assertFalse(Filtres.titreExclu("The King James Version of the Bible"));
        assertFalse(Filtres.titreExclu("Encyclopaedia Britannica, 11th Edition"));
    }

    // ---- filtre de doublons ----

    @Test
    void memeTitreEtMemeAuteurDonnentLaMemeCle() {
        assertEquals(
            Filtres.cleDoublon("Paradise Lost", "Milton, John, 1608-1674"),
            Filtres.cleDoublon("Paradise Lost", "Milton, John, 1608-1674"));
    }

    @Test
    void titresNormalisesMemeCle() {
        // majuscules, ponctuation, article et ordre des mots ne comptent pas
        assertEquals(
            Filtres.cleDoublon("The King James Version of the Bible", ""),
            Filtres.cleDoublon("The Bible, King James Version, Complete", ""));
        assertEquals(
            Filtres.cleDoublon("Paradise Lost", "Milton, John, 1608-1674"),
            Filtres.cleDoublon("PARADISE LOST.", "Milton, John"));
    }

    @Test
    void auteursDifferentsPasDeDoublon() {
        assertNotEquals(
            Filtres.cleDoublon("Poems", "Keats, John, 1795-1821"),
            Filtres.cleDoublon("Poems", "Poe, Edgar Allan, 1809-1849"));
    }

    @Test
    void volumesDifferentsPasDeDoublon() {
        assertNotEquals(
            Filtres.cleDoublon("Les Misérables, Volume 1", "Hugo, Victor"),
            Filtres.cleDoublon("Les Misérables, Volume 2", "Hugo, Victor"));
    }

    @Test
    void onGardeLePremierLivreEnregistre() {
        Filtres.Doublons doublons = new Filtres.Doublons();

        // identifiant 20 : premier vu, donc gardé
        assertFalse(doublons.estDoublon("Paradise Lost", "Milton, John, 1608-1674"));
        doublons.enregistrer("Paradise Lost", "Milton, John, 1608-1674");

        // identifiant 26 : même livre, écarté
        assertTrue(doublons.estDoublon("Paradise Lost", "Milton, John, 1608-1674"));
        // un autre livre n'est pas touché
        assertFalse(doublons.estDoublon("Peter Pan", "Barrie, J. M."));
    }
}
