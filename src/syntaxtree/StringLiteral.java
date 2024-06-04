package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * a string literal, as in '"Hello World"'
 */
public class StringLiteral extends Exp
{

    // instance variables filled in by constructor
    public String str; // the string denoted by the string literal

    // instance variables filled in during later phases
    public StringLiteral uniqueCgRep; // the expression representing this one during code generation

    /**
     * constructor
     * @param pos file position
     * @param astr the string that the string literal denotes
     */
    public StringLiteral(int pos, String astr)
    {
        super(pos);
        str = astr;
        uniqueCgRep = null;
    }

    public String name() {return "StringLiteral";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

    protected String[]stringsInDescr()
    {
        return strArrayPlus1(str, super.stringsInDescr());
    }

    public AstNode[] links()
    {
        return new AstNode[] {uniqueCgRep};
    }

}
