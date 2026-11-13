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

    // repeat ::= atome ('*' | '+' | '?')* ; « a** » donne Repeat(Repeat(a))
    // Projet 2 : + et ? sont traduits avec les noeuds qui existent deja,
    //   x+ = x x*      (x suivi de zero ou plusieurs x)
    //   x? = x | vide  (x ou rien)
    private AstI parseRepeat() {
        AstI ast = parseAtom();

        while (position < re.length()
                && (courant() == '*' || courant() == '+' || courant() == '?')) {

            char operateur = courant();
            avancer(); // consomme l'operateur

            if (operateur == '*') {
                ast = AstFactory.createRepeat(ast);
            } else if (operateur == '+') {
                // le meme sous-arbre sert deux fois : le visiteur cree de nouveaux etats a chaque visite
                ast = AstFactory.createConcat(ast, AstFactory.createRepeat(ast));
            } else {
                ast = AstFactory.createOr(ast, AstFactory.createVide());
            }
        }

        return ast;
    }

    // Projet 2 : classe de caracteres [abc], [a-z], [a-cx-z], [é-ü].
    // Elle devient une alternative a|b|c : pas de nouveau noeud dans l'arbre.
    // Le - au debut ou a la fin est une lettre ; [^...] n'est pas gere.
    private static final int TAILLE_MAX_CLASSE = 1000;

    private AstI parseClasse() {
        avancer(); // consomme [

        if (position < re.length() && courant() == '^') {
            throw new RuntimeException("Classe négative [^...] non gérée");
        }

        // TreeSet : pas de doublons, et l'ordre est toujours le meme
        java.util.Set<Character> lettres = new java.util.TreeSet<>();

        while (true) {
            if (position >= re.length()) {
                throw new RuntimeException("Crochet fermant manquant");
            }
            char c = courant();
            if (c == ']') {
                break;
            }
            avancer();

            // une plage c-d, sauf si le - est le dernier caractere de la classe
            if (position + 1 < re.length() && courant() == '-' && re.charAt(position + 1) != ']') {
                avancer(); // consomme -
                char d = courant();
                avancer();
                if (c > d) {
                    throw new RuntimeException("Plage à l'envers : " + c + "-" + d);
                }
                if (d - c + 1 > TAILLE_MAX_CLASSE) {
                    throw new RuntimeException("Classe trop grande");
                }
                for (char x = c; x <= d; x++) {
                    lettres.add(x);
                    if (x == Character.MAX_VALUE) {
                        break;
                    }
                }
            } else {
                lettres.add(c);
            }

            if (lettres.size() > TAILLE_MAX_CLASSE) {
                throw new RuntimeException("Classe trop grande");
            }
        }
        avancer(); // consomme ]

        if (lettres.isEmpty()) {
            throw new RuntimeException("Classe vide []");
        }

        AstI ast = null;
        for (char c : lettres) {
            AstI lettre = AstFactory.createChar(c);
            ast = ast == null ? lettre : AstFactory.createOr(ast, lettre);
        }
        return ast;
    }

    // atome ::= '(' expression ')' | '.' | '[' classe ']' | lettre
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

        if (c == '[') {
            return parseClasse();
        }

        // ) n'est pas un atome : il termine une sous-expression
        if (c == ')') {
            throw new RuntimeException("Parenthèse fermante inattendue");
        }

        // *, +, ?, | ne peuvent pas commencer un atome
        if (c == '*' || c == '+' || c == '?' || c == '|') {
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
