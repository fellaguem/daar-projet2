package corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetadonneesTest {

    @Test
    void litLesLignesDuFichier(@TempDir Path dossier) throws IOException {
        Path f = dossier.resolve("metadonnees.tsv");
        Files.writeString(f,
            "11\tAlice's Adventures in Wonderland\tCarroll, Lewis, 1832-1898\ten\thttps://www.gutenberg.org/ebooks/11\t27427\n"
            + "10\tThe King James Version of the Bible\t\ten\thttps://www.gutenberg.org/ebooks/10\t792180\n"
            + "ligne cassée\n"
            + "x\tTitre\tAuteur\ten\turl\t12\n",
            StandardCharsets.UTF_8);

        Metadonnees m = Metadonnees.charger(f);

        assertEquals(2, m.taille());
        assertEquals("Alice's Adventures in Wonderland", m.livre(11).titre());
        assertEquals("Carroll, Lewis, 1832-1898", m.livre(11).auteur());
        assertEquals(27427, m.livre(11).nbMots());
        assertEquals("", m.livre(10).auteur()); // auteur vide
        assertEquals("https://www.gutenberg.org/ebooks/10", m.livre(10).urlSource());
        assertNull(m.livre(999));
    }
}
