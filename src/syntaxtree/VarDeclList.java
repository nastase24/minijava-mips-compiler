package syntaxtree;

import java.util.List;

import visitor.Visitor;

/**
 * a list of variable declarations
 */
public class VarDeclList extends AstList<VarDecl>
{

    public VarDeclList()
    {
        super();
    }

    public VarDeclList(List<VarDecl> lst)
    {
        super(lst);
    }

    public String name() {return "VarDeclList";}

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

}
