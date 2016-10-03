package rwtchecker.popup.actions.inference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import rwtchecker.annotation.AnnotationLearner;
import rwtchecker.annotation.FileAnnotations;
import rwtchecker.annotation.RWTAnnotation;
import rwtchecker.popup.actions.TypePropagationProjectInNavigator;
import rwtchecker.rwt.RWType;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.RWTView;

public class TypePropagationVisitor extends ASTVisitor {
	
	private CompilationUnit compilationUnit;
	
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	private boolean insideMethodBody = false;
	private int returnStatementCount = 0;
	private ReturnStatement lastRS = null;
	
	//propagation category #1: return statement to method signature
	//propagation category #2: right side to left side of assignment and variableDeclaration
	//propagation category #3: arguments of method to all calling sites of the method
	Map<String, PropagatedMethod> methodsToPropagate = new HashMap<String, PropagatedMethod>(); 
	
	TypePropagationProjectInNavigator propagationInfo = new TypePropagationProjectInNavigator();
		
	private IPath currentFilePath;
	private IFile currentFile;
	//for learner
	private Queue<String> current_itemset = new LinkedList<String>();
	private boolean saveStatementsForlearning = false;
	private boolean hasRWType = false;
	String itemsets_lhv = "";
	String itemsets_rhv = "";
	String insideKnownMethod = "";
	AnnotationLearner learner = null;
	
	
	
	public TypePropagationVisitor(CompilationUnit compilationUnit) {
		super(true);
		this.compilationUnit = compilationUnit;
		currentFilePath = this.compilationUnit.getJavaElement().getPath();
		currentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(currentFilePath);
		File annotationFile = RWTSystemUtil.getAnnotationFile(currentFile);
		if(annotationFile!= null && annotationFile.exists()){
			fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			if(fileAnnotations == null){
				return;
			}
			fileAnnotations.load(allVariableMap, methodReturnMap, null, null);
		}
		learner = AnnotationLearner.getInstance();
		learner.loadConfirmedData("e:\\confirmed_annotations.txt");
	}
	
	public void preVisit(ASTNode node){
		if (node instanceof MethodDeclaration){
			insideMethodBody = true;
		}
		
		if (node instanceof ReturnStatement){
			if(insideMethodBody){
				lastRS = (ReturnStatement)node;
				returnStatementCount++;
			}
		}
		
		//for learning
		if(node instanceof Assignment || node instanceof VariableDeclarationStatement){
			saveStatementsForlearning = true;
			hasRWType = false;
			current_itemset.clear();
		}
	}
	
	public void postVisit(ASTNode node){
		if(node instanceof CompilationUnit){
			//end of the visitor; do the method propagation
			IJavaProject javaProject = JavaCore.create(currentFile.getProject());
			ArrayList<IResource> javaSourceFiles = RWTSystemUtil.getAllJavaSourceFiles(javaProject);
        	for(IResource javaSource : javaSourceFiles){
        		ASTParser parser = ASTParser.newParser(AST.JLS3);
        		parser.setKind(ASTParser.K_COMPILATION_UNIT);
        		IWorkspace workspace= ResourcesPlugin.getWorkspace(); 
        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
        		if(sourceFile.exists()){
//        			System.out.println(sourceFile);
        			if(sourceFile.getFullPath().toString().contains("CADRG")){
        				System.out.println("pause here");
        			}
        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        			parser.setSource(icompilationUnit); // set source
        			parser.setResolveBindings(true); // we need bindings later on
        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
        			System.out.println("parameter to argument propagating: "+javaSource.getName());
        			compilationResult.accept(new ParameterPropagationVisitor(this.methodsToPropagate,compilationResult));
        		}
        	}
		}
		
		if (node instanceof MethodDeclaration){
			MethodDeclaration methodDecl = (MethodDeclaration)node;
			String methodKey = methodDecl.resolveBinding().getKey();
			if(returnStatementCount==1){
				//propagate the return type to method signature
				propagateReturnType(lastRS.getExpression(), methodDecl);
			}
			insideMethodBody = false;
			returnStatementCount = 0;
			
			//save to the propagation map; get ready for parameter to argument propagation
			PropagatedMethod methodToPropagate = new PropagatedMethod(); 
			methodToPropagate.setMethodKey(methodKey);
			String rwtType = methodReturnMap.get(methodKey);
			boolean hasNonEmptyRWT = false;
			if(rwtType != null && rwtType.length()>0){
				methodToPropagate.setReturnType(rwtType);
				hasNonEmptyRWT = true;
			}
			for (int i=0;i<methodDecl.parameters().size();i++){
				SingleVariableDeclaration parameterDeclaration = (SingleVariableDeclaration)(methodDecl.parameters().get(i));
				String rwt = getRWTypeForSimpleExp(parameterDeclaration.getName());
				if(rwt.length()>1){
					hasNonEmptyRWT = true;
				}
				methodToPropagate.paramTypes.add(rwt);
			}
			if(hasNonEmptyRWT){
				this.methodsToPropagate.put(methodKey,methodToPropagate);	
			}
		}
		
		if(node instanceof Assignment){
			Assignment assignmentNode = (Assignment)node;
			Expression leftExp = assignmentNode.getLeftHandSide();
			Expression rightExp = assignmentNode.getRightHandSide();			
			propagateSideToSide(leftExp, rightExp);
		}
		
		if(node instanceof VariableDeclarationStatement){
			VariableDeclarationStatement variableDeclarationStatementNode = (VariableDeclarationStatement)node;
			for (Iterator iter = variableDeclarationStatementNode.fragments().iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
				if(fragment.getInitializer()==null){
					continue;
				}
				propagateSideToSide(fragment.getName(),fragment.getInitializer());
			}
		}
		
		//for learner
		if(node instanceof MethodInvocation){
			String methodkey = ((MethodInvocation)node).resolveMethodBinding().getKey();
			if(insideKnownMethod.equals(methodkey)){
				//end of the current known method
				saveStatementsForlearning = true;
			}
		}
		if(saveStatementsForlearning){
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
						String rwtype = getRWTypeForSimpleExp((SimpleName)node);
						if(rwtype.length()>0 && !rwtype.equals(RWType.GenericMethod)){
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
				saveStatementsForlearning = false;
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
				saveStatementsForlearning = false;
				hasRWType = false;
			}
		}
	}
	
