package api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Routes de recherche (pour l'instant elles renvoient des données factices)
@RestController
@RequestMapping("/api/search")
public class RechercheController {

    @GetMapping
    public Dto.ReponseRecherche motCle(@RequestParam String q) {
        return DonneesFactices.reponse(q, "motcle");
    }

    // mode = index (par défaut) ou texte
    @GetMapping("/advanced")
    public Dto.ReponseRecherche avancee(@RequestParam String regex,
                                        @RequestParam(defaultValue = "index") String mode) {
        return DonneesFactices.reponse(regex, "regex");
    }
}
