package syntaxtree;
import java.io.PrintStream;

import treedisplay.TreeDisplayable;
import treedisplay.TreeDrawException;
import visitor.Visitor;

/**
 * an array type
 */
public class ArrayType extends Type
{
    public Type baseType;

    /**
     * constructor
     * @param pos file position
     * @param base the base type of the array
     */
    public ArrayType(int pos, Type base)
    {
        super(pos);
        baseType = base;
    }

    /**
     * type equality
     * @param the object tested for being equal to me
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof ArrayType && 
               this.baseType.equals(((ArrayType)obj).baseType);
    }

    public boolean isArray() {return true;}

    public String vtableName() { return "ARRAY_" + baseType.vtableName(); }
    public String typeName()   { return "["+baseType.typeName(); }

    /**
     * hash code
     * @return the object's hash code
     */
    @Override
    public int hashCode()
    {
        return 18623 + 274673*this.baseType.hashCode();
    }

    public String name() {return "ArrayType";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

    public TreeDisplayable getDrawTreeSubobj(int n) throws TreeDrawException
    {
        switch (n)
        {
        case 0:
            return baseType;
        }
        throw new TreeDrawException();
    }

    public String toString2()
    {
        return ""+baseType.toString2()+"[]";
    }
}
