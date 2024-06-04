package visitor;

import syntaxtree.*;
import errorMsg.*;
import java.io.*;

public class CG3Visitor extends Visitor
{
    // The purpose here is to generate assembly for each Node
    // in the AST.

    // IO stream to which we will emit code
    CodeStream code;
    ClassDecl current;
    // current stack height
    int stack;

    public CG3Visitor(ErrorMsg e, PrintStream out)
    {
        code = new CodeStream(out, e);
        code.setVisitor3(this);
        stack = 0;
    }

    @Override
    public Object visit(Program n)
    {
        code.emit(".text");
        code.emit(".globl main");
        code.emit("main:");
        code.emit("  jal vm_init");

        //exit the program
        code.emit("li $s6,2");
        code.emit("li $s7,0");
        code.emit("jal newObject");
        code.emit("la $t0,CLASS_Main");
        code.emit("sw $t0,-12($s7)");
        code.emit("lw $s2,($sp)");
        code.emit("addu $sp,$sp,4");
        code.emit("jal fcn_main_Main");
        code.emit("li $v0,10");
        code.emit("syscall");

        // emit code for all the methods in all the class declarations
        n.classDecls.accept(this);

        // flush the output and return
        code.flush();
        return null;
    }

    //////////////////////////////////////
    // Decls
    /////////////////////////////////////

    @Override
    public Object visit(ClassDecl c){
        this.current = c;
        c.decls.accept(this);
        return null;
    }

    @Override
    public Object visit(MethodDeclVoid m){
        //code.comment(m,"Start MethodDeclVoid " + m.name());
        //code.emit(".globl fcn_" +m.name+ "_" + m.classDecl.name);
        code.emit("fcn_" +m.name+ "_" + current.name + ":");
        int tmp_stack = this.stack;
        this.stack = 0;
        for(int i = 0; i < m.formals.size(); i++){
            if(m.formals.get(i).type.isInt()){
                this.stack += 8;
            }else{
                this.stack += 4;
            }
            m.formals.get(i).offset = this.stack;
        }
        push(new NullType(0),"$ra");
        int tmp_stack_2 = this.stack;
        Object visit = visit((MethodDecl)m);
        pop(this.stack - tmp_stack_2);
        pop(new NullType(0),"$ra");
        code.emit("  jr $ra");
        return visit;
    }

    @Override
    public Object visit(MethodDeclNonVoid m){
        //code.comment(m,"Start MethodDeclNonVoid " + m.name());
        code.emit(".globl fcn_" +m.name+ "_" +m.classDecl.name);
        code.emit("fcn_" +m.name+ "_" +m.classDecl.name + ":");
        //is this right?
        int tmp_stack = this.stack;
        this.stack = 0;
        for(int i = 0; i < m.formals.size(); i++){
            if(m.formals.get(i).type.isInt()){
                this.stack += 8;
            }else{
                this.stack += 4;
            }
            m.formals.get(i).offset = this.stack;
        }
        push(new NullType(0),"$ra");
        int tmp_stack_2 = this.stack;
        //cant i just call visit(m)?
        m.stmts.accept(this);
        m.rtnExp.accept(this);

        //not sure if this is right
        pop(m.rtnType,"$t0");
        pop(this.stack-tmp_stack_2);
        this.stack = tmp_stack_2;
        pop(new NullType(0),"$ra");

        code.emit("  jr $ra");
        //code.comment(m,"End MethodDeclNonVoid " + m.name);

        return null;
    }

    @Override
    public Object visit(LocalVarDecl l){
        l.initExp.accept(this);
        l.offset = stack;
        return null;
    }

    //////////////////////////////////////
    // Statements
    //////////////////////////////////////

