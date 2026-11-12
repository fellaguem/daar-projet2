package api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Page d'un livre (données factices pour l'instant)
@RestController
@RequestMapping("/api/books")
public class LivreController {

    @GetMapping("/{id}")
    public Dto.Livre livre(@PathVariable int id) {
        return DonneesFactices.livre(id);
    }
}
