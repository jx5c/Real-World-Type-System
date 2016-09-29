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
import rwtchecker.annotation.FileAnnotations;
import rwtchecker.annotation.RWTAnnotation;
import rwtchecker.popup.actions.TypePropagationProjectInNavigator;
import rwtchecker.popup.actions.TypePropagationProjectInNavigator.PropagatedMethodsSigniture;
import rwtchecker.util.CMModelUtil;
import rwtchecker.views.RWTView;

public class TypeInferenceAssignmentsVisitor extends ASTVisitor {
	//test git hub here
	private CompilationUnit compilationUnit;
	
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	
	private IPath currentFilePath;
	private IFile currentFile;
	
	private TypePropagationProjectInNavigator typeProagation = new TypePropagationProjectInNavigator();
	
	public TypeInferenceAssignmentsVisitor(CompilationUnit compilationUnit) {
		super(true);
		this.compilationUnit = compilationUnit;
		currentFilePath = this.compilationUnit.getJavaElement().getPath();
		currentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(currentFilePath);
		File annotationFile = CMModelUtil.getAnnotationFile(currentFile);
		if(annotationFile!= null && annotationFile.exists()){
			fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			if(fileAnnotations == null){
				return;
			}
			fileAnnotations.load(allVariableMap, methodReturnMap, null, null);
		}
	}
	
	public void postVisit(ASTNode node){		
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
			if (typeProagation.learningPropagationMap.containsKey(methodKey)){
				boolean inconsistentType = false;
				ArrayList<PropagatedMethodsSigniture> propagatedMethodList = typeProagation.learningPropagationMap.get(methodKey);
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
	
	private String getRWTypeForVarLikeExp(Expression exp){
		String thisRWType = "";
		if(exp instanceof FieldAccess){
			FieldAccess fieldAccess = ((FieldAccess)exp);
			exp = fieldAccess.getName();
		}
		if(exp instanceof SimpleName){				
			IBinding binding= ((SimpleName)exp).resolveBinding();
			if (binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding= ((IVariableBinding) ((SimpleName)exp).resolveBinding()).getVariableDeclaration();
				if(variableBinding.isField()){
					String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
					if(variableBinding.getJavaElement() == null){
						return thisRWType;
					}
					String classDeclPath = variableBinding.getJavaElement().getPath().toString();
					String classDeclKey = variableBinding.getDeclaringClass().getKey();
					if(currentUnitPath.equals(classDeclPath)){
						Map<String, String> variableMap = this.allVariableMap.get(classDeclKey);
						if(variableMap!=null){
							thisRWType = variableMap.get(variableBinding.getName());
						}
					}else{
						IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(variableBinding.getJavaElement().getPath());
						File otherSourceFileAnnotationFile = CMModelUtil.getAnnotationFile(ifile);
						if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
							FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
							if(otherSourcefileAnnotation == null){
								return thisRWType;
							}
							thisRWType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
						}
					}						
				}else{
					String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
					Map<String, String> variableMap = this.allVariableMap.get(methodDeclKey);
					if(variableMap!=null){
						thisRWType = variableMap.get(variableBinding.getName());
					}
				}
			}
		}
		if (exp instanceof MethodInvocation){
			MethodInvocation mi = (MethodInvocation)exp;
			IMethodBinding methodBinding = mi.resolveMethodBinding();
			thisRWType = FileAnnotations.getRWTypeForMethod(methodBinding);
		}
		if(exp instanceof ClassInstanceCreation){
			ClassInstanceCreation cic = (ClassInstanceCreation)exp;
			//public boolean visit( cic){
				IMethodBinding iMethodBinding = cic.resolveConstructorBinding();
				String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
				if (typeProagation.paramPropagationMap.containsKey(methodDeclKey)){
					propagateParamTypes(cic.arguments(), typeProagation.paramPropagationMap.get(methodDeclKey));
				}
			//	return true;
			//}
		}
		
		
		if(thisRWType ==null){
			return "";
		}else{
			return thisRWType;
		}
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