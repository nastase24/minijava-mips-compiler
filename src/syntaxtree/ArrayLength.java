package syntaxtree;
import java.io.PrintStream;

import treedisplay.TreeDisplayable;
import treedisplay.TreeDrawException;
import visitor.Visitor;

/**
 * a array-length expression, as in "a.length"
 */
public class ArrayLength extends UnExp
{

    /**
     * constructor
     * @param pos file position
     * @param arrExp the operand
     */
    public ArrayLength(int pos, Exp arrExp)
    {
        super(pos, arrExp);
    }
    public String name() {return "ArrayLength";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

}
