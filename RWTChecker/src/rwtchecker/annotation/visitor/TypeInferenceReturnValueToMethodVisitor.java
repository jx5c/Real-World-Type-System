package rwtchecker.annotation.visitor;

import org.eclipse.jdt.core.dom.*;

import rwtchecker.annotation.RWTAnnotation;
import rwtchecker.views.RWTView;

public class TypeInferenceReturnValueToMethodVisitor extends TypeInferenceVisitor {
	
	private CompilationUnit compilationUnit;
	
	private boolean insideMethodBody = false;
	private int returnStatementCount = 0;
	private ReturnStatement lastRS = null;
	
	
	public TypeInferenceReturnValueToMethodVisitor(CompilationUnit compilationUnit) {
		super(compilationUnit);
	}
	
	@Override
	public void preVisit(ASTNode node){
		if (node instanceof MethodDeclaration){
			insideMethodBody = true;
		}
		
		if (node instanceof ReturnStatement){
			if(insideMethodBody){
//				ReturnStatement rs = (ReturnStatement)node;
//				Expression exp = rs.getExpression();
				lastRS = (ReturnStatement)node;
				returnStatementCount++;
			}
		}
	}
	
	@Override
	public void postVisit(ASTNode node){
		if (node instanceof MethodDeclaration){
			MethodDeclaration methodDecl = (MethodDeclaration)node;
			insideMethodBody = false;
			if(returnStatementCount==1){
				//propagate the return type
				propagateReturnType(lastRS.getExpression(), methodDecl);
				returnStatementCount = 0;
			}
		}
	}
	
	private void propagateReturnType(Expression exp, MethodDeclaration methodDecl){
		String RWType = this.getRWTypeForVarLikeExp(exp);
		if(RWType.length()>0){
			System.out.println("Propagation: Method return type propagation here: "+RWType);
			RWTView.saveJAVADocElementToFile(methodDecl, RWTAnnotation.Return, null, RWType, true);	
		}
	}	
}