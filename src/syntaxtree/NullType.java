package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * the type of the expression 'null'
 */
public class NullType extends Type
{

    /**
     * constructor
     * @param pos file position
     */
    public NullType(int pos)
    {
        super(pos);
    }

    public String name() {return "NullType";}

    public String vtableName() { return "NULL"; }
    public String typeName()   { return "N"; }
    /**
     * type equality
     * @param the object tested for being equal to me
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof NullType;
    }

    public boolean isNull() {return true;}
    /**
     * hash code
     * @return the object's hash code
     */
    @Override
    public int hashCode()
    {
        return 7326834;
    }

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

    public String toString2()
    {
        return "(null type)";
    }

}
