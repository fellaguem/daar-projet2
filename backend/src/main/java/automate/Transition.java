package automate;

// une fleche de l'automate : un etat de depart, un etat d'arrivee et le symbole lu
public class Transition{
    private Etat source;
    private Etat destination;
    private String symbole;

    public Transition(Etat source, Etat destination, String symbole){
        this.source = source;
        this.destination = destination;
        this.symbole = symbole;
    }

    public String getSymbole() {
        return symbole;
    }

    public Etat getSource(){
        return source;
    }

    public Etat getDestination(){
        return destination;
    }
}
