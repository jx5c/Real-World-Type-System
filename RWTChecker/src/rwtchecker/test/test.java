package rwtchecker.test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

public class test{
	public static void main(String[] args)
	{
	  StringWriter classWriter = new StringWriter();
	  PrintWriter classStream = new PrintWriter( classWriter );
	  classStream.println("package test;                       ");
	  classStream.println("                                    ");
	  classStream.println("/**                                 ");
	  classStream.println(" * This is a test comment           ");
	  classStream.println(" */                                 ");
	  classStream.println("public class TestClass { ");
	  classStream.println("pubic static void main(String[] args)");
	  classStream.println(" // This is a test comment of variable");
	  classStream.println("{ int x = 0 }");
	  classStream.println("}");
	  
	  ASTParser javaParser = ASTParser.newParser(AST.JLS3);
	  javaParser.setEnvironment(null, null, null, true);
	  javaParser.setStatementsRecovery(true);
	  
	  Map<String,String> compilerOptions = JavaCore.getDefaultOptions();
	  compilerOptions.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_7);
	  compilerOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7);
	  compilerOptions.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
	  compilerOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
	  javaParser.setCompilerOptions(compilerOptions);    
	  javaParser.setSource( classWriter.toString().toCharArray() );
	  
	  javaParser.createAST(null).accept( new ASTVisitor(true) {
	    @Override
	    public boolean visit(Javadoc node)
	    {
	      System.out.println( "Found javadoc: " + node );
	      return true;
	    }
	    @Override
	    public boolean visit(LineComment node) {

//	        int startLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
//	        String lineComment = source[startLineNumber].trim();

	        System.out.println("line comments is "+node);

	        return true;
	    }
	    
	    @Override
	    public boolean visit(BlockComment node) {

//	        int startLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
//	        String lineComment = source[startLineNumber].trim();

	        System.out.println("Block comments is "+node);

	        return true;
	    }

	    @Override
	    public boolean visit(MethodDeclaration node)
	    {
	      System.out.println( "Found Type " + node.getName().getFullyQualifiedName() + " -> javadoc: " + node.getJavadoc() );
	      System.out.println(node.getBody());
	      System.out.println();
	      return super.visit(node);
	    }
	  });
	}
}
