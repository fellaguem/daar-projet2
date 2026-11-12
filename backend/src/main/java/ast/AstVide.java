package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;


// le mot vide, cree pour une regex vide, a| ou () : il est trouve dans toutes les lignes
public class AstVide implements AstI {

    public AstVide() {
    }

    @Override
    public AstI getFilsGauche() {
        return null;
    }

    @Override
    public AstI getFilsDroit() {
        return null;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
