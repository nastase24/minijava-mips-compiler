package main;

import java.io.*;

import parse.*;
import syntaxtree.*;
import treedisplay.*;
import visitor.*;
import errorMsg.*;
import java.util.*;
import java.awt.*;

public class Main5
{

    private static final boolean SCANNER_ONLY = false;

    private static final int SILENT = 0;
    private static final int WINDOW = 1;
    private static final int PRINT = 2;

    private static final int DEFAULT_PASS_LIMIT = 12;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final Color defaultColor()
    {
        return new Color(0xbb,0x99,0xff);
    }

    private int mode;
    private boolean prettyPrint = false;
    private boolean verbosePrettyPrint = false;
    private int pass;
    private boolean sendOutputToConsole = false;
    private int fontSize;
    private Color color;
    String inputFileName;
    private String outputFileName;
    private String appendFileName;
    private InputStream inputStream;
    private PrintStream outputStream;
    private InputStream appendStream;
    ErrorMsg errorMsg;

    private boolean gcReport = false;

    public static void main(String args[])
    {
        Main5 mainObj = new Main5();
        mainObj.parseCommandLine(args);
        mainObj.errorMsg = new ErrorMsg(mainObj.inputFileName);
        mainObj.execMain(false);
    }

    void execMain(boolean useTerminal)
    {
        try
        {
            this.openFiles();
            AstNode ast = this.parseFile();
            HashMap<String,ClassDecl> env = this.semanticPhase1(ast);
            this.semanticPhase2(ast, env);
            this.semanticPhase3(ast, env);
            this.semanticPhase4(ast, env);
            this.semanticPhase5(ast, env);
            this.cgPhase1(ast, env);
            this.cgPhase2(ast);
            this.cgPhase3(ast, false);
            this.appendFile();
            this.printSummaryMessage();
            this.displayAst(ast);
            this.closeFiles();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            exitError("Unexpected exception: "+e);
        }
    }

    Main5()
    {
        mode = SILENT;
        pass = DEFAULT_PASS_LIMIT;
        fontSize = DEFAULT_FONT_SIZE;
        inputFileName = null;
        outputFileName = null;
        appendFileName = null;
        errorMsg = null;
    }