	private void propagateLearnedType(Expression exp){
		String lhv_rwtype = getRWTypeForSimpleExp(exp);
		String typeToLearn = learner.confirmedAnnotationMap.get(itemsets_rhv);
		if(lhv_rwtype.length()>0 && !lhv_rwtype.equals("var") && !lhv_rwtype.equals(typeToLearn)){
			System.out.println("Propagation: learning failed: "+exp);
		}else{
			System.out.println("Propagation: learning for type "+typeToLearn);
			propagateTypeForVars(exp, typeToLearn, this.compilationUnit);
		}
	}
	
	private void propagateSideToSide(Expression leftExp, Expression rightExp){
	//left side must be a assignable variable
		if(leftExp instanceof FieldAccess || leftExp instanceof SimpleName){
			String leftRWType = getRWTypeForSimpleExp(leftExp);
			if(rightExp instanceof SimpleName || rightExp instanceof FieldAccess || rightExp instanceof MethodInvocation){
				String rightRWType = getRWTypeForSimpleExp(rightExp);
				if(rightRWType.equals(leftRWType)){
					return;
				}
				if(leftRWType.length()>0 && rightRWType.length()==0){
					//propagate from left to right
					propagateTypeForVars(rightExp,leftRWType, this.compilationUnit);
				}else if(leftRWType.length()==0 && rightRWType.length()>0){
					//propagate from right to left
					propagateTypeForVars(leftExp,rightRWType, this.compilationUnit);
				}
			}
		}
	}
		
	private String getRWTypeForSimpleExp(Expression exp){
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
						File otherSourceFileAnnotationFile = RWTSystemUtil.getAnnotationFile(ifile);
						if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
							FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
							if(otherSourcefileAnnotation == null){
								return thisRWType;
							}
							thisRWType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
						}
					}						
				}else{
					if(variableBinding.getDeclaringMethod()!=null){
						String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
						Map<String, String> variableMap = this.allVariableMap.get(methodDeclKey);
						if(variableMap!=null){
							thisRWType = variableMap.get(variableBinding.getName());
						}	
					}
				}
			}
		}
		if (exp instanceof MethodInvocation){
			MethodInvocation mi = (MethodInvocation)exp;
			IMethodBinding methodBinding = mi.resolveMethodBinding();
			thisRWType = FileAnnotations.getRWTypeForMethod(methodBinding);
		}
		if(thisRWType ==null){
			return "";
		}else{
			return thisRWType;
		}
	}
	
	
	private void propagateReturnType(Expression exp, MethodDeclaration methodDecl){
		String RWType = getRWTypeForSimpleExp(exp);
		if(RWType.length()>0){
			System.out.println("Propagation: Method return type propagation here: "+RWType);
			RWTView.saveJAVADocElementToFile(methodDecl, RWTAnnotation.Return, null, RWType, true);	
		}
	}
	
	
	@Override
	public boolean visit(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		//for annotation learner
		String rwtype = FileAnnotations.getRWTypeForMethod(iMethodBinding);
		if(rwtype.length()>0){
			current_itemset.add(rwtype);
			hasRWType = true;
			insideKnownMethod = iMethodBinding.getKey();
			//stop recording for this method
			saveStatementsForlearning = false;
		}else{
			if(saveStatementsForlearning){
				current_itemset.add(iMethodBinding.getKey());	
			}
		}
		return true;
	}
	
