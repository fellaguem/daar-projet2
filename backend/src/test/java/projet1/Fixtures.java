package projet1;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Lecture des regex et des textes de test du projet 1 (src/test/resources/projet1)
final class Fixtures {

    private Fixtures() {}

    private static Path dossier(String nom) throws URISyntaxException {
        return Path.of(Fixtures.class.getResource("/projet1/" + nom).toURI());
    }

    // les regex de input1 à input6 (on saute les lignes vides et les commentaires #)
    static List<String> regexs() throws IOException, URISyntaxException {
        List<String> regexs = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Path fich = dossier("regex").resolve("input" + i + ".txt");
            for (String s : Files.readAllLines(fich, StandardCharsets.UTF_8)) {
                if (!s.isBlank() && !s.trim().startsWith("#")) {
                    regexs.add(s);
                }
            }
        }
        return regexs;
    }

    // les fichiers texte de test, triés par nom
    static List<Path> textes() throws IOException, URISyntaxException {
        try (Stream<Path> stream = Files.list(dossier("textes"))) {
            return stream.filter(p -> p.toString().endsWith(".txt"))
                         .sorted().collect(Collectors.toList());
        }
    }
}
