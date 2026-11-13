package api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import recherche.Moteur;

// Crée le moteur une seule fois, au démarrage du serveur.
// Dossier des données : propriété moteur.dossier (application.properties, "data" par défaut),
// à changer au lancement avec --moteur.dossier=data/test20 par exemple.
// Si le serveur est lancé depuis backend/ (mvnw spring-boot:run), on cherche aussi dans ../
// Un fichier manquant arrête le démarrage avec un message clair.
@Configuration
public class ConfigurationMoteur {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationMoteur.class);

    @Bean
    public Moteur moteur(@Value("${moteur.dossier:data}") String dossier) throws IOException {
        Path chemin = Path.of(dossier);
        if (!Files.exists(chemin.resolve("index.bin")) && Files.exists(Path.of("..", dossier, "index.bin"))) {
            chemin = Path.of("..", dossier);
        }
        chemin = chemin.toAbsolutePath().normalize();

        long debut = System.nanoTime();
        Moteur moteur = Moteur.charger(chemin);
        LOG.info("Moteur chargé depuis {} : {} livres en {} ms",
            chemin, moteur.nbLivres(), (System.nanoTime() - debut) / 1_000_000);
        return moteur;
    }
}
