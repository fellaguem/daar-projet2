package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

// l'alternative | : on accepte ce que reconnait le fils gauche ou le fils droit
public class AstOr implements AstI {
    private AstI filsDroit;
    private AstI filsGauche;

    public AstOr(AstI filsGauche, AstI filsDroit){
        this.filsGauche = filsGauche;
        this.filsDroit = filsDroit;
    }

    @Override 
    public AstI getFilsGauche(){
        return filsGauche;
    }

    @Override 
    public AstI getFilsDroit(){
        return filsDroit;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
