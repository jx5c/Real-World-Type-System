package rwtchecker.annotation.visitor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.dom.*;

import rwtchecker.CM.CMType;
import rwtchecker.annotation.AnnotationLearner;
import rwtchecker.annotation.FileAnnotations;
import rwtchecker.annotation.RWTAnnotation;
import rwtchecker.popup.actions.TypePropagationProjectInNavigator;
import rwtchecker.popup.actions.TypePropagationProjectInNavigator.PropagatedMethodsSigniture;
import rwtchecker.util.CMModelUtil;
import rwtchecker.views.RWTView;

public class TypeInferenceParameterToArgumentsVisitor extends TypeInferenceVisitor {
	
	private CompilationUnit compilationUnit;
	
	TypePropagationProjectInNavigator typePropagation = new TypePropagationProjectInNavigator(new File("E:\\typePropagation.xml"));
	
	private boolean insideMethodBody = false;
	private int returnStatementCount = 0;
	private ReturnStatement lastRS = null;	
	
	//for learner
	private Queue<String> current_itemset = new LinkedList<String>();
	private boolean learning = false;
	private boolean hasRWType = false;
	String itemsets_lhv = "";
	String itemsets_rhv = "";
	String insideKnownMethod = "";
	AnnotationLearner learner = null;
	
	public TypeInferenceParameterToArgumentsVisitor(CompilationUnit compilationUnit) {
		super(compilationUnit);
		learner = AnnotationLearner.getInstance();
		learner.loadConfirmedData("e:\\confirmed_annotations.txt");
	}
	
