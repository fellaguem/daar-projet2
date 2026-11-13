package api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import recherche.Moteur;

// Routes de recherche (voir docs/api.md)
@RestController
@RequestMapping("/api/search")
public class RechercheController {

    private final Moteur moteur;

    public RechercheController(Moteur moteur) {
        this.moteur = moteur;
    }

    // tri = produit (par défaut), occurrences ou pagerank
    @GetMapping
    public Dto.ReponseRecherche motCle(@RequestParam String q,
                                       @RequestParam(required = false) String tri) {
        return Dto.depuis(moteur.chercherMotCle(q, tri));
    }

    // mode = index (par défaut) ou texte (pas encore disponible)
    @GetMapping("/advanced")
    public Dto.ReponseRecherche avancee(@RequestParam String regex,
                                        @RequestParam(defaultValue = "index") String mode,
                                        @RequestParam(required = false) String tri) {
        return Dto.depuis(moteur.chercherRegex(regex, mode, tri));
    }
}
