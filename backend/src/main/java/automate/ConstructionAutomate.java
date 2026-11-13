
package automate;

import ast.interfaces.AstI;

// enchaine AFN, AFD puis AFD minimal a partir de l'AST, et garde chaque etape
public class ConstructionAutomate {

    private final Afn afn;
    private final Afd afd;
    private final Afd afdMinimal;

    public ConstructionAutomate(AstI ast) {
        this(ast, Integer.MAX_VALUE);
    }

    // avec une limite sur le nombre d'etats de l'AFD (AutomateTropGrandException si elle est depassee)
    public ConstructionAutomate(AstI ast, int etatsMax) {

        // transformation de l'AST en AFN avec notre visitor
        this.afn = ast.accept(new AstToAfnVisitor());

        // on determinise l'AFN pour obtenir notre AFD
        this.afd = new Determinisation(afn, etatsMax).convertir();

        // puis on minimise l'AFD avec Moore
        this.afdMinimal = new Minimisation(afd).minimiser();

    }

    public Afn getAfn() {
        return afn;
    }

    public Afd getAfd() {
        return afd;
    }

    public Afd getAfdMinimal() {
        return afdMinimal;
    }

}
