package parser;

import ast.AstFactory;
import ast.interfaces.AstI;

// transforme la regex en AST par descente recursive, en lisant chaque caractere une seule fois
public class Parser {

    private String re;
    private int position = 0;

    public Parser(String re){
        this.re = re;
    }
    public AstI parse() {
        AstI ast = parseExpression();

        // À la fin du parsing, on doit avoir consommé toute la regex
        if (position != re.length()) {
            throw new RuntimeException(
                "Expression invalide à la position " + position
            );
        }

        return ast;
    }

    // expression ::= concat ('|' concat)*, associative à gauche
    private AstI parseExpression() {
        AstI gauche = parseConcat();

        while (position < re.length() && courant() == '|') {

            avancer(); // consomme |

            AstI droite = parseConcat();

            gauche = AstFactory.createOr(gauche, droite);
        }
        return gauche;
    }

    // concat ::= repeat+ | (vide). Une concaténation vide (fin de la
    // regex ou | ou ) donne un nœud mot vide, comme grep -E
    private AstI parseConcat() {
        if (finDeConcat()) {
            return AstFactory.createVide();
        }

        AstI gauche = parseRepeat();

        while (!finDeConcat()) {

            AstI droite = parseRepeat();

            gauche = AstFactory.createConcat(gauche, droite);
        }

        return gauche;
    }

    // repeat ::= atome '*'* ; « a** » donne Repeat(Repeat(a))
    private AstI parseRepeat() {
        AstI ast = parseAtom();

        while (position < re.length() && courant() == '*') {
            avancer(); // consomme *
            ast = AstFactory.createRepeat(ast);
        }

        return ast;
    }

    // atome ::= '(' expression ')' | '.' | lettre
    private AstI parseAtom(){
        if (position >= re.length()) {
            throw new RuntimeException("Expression invalide : fin inattendue");
        }
        char c = courant();

        // Les parenthèses ne laissent pas de nœud dans l'AST
        if (c == '(') {
            avancer();

            AstI interieur = parseExpression();

            if (position >= re.length() || courant() != ')') {
                throw new RuntimeException("Parenthèse fermante manquante");
            }

            avancer(); // consomme ')'
            return interieur;
        }

        if (c == '.') {
            avancer();
            return AstFactory.createDot();
        }

        // ) n'est pas un atome : il termine une sous-expression
        if (c == ')') {
            throw new RuntimeException("Parenthèse fermante inattendue");
        }

        // *, | ne peuvent pas commencer un atome
        if (c == '*' || c == '|') {
            throw new RuntimeException("Opérateur inattendu : " + c);
        }

        avancer();
        return AstFactory.createChar(c);
    }

    private void avancer(){
        position++;
    }

    private char courant() {
        return re.charAt(position);
    }

    // Une concaténation s'arrête à la fin de la regex, sur | ou sur )
    private boolean finDeConcat() {
        return position >= re.length()
            || courant() == '|'
            || courant() == ')';
    }
}
