package syntaxtree;
import visitor.Visitor;

/**
 * a declaration (abstract)
 */
public abstract class Decl extends AstNode
{

    // instance variables filled in by constructor
    public String name; // the name being declared

    /**
     * constructor
     * @param pos file position
     * @param aname the name being declared
     */
    public Decl(int pos, String aname)
    {
        super(pos);
        name=aname;
    }

    public String name() {return "Decl";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

    protected String[]stringsInDescr()
    {
        return strArrayPlus1(super.stringsInDescr(),name);
    }
}
