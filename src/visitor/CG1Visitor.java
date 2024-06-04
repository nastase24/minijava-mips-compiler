package visitor;

import syntaxtree.*;

import java.util.*;

import errorMsg.*;
import java.io.*;
import java.awt.Point;

//the purpose here is to annotate things with their offsets:
// - formal parameters, with respect to the (callee) frame
// - instance variables, with respect to their slot in the object
// - methods, with respect to their slot in the v-table
// - (Optionally) generate all v-tables.
public class CG1Visitor extends Visitor
{

    // IO stream to emit code.
    CodeStream code;

    //used to track the object class, since that's
    //the root of the inheritance tree.
    private ClassDecl object;

    //current class we're visiting.
    private ClassDecl currentClass;
    private MethodDecl currentMethod;
    ////////////////////////////////////////////////////////////
    // This is used for doing your own VMT generation.
    // Otherwise you don't need it.
    ////////////////////////////////////////////////////////////
    // to collect the array types that are referenced in the code
    private HashSet<ArrayType> arrayTypes;

    public CG1Visitor(ErrorMsg e, PrintStream out, ClassDecl Object)
    {
        code = new CodeStream(out, e);
        object = Object;
        arrayTypes = new HashSet<ArrayType>();
    }

    public Object visit(Program p)
    {
        // comment the following line out if 
        // you are doing your own vtable generation:
        VtableGenerator.generate(p, code);
        p.classDecls.accept(this);
        p.dummyNodes.accept(this);
        return null;
    }

    @Override
    public Object visit(ClassDecl c){
        currentClass = c;
        //currentClass.numDataInstVars = 0;
        c.decls.accept(this);
        if(this.currentClass.superLink != null){
            this.currentClass.numDataInstVars = this.currentClass.superLink.numDataInstVars;
            this.currentClass.numObjInstVars = this.currentClass.superLink.numObjInstVars;
        }
        //c.decls.accept(this);
        c.subclasses.accept(this);
        return null;
    }

    @Override
    public Object visit(MethodDeclVoid m){
        Object visit = visit((MethodDecl)m);
        this.currentMethod = m;
        int formals = this.currentMethod.formals.size();
        int offset_counter = 0;
        for(int i = formals-1; i >= 0; i--){
            this.currentMethod.formals.get(i).offset = 4 * offset_counter;
            if(this.currentMethod.formals.get(i).type.isInt()){
                offset_counter += 2;
            }else{
                offset_counter += 1;
            }
        }
        return visit;
    }

    @Override
    public Object visit(MethodDeclNonVoid m){
        m.formals.accept(this);
        m.stmts.accept(this);
        m.rtnType.accept(this);
        m.rtnExp.accept(this);
        this.currentMethod = m;
        int formals = this.currentMethod.formals.size();
        int offset_counter = 0;
        for(int i = formals-1; i >= 0; i--){
            this.currentMethod.formals.get(i).offset = 4 * offset_counter;
            if(this.currentMethod.formals.get(i).type.isInt()){
                offset_counter += 2;
            }else{
                offset_counter += 1;
            }
        }
        return null;
    }

    @Override
    public Object visit(InstVarDecl i){
        Object visit = visit((VarDecl)i);
        if(i.type.isInt() || i.type.isBoolean()){
            i.offset = -16 - (this.currentClass.numDataInstVars * 4);
            this.currentClass.numDataInstVars += 1;
        }else{
            i.offset = (this.currentClass.numObjInstVars * 4);
            this.currentClass.numObjInstVars +=1;
        }
        return visit;
    }

//    @Override
//    public Object visit(NewArray n){
//        arrayTypes.add(new ArrayType(n.pos,n.type));
//        return null;
//    }
//
//    @Override
//    public Object visit(ArrayType a){
//        arrayTypes.add(a);
//        return null;
//    }

    /////////////////////////////////////////////////////////////
    //
    // helper methods for generating VMTs
    //
    /////////////////////////////////////////////////////////////

    /**
     * emits the name of the class as a sequence of bytes.
     * This is used by the default implementation of toString(),
     * So, we need it as part of the VMT.
     */
    public void emitPrintName(AstNode n, String name)
    {
        // emit padding bytes for string
        for(int i = name.length()%4; 0 < i && i < 4; i++)
        {
            code.emit(n, "  .byte 0");
        }

        //print out the first character with the first bit set to 1
        //This allows the toString method to know that
        //we've reached the first character of the string.
        code.emit(n, "  .byte "+ ((int)name.charAt(0) | 0x80) +
                     " # '"+name.charAt(0)+"' with high bit set");
        for(char c : name.substring(1).toCharArray())
        {
            code.emit(n, "  .byte "+(int)c+ " # '"+c+"'");
        }
    }

    /**
     * Emit VMT for arrays.
     * Since arrays can't override methods, 
     * they have the same VMT as Object.
     */
    public void emitArrayTypeVtables()
    {
        // emit object arrays before int and bool arrays (if they exists)
        // because the garbage collector
        // needs to know if it's a data array.
        ArrayType iarr = null;
        ArrayType barr = null;
        for(ArrayType at : arrayTypes)
        {
            if(at.baseType.isInt())
            {
                iarr = at;
            }
            else if(at.baseType.isBoolean())
            {
                barr = at;
            }
            else
            {
                emitArray(at);
            }
        }
        code.emit(new IntegerType(-1), "dataArrayVTableStart:");
        if(iarr != null)
        {
            emitArray(iarr);
        }
        if(barr != null)
        {
            emitArray(barr);
        }
    }

    public void emitArray(ArrayType at)
    {
        emitPrintName(at, at.typeName());
        code.emit(at, "CLASS_"+at.vtableName()+":");
        code.emit(at, "  .word fcn_hashCode_Object");
        code.emit(at, "  .word fcn_toString_Object");
        code.emit(at, "  .word fcn_equals_Object");
        code.emit(at, "END_CLASS_"+at.vtableName()+":");
    }
}

