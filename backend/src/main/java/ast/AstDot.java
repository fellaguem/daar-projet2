package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

// le point : une feuille qui accepte n'importe quel caractere
public class AstDot implements AstI{

    /** crée un noeud point */
    public AstDot(){
    }

    @Override 
    public AstI getFilsGauche(){
        return null;
    }

    @Override 
    public AstI getFilsDroit(){
        return null;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
