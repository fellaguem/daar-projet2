package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

// une feuille de l'arbre : un caractere a lire tel quel
public class AstChar implements AstI{
    private Character valeur;

    public AstChar(Character valeur){
        this.valeur = valeur;
    }

    public Character getValeur(){
        return valeur;
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
