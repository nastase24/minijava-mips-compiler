package syntaxtree;
import visitor.Visitor;
import treedisplay.*;

/**
 * a MiniJava program
 */
public class Program extends AstNode
{

    // instance variables filled in by constructor
    public ClassDeclList classDecls; // the list of class declarations
    public Statement mainStatement; // the (call) statement that starts the execution
    public ClassDeclList dummyNodes; // for the predefined classes

    /**
     * constructor
     * @param pos file position
     * @param acl the program's list of class declarations
     */
    public Program(int pos, ClassDeclList acl)
    {
        super(pos);
        IdentifierType mainType = new IdentifierType(-1, "Main");
        Exp newExp = new NewObject(-1, mainType);
        Call callExp = new Call(-1, newExp, "main", new ExpList());
        mainStatement = new CallStatement(-1, callExp);
        classDecls=acl;
        dummyNodes = new ClassDeclList();
    }

    public String name() {return "Program";}

    public void setDummyNodes(ClassDeclList nodes)
    {
        dummyNodes = nodes;
    }

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
            return classDecls;
        case 1:
            return mainStatement;
        case 2:
            return dummyNodes;
        }
        throw new TreeDrawException();
    }

}