    @Override
    public Object visit(Assign a){
        a.lhs.accept(this);
        a.rhs.accept(this);
        int offset = 0;
        if(a.lhs instanceof IdentifierExp){
            pop(a.lhs.type,"$t0");
            code.comment(a,"IdentifierExp Assignment");
            if(((IdentifierExp)a.lhs).link instanceof InstVarDecl){
                offset = ((IdentifierExp)a.lhs).link.offset;
                code.emit("  sw $t0," +offset+ "($sp)");
            }else{
                code.emit("  sw $t0," +(stack-((IdentifierExp)a.lhs).link.offset)+ "($sp)");

            }
        }else if(a.lhs instanceof ArrayLookup){
            code.comment(a,"ArrayLookup Assignment");
            pop(a.rhs.type,"$t0");
            pop(((ArrayLookup)a.lhs).idxExp.type,"$t1");
            pop(((ArrayLookup)a.lhs).arrExp.type,"$t2");
            outOfBounds("$t2","$t1");
            arrayLoad("$t1", "$t2", "$t1");
            code.emit("  sw $t0, ($t1)");
        }else if(a.lhs instanceof InstVarAccess){
            code.comment(a,"InstVar Assignment");
            offset = ((InstVarAccess)a.lhs).varDec.offset;
            pop(a.lhs.type, "$t0");
            pop(new NullType(0), "$t1");
            nullPointerException("$t1");
            code.emit("  sw $t0," +offset+ "($s2)");
        }
        return null;
    }

//    @Override
//    public Object visit(Block b){
//        for(Statement stmt: b.stmts){
//            stmt.accept(this);
//        }
//        //what is size locals
//        //pop()
//        return null;
//    }

    @Override
    public Object visit(CallStatement c){
        c.callExp.accept(this);
        if(c.callExp.type instanceof IntegerType){
            pop(8);
        }else if(c.callExp.methodLink instanceof MethodDeclVoid){
            pop(4);
        }
        return null;
    }

    @Override
    public Object visit(Call c){
        c.obj.accept(this);
        int distance = stack;
        for(int i = 0; i < c.parms.size(); i++){
            c.parms.get(i).accept(this);
        }
        distance = this.stack - distance;
        code.emit("  lw $t0," +distance+ "($sp)");
        code.emit("  sw $s2," +distance+ "($sp)");
        code.emit("  move $s2, $t0");

        code.emit("  lw $t0, -12($s2)");
        code.emit("  lw $t0," + (4*c.methodLink.vtableOffset)+ "($t0)");
        code.emit("  jalr $t0");

        pop(distance);
        pop(new NullType(0),"$s2");
        push(c.type,"$t0");
        return null;
    }

    @Override
    public Object visit(LocalDeclStatement l){
        l.localVarDecl.accept(this);
        code.emit("  lw $0,($sp)");
        return null;
    }

    @Override
    public Object visit(If i){
        i.exp.accept(this);
        pop(i.exp.type,"$t0");
        code.emit("  beq $t0,$0,if_else_" +i.uniqueId);
        i.trueStmt.accept(this);
        code.emit("j if_done_" +i.uniqueId);
        code.emit("if_else_" +i.uniqueId+ ":");
        i.falseStmt.accept(this);
        code.emit("if_done_" +i.uniqueId+ ":");
        return null;
    }

    @Override
    public Object visit(While w){
        code.emit("while_cond_" +w.uniqueId+ ":");
        w.stackHeight = this.stack;
        w.exp.accept(this);
        pop(w.exp.type,"$t0");
        code.emit("  beq $t0, $0,break_target_" +w.uniqueId);
        w.body.accept(this);
        code.emit("  j while_cond_" + w.uniqueId);
        code.emit("break_target_" +w.uniqueId+ ":");
        return null;
    }

    @Override
    public Object visit(Break b){
        pop(this.stack - b.breakLink.stackHeight);
        b.breakLink.stackHeight = this.stack;
        code.emit("j break_target_" + b.breakLink.uniqueId);
        return null;
    }

    @Override
    public Object visit(Switch s){
        s.exp.accept(this);
        pop(s.exp.type,"$t0");
        for(Statement stmt : s.stmts){
            if(stmt instanceof Case){
                code.emit("li $t1," +stmt.accept(this));
                code.emit("  beq $t0,$t1,case_label_" +stmt.uniqueId);
            }else if(stmt instanceof Default){
                code.emit("j " +stmt.uniqueId);
            }
        }
        s.stmts.accept(this);
        code.emit("break_target_" +s.uniqueId+ ":");
        return null;
    }

    @Override
    public Object visit(StatementList s){
        int tmp_stack = this.stack;
        //System.out.println(this.stack);
        Object visit = visit((AstList)s);
//        System.out.println(this.stack);
//        System.out.println(tmp_stack);
//        System.out.println(this.stack - tmp_stack);
        pop(this.stack - tmp_stack);
        return visit;

    }

