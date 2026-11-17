package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Démarrage "à chaud", côté HTTP : dès que le serveur écoute, il s'envoie quelques requêtes.
// Moteur.chauffer() chauffe la recherche elle-même ; ici on chauffe le reste (Spring MVC, écriture
// du JSON). Sans cela, la première requête de la démo prenait environ 640 ms, les suivantes 70 à 115 ms.
// Si le serveur n'écoute pas sur 127.0.0.1 (server.address réglé sur une autre adresse), on ne
// fait rien de plus qu'un message dans le journal.
@Component
public class ChauffeHttp {

    private static final Logger LOG = LoggerFactory.getLogger(ChauffeHttp.class);

    private static final List<String> CHEMINS = List.of(
        "/api/search?q=the%20whale",
        "/api/search?q=love&tri=occurrences",
        "/api/search/advanced?regex=wh%28a%7Ce%29le",
        "/api/search/advanced?regex=q.*z&motEntier=false&tri=pagerank",
        "/api/books/0");   // livre introuvable : chauffe aussi la réponse d'erreur

    @EventListener(ApplicationReadyEvent.class)
    public void chauffer(ApplicationReadyEvent evenement) {
        if (!(evenement.getApplicationContext() instanceof WebServerApplicationContext contexte)) {
            return;
        }
        int port = contexte.getWebServer().getPort();
        long debut = System.nanoTime();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        try {
            for (int tour = 0; tour < 3; tour++) {
                for (String chemin : CHEMINS) {
                    client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + chemin)).GET().build(),
                        HttpResponse.BodyHandlers.discarding());
                }
            }
            LOG.info("Serveur chauffé (requêtes HTTP) en {} ms", (System.nanoTime() - debut) / 1_000_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.info("Chauffe HTTP impossible ({}), sans conséquence", e.getMessage());
        }
    }
}
