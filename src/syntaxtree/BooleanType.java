package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * the type 'boolean'
 */
public class BooleanType extends Type
{

    /**
     * constructor
     * @param pos file position
     */
    public BooleanType(int pos)
    {
        super(pos);
    }

    public String name() {return "BoolType";}

    /**
     * type equality
     * @param the object tested for being equal to me
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof BooleanType;
    }

    public boolean isBoolean() {return true;}

    public String vtableName() { return "BOOLEAN"; }
    public String typeName()   { return "Z"; }

    /**
     * hash code
     * @return the object's hash code
     */
    @Override
    public int hashCode()
    {
        return 327236434;
    }

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

    public String toString2()
    {
        return "boolean";
    }
}
