package ast;

import ast.interfaces.AstFactoryI;
import ast.interfaces.AstI;

// regroupe la creation des noeuds de l'arbre, utiliser par le parser et par MatcherUnePasse
public class AstFactory implements AstFactoryI{

    private AstFactory() {
    }

    public static AstI createChar(Character valeur) {
        return new AstChar(valeur);
    }

    public static AstI createDot() {
        return new AstDot();
    }

    public static AstI createVide() {
        return new AstVide();
    }

    public static AstI createRepeat(AstI fils) {
        return new AstRepeat(fils);
    }

    public static AstI createConcat(AstI gauche, AstI droite) {
        return new AstConcat(gauche, droite);
    }

    public static AstI createOr(AstI gauche, AstI droite) {
        return new AstOr(gauche, droite);
    }
}