    @Override
    public Object visit(Case c){
        code.emit("case_label_" +c.uniqueId+ ":");
        return null;
    }

    @Override
    public Object visit(Default d){
        code.emit("case_label_" +d.uniqueId+ ":");
        return null;
    }

    @Override
    public Object visit(Label l){
        code.emit("case_label_" +l.uniqueId+ ":");
        return null;
    }

    /////////////////////////////////////
    // Expressions
    /////////////////////////////////////

    @Override
    public Object visit(IntegerLiteral i){
        code.emit("  li $t0," +i.val);
        push(i.type,"$t0");
        return null;
    }

    @Override
    public Object visit(StringLiteral s){
        code.emit("  la $t0, strLit_" +s.uniqueId);
        push(s.type,"$t0");
        return null;
    }

    @Override
    public Object visit(True t){
        code.emit("  li $t0,1");
        push(t.type,"$t0");
        return null;
    }

    @Override
    public Object visit(False f){
        push(f.type,"$0");
        return null;
    }

    @Override
    public Object visit(Null n){
        push(n.type,"$0");
        return null;
    }

    @Override
    public Object visit(This t){
        push(t.type,"$s2");
        return null;
    }

    @Override
    public Object visit(Super s){
        push(s.type,"$s2");
        return null;
    }

    @Override
    public Object visit(IdentifierExp i){
        int address = this.stack - i.link.offset;
        code.emit("  lw $t0," +address+ "($sp)");
        push(i.type,"$t0");
        return null;
    }


    //////////////////////////////////////
    // Binary Expressions
    /////////////////////////////////////

    @Override
    public Object visit(Plus p){
        Object visit = visit((BinExp)p);
        pop(p.type,"$t2");
        pop(p.type,"$t1");
        code.emit("  addu $t0,$t1,$t2");
        push(p.type,"$t0");
        return visit;
    }

    @Override
    public Object visit(Minus m){
        Object visit = visit((BinExp)m);
        pop(m.type,"$t2");
        pop(m.type,"$t1");
        code.emit("  subu $t0,$t1,$t2");
        push(m.type,"$t0");
        return visit;
    }

    @Override
    public Object visit(Times t){
        Object visit = visit((BinExp)t);
        pop(t.type,"$t2");
        pop(t.type,"$t1");
        code.emit("  mul $t0,$t1,$t2");
        push(t.type,"$t0");
        return visit;
    }

    @Override
    public Object visit(Divide d){
        Object visit = visit((BinExp)d);
        code.emit("  jal divide");
        return visit;
    }

    @Override
    public Object visit(Remainder r){
        Object visit = visit((BinExp)r);
        code.emit("  jal remainder");
        return visit;
    }

    @Override
    public Object visit(And a){
        Object visit = visit((BinExp)a);
        code.emit("  lw $t0,($sp)");
        code.emit("  beq $t0,$0,skip_" + a.uniqueId);
        pop(a.type,"$t0");
        code.emit(a,"skip_" + a.uniqueId + ":");
        return visit;
    }

    @Override
    public Object visit(Equals e) {
        Object visit = visit((BinExp) e);
        pop(e.right.type, "$t2");
        pop(e.left.type,"$t1");
        code.emit("seq $t0,$t1,$t2");
        push(e.type,"$t0");
        return visit;
    }

    @Override
    public Object visit(LessThan l){
        Object visit = visit((BinExp)l);
        pop(l.right.type,"$t2");
        pop(l.left.type,"$t1");
        code.emit("slt $t0,$t1,$t2");
        push(l.type,"$t0");
        return visit;
    }

    @Override
    public Object visit(GreaterThan g){
        Object visit = visit((BinExp)g);
        pop(g.right.type,"$t2");
        pop(g.left.type,"$t1");
        code.emit("  sgt $t0,$t1,$t2");
        push(g.type,"$t0");
        return visit;
    }

    @Override
    public Object visit(Or o){
        Object visit = visit((BinExp)o);
        code.emit("lw $t0,($sp)");
        code.emit("bne $t0,$0,skip_" + o.uniqueId);
        pop(o.type,"$t0");
        code.emit("skip_" + o.uniqueId + ":");
        return visit;
    }

    /////////////////////////////
    // Other Expressions
    //////////////////////////////

