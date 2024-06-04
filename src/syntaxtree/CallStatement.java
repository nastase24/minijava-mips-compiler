package syntaxtree;
import java.io.PrintStream;

import treedisplay.TreeDisplayable;
import treedisplay.TreeDrawException;
import visitor.Visitor;

/**
 * a statement that consists of a method call
 */
public class CallStatement extends Statement
{

    // instance variables filled in by constructor
    public Call callExp; // the expression that is the actual call

    /**
     * constructor
     * @param pos file position
     * @param aexp the call-expression
     */
    public CallStatement(int pos, Call aexp)
    {
        super(pos);
        callExp = aexp;
    }

    public String name() {return "CallStatement";}

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
            return callExp;
        }
        throw new TreeDrawException();
    }
}