/*
	@Override
	public boolean visit(ClassInstanceCreation cic){
		IMethodBinding iMethodBinding = cic.resolveConstructorBinding();
		String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
		if (typePropagation.paramPropagationMap.containsKey(methodDeclKey)){
			propagateParamTypes(cic.arguments(), typePropagation.paramPropagationMap.get(methodDeclKey));
		}
		return true;
	}
	*/
	
	/*
	private void propagateParamTypes(List arguments, PropagatedMethodsSigniture propagatedMethod){
		for (int i=0;i<arguments.size();i++){
			Expression exp = (Expression)(arguments.get(i));
			propagateTypeForVars(exp, propagatedMethod.paramTypes.get(i));
		}
	}
	*/
	
	public static void propagateTypeForVars(Expression exp, String rwtype, CompilationUnit compilationUnit){
		if (rwtype.equals(RWType.NonType)|| rwtype.length()==0){
			return;
		}
		//propagating types for local variables
		//propagating types for fields
		//propagating types for this.field
		//propagating types for calling method such as getXXX()

		if(exp instanceof FieldAccess){
			FieldAccess fieldAccess = ((FieldAccess)exp);
			exp = fieldAccess.getName();
		}
		if(exp instanceof SimpleName){				
			IBinding binding= ((SimpleName)exp).resolveBinding();
		 	if (binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding bindingDecl= ((IVariableBinding) ((SimpleName)exp).resolveBinding()).getVariableDeclaration();
				String formalElementName = bindingDecl.getName();
    			System.out.println("Propagation: variables need propagation here: "+rwtype);
				if(bindingDecl.isField()){
					ASTNode declaringClassNode = compilationUnit.findDeclaringNode(bindingDecl.getDeclaringClass());
					if(declaringClassNode!= null && declaringClassNode instanceof TypeDeclaration){
		    			TypeDeclaration parentTD = (TypeDeclaration)declaringClassNode;
		    			RWTView.saveJAVADocElementToFile(parentTD, RWTAnnotation.Define, formalElementName, rwtype, true);
					}
				}else{
					ASTNode declaringMethodNode = compilationUnit.findDeclaringNode(bindingDecl.getDeclaringMethod());
					MethodDeclaration methodDeclaration = (MethodDeclaration)declaringMethodNode;
	                RWTView.saveJAVADocElementToFile(methodDeclaration, RWTAnnotation.Define, formalElementName, rwtype, true);
				}
			}
		}
		/* the method could be generic, so propagating with method signature may not be a good idea;
		 * instead, we should consider about how deal with generic methods;
		 */
		/*
		if(exp instanceof MethodInvocation){
			MethodInvocation mi = (MethodInvocation)exp;
			IMethodBinding methodBinding = mi.resolveMethodBinding();
			String methodDeclKey = methodBinding.getMethodDeclaration().getKey();
			IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(methodBinding.getJavaElement().getPath());
			System.out.println("Propagation: assignment propagation here: "+RWType);
			CMTypeView.saveJAVADocElementToFile(methodDeclKey, ifile, RWTAnnotation.Return, null, RWType);
		}
		*/
	}
	
}
class ParameterPropagationVisitor extends ASTVisitor{
	
	CompilationUnit compilationUnit = null; 
	FileAnnotations fileAnnotations = null;
	Map<String, PropagatedMethod> methodsToPropagate = null;
	ParameterPropagationVisitor(Map<String, PropagatedMethod> methodsToPropagate, CompilationUnit compilationUnit){
		ParameterPropagationVisitor.this.compilationUnit  = compilationUnit; 
		ParameterPropagationVisitor.this.methodsToPropagate = methodsToPropagate;
	}

	private IVariableBinding getBindingForExp(Expression exp){
		if(exp instanceof FieldAccess){
			FieldAccess fieldAccess = ((FieldAccess)exp);
			exp = fieldAccess.getName();
		}
		if(exp instanceof SimpleName){				
			IBinding binding= ((SimpleName)exp).resolveBinding();
			if (binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding bindingDecl= ((IVariableBinding) ((SimpleName)exp).resolveBinding()).getVariableDeclaration();
				return bindingDecl;
			}
		}
		return null;
	}
	