    @Override
    public Object visit(ArrayLookup a){
        a.arrExp.accept(this);
        a.idxExp.accept(this);
        pop(a.type,"$t0");
        pop(a.type,"$t1");
        outOfBounds("$t0","$t1");
        arrayLoad("$t1","$t0","$t1");
        code.emit("lw $t0,($t1)");
        push(a.type,"$t0");
        return null;
    }

    @Override
    public Object visit(InstVarAccess i){
        i.exp.accept(this);
        pop(i.type,"$t0");
        nullPointerException("$t0");
        code.emit("lw $t0," +i.varDec.offset+ "($s2)");
        push(i.varDec.type,"$t0");
        return null;
    }

    @Override
    public Object visit(Cast c){
        c.exp.accept(this);
        code.emit("la $t0, CLASS_" +c.castType.vtableName());
        code.emit("la $t1, END_CLASS_" +c.castType.vtableName());
        code.emit("jal checkCast");
        return null;
    }

    @Override
    public Object visit(InstanceOf i){
        i.exp.accept(this);
        code.emit("la $t0, CLASS_" +i.checkType.vtableName());
        code.emit("la $t1, END_CLASS_" +i.checkType.vtableName());
        code.emit("jal instanceOf");
        return null;
    }

    @Override
    public Object visit(NewObject n){
        code.emit("  li $s6," + (n.objType.link.numDataInstVars+1));
        code.emit("  li $s7," + (n.objType.link.numObjInstVars));
        code.emit("  jal newObject");
        code.emit("  la $t0,CLASS_" +n.type.vtableName());
        code.emit("  sw $t0,-12($s7)");
        this.stack += 4;
        return null;
    }

    @Override
    public Object visit(NewArray n){
        n.objType.accept(this);
        n.sizeExp.accept(this);
        code.emit("li $s6,1");
        pop(n.sizeExp.type,"$s7");
        code.emit("jal newObject");
        //do i need to make a method for T[]
        code.emit("la $t0, CLASS_ARRAY_" +n.objType.vtableName());
        code.emit("sw $t0, -12($s7)");
        this.stack += 4;
        return null;
    }

    @Override
    public Object visit(Not n){
        Object visit = visit((UnExp)n);
        code.emit("lw $t0,($sp)");
        code.emit("xor $t0,$t0,1");
        code.emit("sw $t0,($sp)");
        return visit;
    }

    @Override
    public Object visit(ArrayLength a){
        Object visit = visit((UnExp)a);
        pop(a.exp.type,"$t0");
        nullPointerException("$t0");
        code.emit("lw $t0,-4($t0)");
        push(a.exp.type,"$t0");
        return visit;
    }


    ////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////
    public void push(Type t, String reg){
        //code.comment(t,"Start push");
        if(t.isInt()){
            code.emit("  subu $sp,$sp,8");
            code.emit("  sw $s5,4($sp)");
            code.emit("  sw " +reg+",($sp)");
            stack += 8;
        }else{
            code.emit("  subu $sp,$sp,4");
            code.emit("  sw " +reg+ ",($sp)");
            stack += 4;
        }
        //code.comment(t,"End push");
    }

    public void pop(Type t, String reg){
        //code.comment(t,"Start pop");
        if(t.isInt()){
            code.emit("  lw " +reg+ ",($sp)");
            code.emit("  addu $sp,$sp,8");
            stack -= 8;
        }else{
            code.emit("  lw " +reg+ ",($sp)");
            code.emit("  addu $sp,$sp,4");
            stack -= 4;
        }
        //code.comment(t,"End pop");
    }

    public void pop(int size){
        code.emit("  addu $sp,$sp," +size);
        stack -= size;
    }

    private void swap(String reg, String mem){
        code.emit("  lw $t0," +mem);
        code.emit("  sw " +reg+ "," +mem);
        code.emit("  move " +reg+ ",$t0");
    }

    private void outOfBounds(String reg1, String reg2){
        nullPointerException(reg1);
        code.emit("lw $t3, -4(" +reg1+ ")");
        code.emit("bgeu " +reg2+ ",$t3,arrayIndexOutOfBounds");
    }

    private void arrayLoad(String reg1, String reg2, String reg3){
        code.emit("sll " +reg1+ "," +reg3+ ",2");
        code.emit("addu " +reg1+ "," +reg1+ "," + reg2);
    }

    private void nullPointerException(String reg1){
        code.emit("  beq " +reg1+ ",$0,nullPtrException");
    }

}