    void parseCommandLine(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.length() == 0)
            {
                exitError("Illegal argument: '"+arg+"'");
            }
            else if (arg.charAt(0) == '-')
            {
                if (arg.startsWith("-w"))
                {
                    mode = WINDOW;
                    int endIntPos =  arg.indexOf(":", 2);
                    int startColorPos = arg.length();
                    if (endIntPos < 0)
                    {
                        endIntPos = arg.length();
                    }
                    else
                    {
                        startColorPos = endIntPos + 1;
                    }
                    String intPart = arg.substring(2, endIntPos);
                    String colorPart = arg.substring(startColorPos);
                    try
                    {
                        fontSize = Integer.parseInt(intPart);
                        if (fontSize <= 0) fontSize = DEFAULT_FONT_SIZE;
                    }
                    catch (NumberFormatException nfx)
                    {
                        fontSize = DEFAULT_FONT_SIZE;
                    }
                    try
                    {
                        int colorNum = Integer.parseInt(colorPart, 16);
                        color = new Color(colorNum);
                    }
                    catch (NumberFormatException nfx)
                    {
                        color = defaultColor();
                    }
                }
                else
                {
                    switch (arg.substring(1))
                    {
                    case "1":
                        pass = 1;
                        break;
                    case "2":
                        pass = 2;
                        break;
                    case "3":
                        pass = 3;
                        break;
                    case "4":
                        pass = 4;
                        break;
                    case "5":
                        pass = 5;
                        break;
                    case "6":
                        pass = 6;
                        break;
                    case "7":
                        pass = 7;
                        break;
                    case "8":
                        pass = 8;
                        break;
                    case "9":
                        pass = 9;
                        break;
                    case "10":
                        pass = 10;
                        break;
                    case "11":
                        pass = 11;
                        break;
                    case "a":
                        i++;
                        if (i >= args.length) exitError ("No file name after '-a'");
                        appendFileName = args[i];
                        break;
                    case "c":
                        // send output to console
                        sendOutputToConsole = true;
                        break;
                    case "p":
                        mode = PRINT;
                        break;
                    case "pp":
                        mode = PRINT;
                        prettyPrint = true;
                        break;
                    case "vpp":
                        mode = PRINT;
                        prettyPrint = true;
                        verbosePrettyPrint = true;
                        break;
                    case "w":
                        mode = WINDOW;
                        fontSize = DEFAULT_FONT_SIZE;
                        color = defaultColor();
                        break;
                    case "o":
                        i++;
                        if (i >= args.length)
                        {
                            exitError("No output file specified after '-o'");
                        }
                        if (outputFileName != null)
                            exitError("Too many output file names given");
                        outputFileName = args[i];
                        break;
                    case "-gc":
                        gcReport = true;
                        break;
                    default:
                        exitError("Illegal switch: '"+arg+"'");
                        break;
                    }
                }
            }
            else
            {
                if (inputFileName != null) exitError("Too many input file names given");
                inputFileName = arg;
            }
        }
        if (inputFileName == null) exitError("No input file name given");
    }

    private void openFiles()
    {

        try
        {
            inputStream = new FileInputStream(inputFileName);
        }
        catch (FileNotFoundException e)
        {
            exitError("File not found: " + inputFileName);
        }

        // open output stream:
        // - if "-c" switch given, then console, otherwise,
        // - if "-o" switch given, the argument following the "-o", otherwise
        // - the input file name with any extension removed, and with ".asm" appended
        outputStream = System.out;
        if (!sendOutputToConsole)
        {
            if (outputFileName == null)
            {
                outputFileName = inputFileName;
                int periodSpot = outputFileName.lastIndexOf(".");
                if (periodSpot >= 0)
                {
                    outputFileName = outputFileName.substring(0,periodSpot);
                }
                outputFileName += ".asm";
            }
            try
            {
                OutputStream os = new FileOutputStream(outputFileName);
                outputStream = new PrintStream(os);
            }
            catch (IOException e)
            {
                exitError("Error opening output file: " + outputFileName);
            }
        }
        if (appendFileName != null)
        {
            try
            {
                appendStream = new FileInputStream(appendFileName);
            }
            catch (FileNotFoundException e)
            {
                exitError("File not found: " + appendFileName);
            }
        }
    }

    private AstNode parseFile()
    {

        if (SCANNER_ONLY)
        {
            // parse the original input, sending its data to the pipe
            new MJScannerParseTable(new MJScanner(errorMsg, System.out)).parse(inputStream,0,false);
            return null;
        }
        else if (MJGrammar.FILTER_GRAMMAR)
        {

            try
            {

                ByteArrayOutputStream os = new ByteArrayOutputStream(1000000);
                // parse the original input, sending its data to the pipe
                ErrorMsg scannerErrorObject = new ErrorMsg(errorMsg.getFileName());
                MJScanner scanner = new MJScanner(scannerErrorObject, os);
                new MJScannerParseTable(scanner).parse(inputStream,0,false);

                if (scannerErrorObject.anyErrors)
                {
                    errorMsg.error(-1, "Error detected during scanning");
                }
                os.close();

                // create the semantic action object
                MJGrammar mj = new MJGrammar(errorMsg);

                ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

                // parse the filtered text
                new MJGrammarParseTable(mj).parse(is,0,false);
                is.close();

                // return the result of the parse
                return mj.parseResult();
            }
            catch (IOException iox)
            {
                System.err.println("Internal piping error.");
                return null;
            }
        }
        else
        {
            // create the semantic action object
            MJGrammar mj = new MJGrammar(errorMsg);

            // parse the input
            new MJGrammarParseTable(mj).parse(inputStream,0,false);

            // return the result of the parse
            return mj.parseResult();
        }
    }

    public HashMap<String,ClassDecl> semanticPhase1(AstNode ast)
    {
        if (!errorMsg.anyErrors && ast != null && pass >= 2)
        {
            Sem1Visitor vis = new Sem1Visitor(errorMsg);
            ast.accept(vis);
            return vis.getEnv();
        }
        else
        {
            return null;
        }
    }

    public void semanticPhase2(AstNode ast, HashMap<String,ClassDecl> env)
    {
        if (!errorMsg.anyErrors && ast != null && pass >= 3)
        {
            ast.accept(new Sem2Visitor(env, errorMsg));
        }
    }

    public void semanticPhase3(AstNode ast, HashMap<String,ClassDecl> env)
    {
        if (!errorMsg.anyErrors && ast != null && pass >= 4)
        {
            ast.accept(new Sem3Visitor(env, errorMsg));
        }
    }

    public void semanticPhase4(AstNode ast, HashMap<String,ClassDecl> env)
    {
        if (!errorMsg.anyErrors && ast != null && pass >= 5)
        {
            ast.accept(new Sem4Visitor(env, errorMsg));
        }
    }

    public void semanticPhase5(AstNode ast, HashMap<String,ClassDecl> env)
    {
        if (!errorMsg.anyErrors && ast != null && pass >= 6)
        {
            ast.accept(new Sem5Visitor(errorMsg));
        }
    }

    public void cgPhase1(AstNode ast, HashMap<String,ClassDecl> env)
    {
        if (!errorMsg.anyErrors && ast != null && pass >= 7)
        {
            ast.accept(new CG1Visitor(errorMsg, 
                        outputStream, env.get("Object")));
        }
    }

    public void cgPhase2(AstNode ast)
    {
        if (!errorMsg.anyErrors && ast != null && pass >= 8)
        {
            ast.accept(new CG2Visitor(errorMsg, outputStream));
        }
    }

    public void cgPhase3(AstNode ast, boolean gcReport)
    {
        if (!errorMsg.anyErrors && ast != null && pass >= 9)
        {
            ast.accept(new CG3Visitor(errorMsg, outputStream));
        }
    }

    public void appendFile()
    {
        if (!errorMsg.anyErrors && appendStream != null && pass >= 10)
        {
            try
            {
                for (;;)
                {
                    int b = appendStream.read();
                    if (b < 0) break;
                    outputStream.write(b);
                }
            }
            catch (EOFException iox)
            {
                System.err.println("EOF exception");
            }
            catch (IOException iox)
            {
                exitError("Error attempting to process append-file "+appendFileName);
            }
        }
    }

    public void closeFiles()
    {
        try
        {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (appendStream != null) appendStream.close();
        }
        catch (IOException iox)
        {
            exitError("Error closing files.");
        }
    }

    public void printSummaryMessage()
    {
        if (errorMsg.anyErrors)
        {
            System.err.println("Compilation failed for "+inputFileName+".");
        }
        else
        {
            System.err.println("Compilation successful for "+inputFileName+".");
        }
    }

    public void displayAst(AstNode ast)
    {
        if (ast == null) return;
        if (mode == SILENT) return;
        String title =
            (mode == PRINT)
            ? null : ("abstract syntax tree for "+inputFileName);
        if (mode == PRINT)
        {
            if (prettyPrint)
            {
                PrettyPrintVisitor ppv =
                    new PrettyPrintVisitor(true, true, verbosePrettyPrint,
                                           new PrintStream(outputStream));
                ast.accept(ppv);
            }
            else
            {
                TreeDrawer myObj = new TreeDrawer(ast, null);
                myObj.printToStream(new PrintStream(outputStream));
                return;
            }
        }
        else
        {
            TreeDisplayFrame myObj =
                new TreeDisplayFrame(ast, title, errorMsg, fontSize, color);
            myObj.setVisible(true);
        }
    }

    public static void exitError(String msg)
    {
        System.err.println(msg);
        exit(1);
    }
    public static void exit(int code)
    {
        System.exit(code);
    }
}
