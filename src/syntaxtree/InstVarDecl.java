package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * declaration of an instance variable
 */
public class InstVarDecl extends VarDecl
{

    /**
     * constructor
     * @param pos file position
     * @param atype the type of the variable
     * @param aname the name being declared
     */
    public InstVarDecl(int pos, Type atype, String aname)
    {
        super(pos, atype, aname);
    }

    public String name() {return "InstVarDecl";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

}