	@Override
	public void preVisit(ASTNode node){
		if (node instanceof MethodDeclaration){
			MethodDeclaration methodDecl = (MethodDeclaration)node;
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
		
		//for learning
		if(node instanceof Assignment || node instanceof VariableDeclarationStatement){
			learning = true;
			hasRWType = false;
			current_itemset.clear();
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
		
		if(node instanceof Assignment){
			Assignment assignmentNode = (Assignment)node;
			Expression leftExp = assignmentNode.getLeftHandSide();
			Expression rightExp = assignmentNode.getRightHandSide();
			//propagation type: right side is method invocation, left side is variable; propagate from right to left
			propagateRightToLeft(leftExp,rightExp);
			//propagation type: left side is field, right side is input variable, and the method has only one statement
			propagateFieldToParameter(assignmentNode);
		}
		
		if(node instanceof VariableDeclarationStatement){
			VariableDeclarationStatement variableDeclarationStatementNode = (VariableDeclarationStatement)node;
			for (Iterator iter = variableDeclarationStatementNode.fragments().iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
				if(fragment.getInitializer()==null){
					continue;
				}
				propagateRightToLeft(fragment.getName(),fragment.getInitializer());
			}
		}
		
		//for learner
		if(node instanceof MethodInvocation){
			String methodkey = ((MethodInvocation)node).resolveMethodBinding().getKey();
			if(insideKnownMethod.equals(methodkey)){
				//end of the current known method
				learning = true;
			}
		}
		if(learning){
			if (node instanceof NumberLiteral){
				current_itemset.add(((NumberLiteral)node).toString());
			}
			if(node instanceof InfixExpression){
				current_itemset.add(((InfixExpression)node).getOperator().toString());
			}
			if(node instanceof SimpleName){
				IBinding binding= ((SimpleName)node).resolveBinding();
				if(binding !=null){
					if (binding.getKind() == IBinding.VARIABLE) {
						String rwtype = getRWTypeForVarLikeExp((SimpleName)node);
						if(rwtype.length()>0&& !rwtype.equals(CMType.GenericMethod)){
							current_itemset.add(rwtype);
							hasRWType = true;
						}else{
							current_itemset.add("var");
						}
					}
				}
			}
			if(node instanceof Assignment){
				if(hasRWType){
					if(current_itemset.size()>=2){
						current_itemset.poll();
						StringBuffer temp = new StringBuffer();
						for (String item : current_itemset){
							temp.append(item);
							//separator
							temp.append("&&");
						}
						itemsets_rhv = temp.toString();
						
						if(learner.confirmedAnnotationMap.containsKey(itemsets_rhv)){
							Expression leftExp = ((Assignment)node).getLeftHandSide();
							propagateLearnedType(leftExp);
						}
					}	
				}
				learning = false;
				hasRWType = false;
			}
			
			if(node instanceof VariableDeclarationStatement){
				if(hasRWType){
					if(current_itemset.size()>=2){
						VariableDeclarationStatement vDeclSt = (VariableDeclarationStatement)node;
						for (Iterator iter = vDeclSt.fragments().iterator(); iter.hasNext();iter.next()) {
							current_itemset.poll();
						}
						StringBuffer temp = new StringBuffer();
						for (String item : current_itemset){
							temp.append(item);
							//separator
							temp.append("&&");
						}
						itemsets_rhv = temp.toString();
						if(learner.confirmedAnnotationMap.containsKey(itemsets_rhv)){
							for (Iterator iter = vDeclSt.fragments().iterator(); iter.hasNext();) {
								VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
								SimpleName leftSN = (SimpleName)(fragment.getName());
								propagateLearnedType(leftSN);
							}
						}
					}	
				}
				learning = false;
				hasRWType = false;
			}
		}
	}
	
	private void propagateLearnedType(Expression exp){
		String lhv_rwtype = getRWTypeForVarLikeExp(exp);
		String typeToLearn = learner.confirmedAnnotationMap.get(itemsets_rhv);
		
		if(lhv_rwtype.length()>0 && !lhv_rwtype.equals("var") && !lhv_rwtype.equals(typeToLearn)){
			System.out.println("Propagation: learning failed: "+exp);
		}else{
			System.out.println("Propagation: learning for type "+typeToLearn);
			propagateTypeForVars(exp, typeToLearn);
		}
	}
	
	private void propagateFieldToParameter(Assignment assignmentNode){
		ASTNode parentNode = assignmentNode.getParent();
        while(!(parentNode instanceof MethodDeclaration) && !(parentNode instanceof TypeDeclaration)){
        	parentNode = parentNode.getParent();
        }
		if(parentNode instanceof MethodDeclaration){
			MethodDeclaration md = (MethodDeclaration)parentNode;
			if(md.getBody().statements().size()==1){
				//only one statement
				Expression leftExp = assignmentNode.getLeftHandSide();
				Expression rightExp = assignmentNode.getRightHandSide();
				if(leftExp instanceof FieldAccess || leftExp instanceof SimpleName){
					String leftRWType = getRWTypeForVarLikeExp(leftExp);
					if(leftRWType.length()>0){
						if(rightExp instanceof SimpleName){
							String rightRWType = getRWTypeForVarLikeExp(rightExp);
							if(rightRWType.equals(leftRWType)){
								return;
							}else if(rightRWType.length()==0){
								propagateTypeForVars(rightExp,leftRWType);
							}
						}
					}
				}
			}
		}
	}
	
	private void propagateRightToLeft(Expression leftExp, Expression rightExp ){
		if (rightExp instanceof MethodInvocation){
			MethodInvocation methodInvocationNode = (MethodInvocation)rightExp;
			String methodKey = methodInvocationNode.resolveMethodBinding().getKey();
			if (typePropagation.learningPropagationMap.containsKey(methodKey)){
				boolean inconsistentType = false;
				ArrayList<PropagatedMethodsSigniture> propagatedMethodList = typePropagation.learningPropagationMap.get(methodKey);
				for(PropagatedMethodsSigniture propagatedMethod : propagatedMethodList){
					String rwtypeToPropagated = propagatedMethod.getReturnType();
					if (rwtypeToPropagated.length()==0){
						//save some time
						return;
					}
					for (int i=0;i<methodInvocationNode.arguments().size();i++){
						Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
						String rwtype = getRWTypeForVarLikeExp(exp);
						if(!rwtype.equals(propagatedMethod.paramTypes.get(i))){
							inconsistentType = true;
							break;
						}
					}
					if (!inconsistentType){
						//no consistent type, propagation to the left side
						propagateTypeForVars(leftExp, rwtypeToPropagated);
					}	
				}
			}
		}
	}
		
	private void propagateReturnType(Expression exp, MethodDeclaration methodDecl){
		String RWType = getRWTypeForVarLikeExp(exp);
		if(RWType.length()>0){
			System.out.println("Propagation: Method return type propagation here: "+RWType);
			RWTView.saveJAVADocElementToFile(methodDecl, RWTAnnotation.Return, null, RWType, true);	
		}
	}
	
	
	@Override
	public boolean visit(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
		if (typePropagation.paramPropagationMap.containsKey(methodDeclKey)){
			propagateParamTypes(methodInvocationNode.arguments(), typePropagation.paramPropagationMap.get(methodDeclKey));
		}
		
		//for annotation learner
		String rwtype = FileAnnotations.getRWTypeForMethod(iMethodBinding);
		if(rwtype.length()>0){
			current_itemset.add(rwtype);
			hasRWType = true;
			insideKnownMethod = iMethodBinding.getKey();
			//stop learning for this method
			learning = false;
		}
		else{
			if(learning){
				current_itemset.add(iMethodBinding.getKey());	
			}
		}
		return true;
	}
	

	@Override
	public boolean visit(ClassInstanceCreation cic){
		IMethodBinding iMethodBinding = cic.resolveConstructorBinding();
		String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
		if (typePropagation.paramPropagationMap.containsKey(methodDeclKey)){
			propagateParamTypes(cic.arguments(), typePropagation.paramPropagationMap.get(methodDeclKey));
		}
		return true;
	}
	
	private void propagateParamTypes(List arguments, PropagatedMethodsSigniture propagatedMethod){
		for (int i=0;i<arguments.size();i++){
			Expression exp = (Expression)(arguments.get(i));
			propagateTypeForVars(exp, propagatedMethod.paramTypes.get(i));
		}
	}
	
	private void propagateTypeForVars(Expression exp, String RWType){
		if (RWType.equals(CMType.NonType)|| RWType.length()==0){
			return;
		}
		//propagating types for local variables
		//propagating types for fields
		//propagating types for this.field
		//propagating types for getSomething()
		if(exp instanceof FieldAccess){
			FieldAccess fieldAccess = ((FieldAccess)exp);
			exp = fieldAccess.getName();
		}
		if(exp instanceof SimpleName){				
			IBinding binding= ((SimpleName)exp).resolveBinding();
		 	if (binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding bindingDecl= ((IVariableBinding) ((SimpleName)exp).resolveBinding()).getVariableDeclaration();
				String formalElementName = bindingDecl.getName();
    			System.out.println("Propagation: variables need propagation here: "+RWType);
				if(bindingDecl.isField()){
					ASTNode declaringClassNode = compilationUnit.findDeclaringNode(bindingDecl.getDeclaringClass());
					if(declaringClassNode!= null && declaringClassNode instanceof TypeDeclaration){
		    			TypeDeclaration parentTD = (TypeDeclaration)declaringClassNode;						    			
		    			RWTView.saveJAVADocElementToFile(parentTD, RWTAnnotation.Define, formalElementName, RWType, true);
					}
//					else{
//						String declarationBodykey = bindingDecl.getDeclaringClass().getKey();
//						IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(bindingDecl.getJavaElement().getPath());
//						CMTypeView.saveJAVADocElementToFile(declarationBodykey, ifile, RWTAnnotation.Define, formalElementName, RWType);
//					}
				}else{
					ASTNode declaringMethodNode = compilationUnit.findDeclaringNode(bindingDecl.getDeclaringMethod());
					MethodDeclaration methodDeclaration = (MethodDeclaration)declaringMethodNode;
	                RWTView.saveJAVADocElementToFile(methodDeclaration, RWTAnnotation.Define, formalElementName, RWType, true);
				}
			}
		 	else {
				throw new IllegalArgumentException("Unexpected binding"); //$NON-NLS-1$
			}
		}
	}
	
}