package ast.interfaces;

public interface AstI {

    public AstI getFilsGauche();

    public AstI getFilsDroit();
    
    <T> T accept(AstVisitor<T> visitor);

}
