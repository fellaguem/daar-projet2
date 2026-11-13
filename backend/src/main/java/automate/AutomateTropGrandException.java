package automate;

// La déterminisation a dépassé le nombre d'états autorisé.
// Une regex comme "a..........." donne un AFD de 2^k états : sans limite, le serveur ne finirait jamais.
public class AutomateTropGrandException extends RuntimeException {

    public AutomateTropGrandException(int etatsMax) {
        super("L'automate dépasse " + etatsMax + " états");
    }
}
