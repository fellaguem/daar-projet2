package api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import recherche.CorpusDeTest;

// Le vrai serveur, démarré sur un port libre, avec le petit corpus de test.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiTest {

    private static final Path DOSSIER = creerCorpus();

    private static Path creerCorpus() {
        try {
            return CorpusDeTest.creer(Files.createTempDirectory("corpus-api-"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        registre.add("moteur.dossier", DOSSIER::toString);
        registre.add("server.address", () -> "127.0.0.1");
    }

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    private record Rep(int code, JsonNode corps) {}

    private Rep get(String chemin) {
        try {
            HttpResponse<String> r = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + chemin)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            return new Rep(r.statusCode(), json.readTree(r.body()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Test
    void rechercheParMotCleConformeAuContrat() {
        Rep r = get("/api/search?q=whale");
        assertEquals(200, r.code());
        JsonNode c = r.corps();
        assertEquals("whale", c.get("requete").asText());
        assertEquals("motcle", c.get("type").asText());
        assertTrue(c.get("tempsMs").isNumber());
        assertEquals(3, c.get("resultats").size());
        JsonNode premier = c.get("resultats").get(0);
        for (String champ : List.of("id", "titre", "auteur", "occurrences", "pagerank", "score")) {
            assertTrue(premier.has(champ), champ);
        }
        assertTrue(c.get("suggestions").isArray());
        // champs propres à la regex : absents
        assertFalse(c.has("nbMotsTrouves"));
        assertFalse(c.has("motsTrouves"));
    }

    @Test
    void rechercheParRegexConformeAuContrat() {
        Rep r = get("/api/search/advanced?regex=" + enc("wh(a|e)le") + "&mode=index");
        assertEquals(200, r.code());
        assertEquals("regex", r.corps().get("type").asText());
        assertEquals(1, r.corps().get("nbMotsTrouves").asInt());
        assertEquals("whale", r.corps().get("motsTrouves").get(0).asText());
        assertEquals(3, r.corps().get("resultats").size());
    }

    @Test
    void triEtSuggestions() {
        Rep r = get("/api/search?q=" + enc("the whale") + "&tri=occurrences");
        assertEquals(1, r.corps().get("resultats").get(0).get("id").asInt()); // 9 occurrences
        Rep s = get("/api/search?q=" + enc("ship ocean"));
        assertEquals(1, s.corps().get("suggestions").get(0).get("id").asInt());
    }

    @Test
    void pageDUnLivre() {
        Rep r = get("/api/books/2");
        assertEquals(200, r.code());
        JsonNode c = r.corps();
        assertEquals(2, c.get("id").asInt());
        assertEquals("Baleine longue", c.get("titre").asText());
        assertEquals("https://www.gutenberg.org/ebooks/2", c.get("urlSource").asText());
        assertTrue(c.get("extrait").asText().startsWith("The whale"));
        assertEquals(3, c.get("voisins").get(0).get("id").asInt());
    }

    @Test
    void erreurs400Et404() {
        for (String chemin : List.of(
                "/api/search?q=",                                  // vide
                "/api/search",                                     // q absent
                "/api/search?q=whale&tri=titre",                   // tri inconnu
                "/api/search/advanced?regex=" + enc("(wha"),       // regex mal écrite
                "/api/search/advanced?regex=" + enc("a*"),         // accepte le mot vide
                "/api/search/advanced?regex=wha&mode=texte",       // pas encore disponible
                "/api/books/abc")) {                               // id pas un nombre
            Rep r = get(chemin);
            assertEquals(400, r.code(), chemin);
            assertFalse(r.corps().get("erreur").asText().isBlank(), chemin);
        }
        Rep r = get("/api/books/999");
        assertEquals(404, r.code());
        assertTrue(r.corps().get("erreur").asText().contains("999"));
    }

    @Test
    void plusieursClientsEnMemeTemps() throws Exception {
        List<String> chemins = List.of("/api/search?q=whale", "/api/search?q=" + enc("ship ocean"),
            "/api/search/advanced?regex=" + enc("(the|whale)"), "/api/books/2", "/api/search?q=the");
        List<String> attendus = new ArrayList<>();
        for (String c : chemins) {
            attendus.add(sansTemps(get(c).corps()));
        }

        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<String>> futurs = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                String c = chemins.get(i % chemins.size());
                futurs.add(pool.submit(() -> {
                    Rep r = get(c);
                    assertEquals(200, r.code());
                    return sansTemps(r.corps());
                }));
            }
            for (int i = 0; i < futurs.size(); i++) {
                assertEquals(attendus.get(i % chemins.size()), futurs.get(i).get(), chemins.get(i % chemins.size()));
            }
        } finally {
            pool.shutdown();
        }
    }

    // la réponse sans tempsMs (qui change d'un appel à l'autre)
    private static String sansTemps(JsonNode n) {
        JsonNode copie = n.deepCopy();
        if (copie.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) copie).remove("tempsMs");
        }
        return copie.toString();
    }
}
