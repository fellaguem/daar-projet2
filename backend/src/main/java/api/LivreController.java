package api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import recherche.Moteur;

// Page d'un livre : métadonnées, extrait, voisins dans le graphe de Jaccard
@RestController
@RequestMapping("/api/books")
public class LivreController {

    private final Moteur moteur;

    public LivreController(Moteur moteur) {
        this.moteur = moteur;
    }

    @GetMapping("/{id}")
    public Dto.Livre livre(@PathVariable int id) {
        return Dto.depuis(moteur.livre(id));
    }
}
