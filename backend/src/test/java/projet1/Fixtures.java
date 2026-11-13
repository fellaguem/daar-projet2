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
public final class Fixtures {

    private Fixtures() {}

    private static Path dossier(String nom) throws URISyntaxException {
        return Path.of(Fixtures.class.getResource("/projet1/" + nom).toURI());
    }

    // les regex de tous les fichiers inputN.txt, dans l'ordre de N
    // (input1 à input6 : projet 1 ; input7 : syntaxe étendue du projet 2, + ? et [..])
    // on saute les lignes vides et les commentaires #
    public static List<String> regexs() throws IOException, URISyntaxException {
        List<String> regexs = new ArrayList<>();
        for (int i = 1; Files.exists(dossier("regex").resolve("input" + i + ".txt")); i++) {
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
    public static List<Path> textes() throws IOException, URISyntaxException {
        try (Stream<Path> stream = Files.list(dossier("textes"))) {
            return stream.filter(p -> p.toString().endsWith(".txt"))
                         .sorted().collect(Collectors.toList());
        }
    }
}