	@Override
	public void postVisit(ASTNode node){
		if(node instanceof MethodInvocation){
			MethodInvocation methodInvocationNode = (MethodInvocation)node;
			String methodKey = methodInvocationNode.resolveMethodBinding().getKey();
			if(methodsToPropagate.containsKey(methodKey)){
				PropagatedMethod methodSignature = methodsToPropagate.get(methodKey);
				for (int i=0;i<methodInvocationNode.arguments().size();i++){
					Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
					String rwtType = methodSignature.paramTypes.get(i);
					if(rwtType.length()>0){
						TypePropagationVisitor.propagateTypeForVars(exp,rwtType,ParameterPropagationVisitor.this.compilationUnit);	
					}
				}	
			}
		}
		
		if(node instanceof ClassInstanceCreation){
			ClassInstanceCreation cic = (ClassInstanceCreation)node;
			String bodyKey = cic.resolveConstructorBinding().getKey();
			if(methodsToPropagate.containsKey(bodyKey)){
				PropagatedMethod methodSignature = methodsToPropagate.get(bodyKey);
				for (int i=0;i<cic.arguments().size();i++){
					Expression exp = (Expression)(cic.arguments().get(i));
					String rwtType = methodSignature.paramTypes.get(i);
					if(rwtType.length()>0){
						TypePropagationVisitor.propagateTypeForVars(exp,rwtType,ParameterPropagationVisitor.this.compilationUnit);	
					}
				}	
			}
		}
		
		if(node instanceof SuperConstructorInvocation){
			SuperConstructorInvocation sci = (SuperConstructorInvocation)node;
			String bodyKey = sci.resolveConstructorBinding().getKey();
			if(methodsToPropagate.containsKey(bodyKey)){
				PropagatedMethod methodSignature = methodsToPropagate.get(bodyKey);
				for (int i=0;i<sci.arguments().size();i++){
					Expression exp = (Expression)(sci.arguments().get(i));
					String rwtType = methodSignature.paramTypes.get(i);
					if(rwtType.length()>0){
						TypePropagationVisitor.propagateTypeForVars(exp,rwtType,ParameterPropagationVisitor.this.compilationUnit);	
					}
				}	
			}
		}
		
		if(node instanceof ConstructorInvocation){
			ConstructorInvocation cic = (ConstructorInvocation)node;
			String bodyKey = cic.resolveConstructorBinding().getKey();
			if(methodsToPropagate.containsKey(bodyKey)){
				PropagatedMethod methodSignature = methodsToPropagate.get(bodyKey);
				for (int i=0;i<cic.arguments().size();i++){
					Expression exp = (Expression)(cic.arguments().get(i));
					String rwtType = methodSignature.paramTypes.get(i);
					if(rwtType.length()>0){
						TypePropagationVisitor.propagateTypeForVars(exp,rwtType,ParameterPropagationVisitor.this.compilationUnit);	
					}
				}	
			}
		}
		
		if(node instanceof Assignment){
			Assignment assignNode = (Assignment)node;
			Expression leftSide = assignNode.getLeftHandSide();
			Expression rightSide = assignNode.getRightHandSide();
			if(rightSide instanceof MethodInvocation){
				MethodInvocation methodInvocationNode = (MethodInvocation)rightSide;
				String methodKey = methodInvocationNode.resolveMethodBinding().getKey();
				if(methodsToPropagate.containsKey(methodKey)){
					PropagatedMethod methodSignature = methodsToPropagate.get(methodKey);
					String rwtType = methodSignature.getReturnType();
					if(rwtType.length()>0){
						TypePropagationVisitor.propagateTypeForVars(leftSide,rwtType,ParameterPropagationVisitor.this.compilationUnit);
					}
				}
			}
		}
		
		if(node instanceof VariableDeclarationStatement){
			VariableDeclarationStatement varDecl = (VariableDeclarationStatement)node;
			for (Iterator iter = varDecl.fragments().iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
				if(fragment.getInitializer()==null){
					continue;
				}
				Expression leftSide = fragment.getName();
				Expression rightSide = fragment.getInitializer();
				if(rightSide instanceof MethodInvocation){
					MethodInvocation methodInvocationNode = (MethodInvocation)rightSide;
					String methodKey = methodInvocationNode.resolveMethodBinding().getKey();
					if(methodsToPropagate.containsKey(methodKey)){
						PropagatedMethod methodSignature = methodsToPropagate.get(methodKey);
						String rwtType = methodSignature.getReturnType();
						if(rwtType.length()>0){
							TypePropagationVisitor.propagateTypeForVars(leftSide,rwtType,ParameterPropagationVisitor.this.compilationUnit);
						}
					}
				}
			}
		}
	}
}

class PropagatedMethod{
	private String methodKey = "";
	ArrayList<String> paramTypes = new ArrayList<String>();
	private String returnType = "";
	public String getMethodKey() {
		return methodKey;
	}
	public void setMethodKey(String methodKey) {
		this.methodKey = methodKey;
	}
	public String getReturnType() {
		return returnType;
	}
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
}