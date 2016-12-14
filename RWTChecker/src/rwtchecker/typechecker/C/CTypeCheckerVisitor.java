package rwtchecker.typechecker.C;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.c.CArrayType;
import org.eclipse.cdt.internal.core.dom.parser.c.CBasicType;
import org.eclipse.cdt.internal.core.dom.parser.c.CFunction;
import org.eclipse.cdt.internal.core.dom.parser.c.CStructure;
import org.eclipse.cdt.internal.core.dom.parser.c.CVariable;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPArrayType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPBasicType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPVariable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.rwt.RWT_Attribute;
import rwtchecker.rwt.RWType;
import rwtchecker.rwtrules.RWTypeRuleCategory;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.typechecker.NewTypeCheckerVisitor;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.RWTView;
import rwtchecker.util.DiagnosticMessage;
import rwtchecker.util.ErrorUtil;

public class CTypeCheckerVisitor extends ASTVisitor {
	
	public static HashSet<String> cmtypeHashSet = new HashSet<String>();
	
	private RWTypeRulesManager rwtRulesManager;
	
	private ArrayList<DiagnosticMessage> errorReports = new ArrayList<DiagnosticMessage>();
	private IASTTranslationUnit compilationUnit;
	
	private boolean accessRWT = false;
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	
	//for annotations
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	//for every ast node, track their real-world types
	private HashMap<IASTNode, String> rwtypeTableForExp = new HashMap<IASTNode, String>();
	
	//for invariants checking; save them for later improvement
	Map<String, Map<String, HashSet<String>>> allInvAttToRecordMap = new HashMap<String, Map<String, HashSet<String>>>();
	Map<String, ArrayList<String>> allInvariantsMap = new HashMap<String, ArrayList<String>>();
	
	private IFile currentFile;
	private IProject currentProject;
	
	private boolean methodInvError = false;

	private boolean checkingUnits;
	
	public CTypeCheckerVisitor(RWTypeRulesManager manager, IASTTranslationUnit compilationUnit, boolean unitsChecking, IFile ifile) {
		super(true);
		this.rwtRulesManager = manager;
		this.compilationUnit = compilationUnit;
		
		currentFile = ifile;
		currentProject = currentFile.getProject();
		File annotationFile = RWTSystemUtil.getAnnotationFile(currentFile);
		if(annotationFile!= null && annotationFile.exists()){
			fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			fileAnnotations.load(allVariableMap, methodReturnMap, allInvAttToRecordMap, allInvariantsMap);
		}
		this.checkingUnits = unitsChecking;
		this.errorReports.clear();
//		super.
		
	}

	@Override
	public int visit(IASTName astName){
		//This function loads all annotated elements into the hashmap
		IBinding astBinding = astName.resolveBinding();
		if (astBinding instanceof CVariable){
			String declBodyKey = RWTView.makeKeyForDeclBodies(astBinding.getOwner().getClass().getName(), astBinding.getOwner().getName());
			String thisRWTType = null;
			//binding for a variable
			CVariable cv = (CVariable)astBinding;
			IType variableType = cv.getType();
			if(variableType instanceof CArrayType){
				//this is for array; pop up a menu so the user can select the index for binding
				CArrayType arrayV = (CArrayType)variableType;
				System.out.println(arrayV.getSize());
			}else if (variableType instanceof CBasicType){
				//this is for a variable; variable could be inside a structure, or
				String varName = astName.toString();
				Map<String, String> variableMap = this.allVariableMap.get(declBodyKey);
				if(variableMap!=null){
					thisRWTType = variableMap.get(varName);
				}
			}else if (variableType instanceof CStructure){
				//this is for a variable of a structure; TO BE DONE
				System.out.println("structure");
			}
			this.associateAttSetsWithExp(astName, thisRWTType);
		}else if(astBinding instanceof CFunction){
			String functionKey = RWTView.makeKeyForDeclBodies(astBinding.getClass().getName(), astBinding.getName());
			String returnType = null;
			String currentUnitPath = this.compilationUnit.getFilePath();
			if(astBinding!=null){
//				String methodDeclUnitPath = iMethodBinding.getJavaElement().getPath().toString();
//				String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
				//if the function being called comes from the same file
				if(currentUnitPath != null){
					if(methodReturnMap!=null){
						returnType = methodReturnMap.get(functionKey);
					}
				}else{
				//if the function being called comes from another file
//					IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(iMethodBinding.getJavaElement().getPath());
					IPath ipath = new Path(currentUnitPath);
					IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(ipath);
					File otherSourceFileAnnotationFile = RWTSystemUtil.getAnnotationFile(ifile);
					if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
						FileAnnotations otherSourcefileAnnotationsClone = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
						if(otherSourcefileAnnotationsClone == null){
							return 3;
						}
						returnType = otherSourcefileAnnotationsClone.getReturnCMTypeForMethod(functionKey);
					}
				}						
	 		}
			this.associateAttSetsWithExp(astName, returnType);	
		}
		return 3;
	}
	
	@Override
	public int leave(IASTExpression exp){
		if(exp instanceof IASTCastExpression){
			//for case expression (float)(1.1)
			IASTCastExpression castExp = (IASTCastExpression)exp;
			this.associateAttSetsWithExp(castExp, this.getAnnotatedTypeForExpression(castExp.getOperand()));
		}else if(exp instanceof IASTUnaryExpression){
			//for parenthesis    rwt for (x) is the same for x;
			IASTUnaryExpression unaryExp = (IASTUnaryExpression)exp;
			if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
				String argumentCMType = this.getAnnotatedTypeForExpression(unaryExp.getOperand());
				this.associateAttSetsWithExp(unaryExp, argumentCMType);	
			}
		}else if(exp instanceof IASTIdExpression){
			//for local variables,    rwt for  idExpression is the same for the IASTName
			IASTIdExpression idExp = (IASTIdExpression)exp ;
			this.associateAttSetsWithExp(idExp, this.getAnnotatedTypeForExpression(idExp.getName()));
		}else if(exp instanceof IASTFieldReference){
			//for field access; e.g. a.b => a is the expression, b is the field name. 
			IASTFieldReference fieldReference = (IASTFieldReference)exp;
			String annotatedFieldAccessType = this.getAnnotatedTypeForExpression(fieldReference.getFieldOwner());
			String annotatedIdentifierType = this.getAnnotatedTypeForExpression(fieldReference.getFieldName());
			//using union operation for the two types
			if(!NewTypeCheckerVisitor.checkConsistency(annotatedFieldAccessType, annotatedIdentifierType)){
				//inconsistent attributes
				this.addNewErrorMessage(fieldReference, ErrorUtil.getInconsistentAttributeError(),DiagnosticMessage.ERROR);
				associateAttSetsWithExp(fieldReference, annotatedIdentifierType);
			}else{
				String unitedSetsType = NewTypeCheckerVisitor.uniteTwoSets(annotatedFieldAccessType, annotatedIdentifierType, rwtRulesManager);
				associateAttSetsWithExp(fieldReference, unitedSetsType);
			}
		}else if(exp instanceof IASTConditionalExpression){
			//for Conditional Expression of the format X ? Y : Z
		}else if(exp instanceof IASTBinaryExpression){
			//for  a binary expression.  it has different operators that need to be handled differently
			IASTBinaryExpression binaryExp = (IASTBinaryExpression)exp;
			this.checkBinaryExpression(binaryExp);
		}
		
		IType expType = exp.getExpressionType();
		IASTNode node = exp.getOriginalNode();
		System.out.println(node);
		if(node instanceof CASTBinaryExpression){
			CASTBinaryExpression binaryExp = (CASTBinaryExpression)node;
			System.out.println(binaryExp.getOperand1());
			System.out.println(binaryExp.getOperand2());
		}
		
		
		
		return 3;
	}
	
	public int visit(IASTDeclaration declaration){
		System.out.println("declaration: " + declaration + " ->  " + declaration.getRawSignature());
	
		if ((declaration instanceof IASTSimpleDeclaration)) {
			IASTSimpleDeclaration ast = (IASTSimpleDeclaration)declaration;
			try{
				System.out.println("--- type: " + ast.getSyntax() + " (childs: " + ast.getChildren().length + ")");
				IASTNode typedef = ast.getChildren().length == 1 ? ast.getChildren()[0] : ast.getChildren()[1];
		             System.out.println("------- typedef: " + typedef);
		             IASTNode[] children = typedef.getChildren();
		             if ((children != null) && (children.length > 0))
		               System.out.println("------- typedef-name: " + children[0].getRawSignature());
			}catch (ExpansionOverlapsBoundaryException e){
				e.printStackTrace();
			}
	
			IASTDeclarator[] declarators = ast.getDeclarators();
			for (IASTDeclarator iastDeclarator : declarators) {
				System.out.println("iastDeclarator > " + iastDeclarator.getName());
			}
	        IASTAttribute[] attributes = ast.getAttributes();
	        for (IASTAttribute iastAttribute : attributes) {
	            System.out.println("iastAttribute > " + iastAttribute);
	        }
	    }
	
		 if ((declaration instanceof IASTFunctionDefinition)) {
		    IASTFunctionDefinition ast = (IASTFunctionDefinition)declaration;
		    ICPPASTFunctionDeclarator typedef = (ICPPASTFunctionDeclarator)ast.getDeclarator();		    
		 }
		 
		 return 3;
	 }
	 
     public int visit(IASTTypeId typeId){
         System.out.println("typeId: " + typeId.getRawSignature());
         return 3;
     }
 
     public int visit(IASTStatement statement){
         System.out.println("statement: " + statement.getRawSignature());
         return 3;
     }
 
     public int visit(IASTAttribute attribute){
         return 3;
     }	       
	
     private void checkBinaryExpression(IASTBinaryExpression binaryExp){
    	 	IASTExpression leftExp = binaryExp.getOperand1(); 
    	 	IASTExpression rightExp = binaryExp.getOperand2();
    	 	String leftCMType = this.getAnnotatedTypeForExpression(leftExp);
	 		String rightCMType = this.getAnnotatedTypeForExpression(rightExp);
			int operator = binaryExp.getOperator();
			switch (operator){
				case IASTBinaryExpression.op_assign:
					//for assignemnt; do some error checking
					checkAssignmentExp(binaryExp, leftExp, rightExp, leftCMType, rightCMType);
				break;
				case IASTBinaryExpression.op_equals:
					//for ==;
					checkComparableExp(binaryExp, leftExp, rightExp, leftCMType, rightCMType);
				break;
				case IASTBinaryExpression.op_greaterEqual:
					//for >=;
					checkComparableExp(binaryExp, leftExp, rightExp, leftCMType, rightCMType);
				break;
				case IASTBinaryExpression.op_greaterThan:
					//for >;
					checkComparableExp(binaryExp, leftExp, rightExp, leftCMType, rightCMType);
				break;
				case IASTBinaryExpression.op_lessEqual:
					//for <=;
					checkComparableExp(binaryExp, leftExp, rightExp, leftCMType, rightCMType);
				break;
				case IASTBinaryExpression.op_lessThan:
					//for <;
					checkComparableExp(binaryExp, leftExp, rightExp, leftCMType, rightCMType);
				break;
				case IASTBinaryExpression.op_multiply:
					//for *;
					checkInfixExpression(binaryExp);
				break;
				case IASTBinaryExpression.op_divide:
					//for /;
				break;
				case IASTBinaryExpression.op_plus:
					//for +;
				break;
				case IASTBinaryExpression.op_minus:
					//for -;
				break;
				case IASTBinaryExpression.op_min:
					//for min;
				break;
				case IASTBinaryExpression.op_max:
					//for max;
				break;
				case IASTBinaryExpression.op_divideAssign:
					//for /=;
				break;
				case IASTBinaryExpression.op_minusAssign:
					//for -=;
				break;
				case IASTBinaryExpression.op_plusAssign:
					//for -=;
				break;
				case IASTBinaryExpression.op_multiplyAssign:
					//for *=;
				break;
			}
     }
     
     private void checkAssignmentExp(IASTNode currentNode, IASTExpression leftExp, IASTExpression rightExp, String leftCMType, String rightCMType){
 		
		if(leftCMType.equals(rightCMType)){
			return;
		}
		if(!leftCMType.equals(RWType.TypeLess) && !rightCMType.equals(RWType.TypeLess)){
			String returnType = this.rwtRulesManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Assignable, rightCMType);
			if(returnType != null){
				return;
			}
		}
	    if(!leftCMType.equalsIgnoreCase(RWType.UnknownType) &&
				!leftCMType.equalsIgnoreCase(rightCMType) && 
				!rightCMType.equalsIgnoreCase(RWType.UnknownType) ){
			addNewErrorMessage(currentNode , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);	
		}
	    
		//simple inference here
		//if we are in units checking, do inference here 
		if(this.checkingUnits){
			if(leftCMType.equals(RWType.TypeLess) && !rightCMType.equals(RWType.TypeLess) 
					&& !rightCMType.equals(RWType.error_propogate) && !rightCMType.equals(RWType.error_source)){
				if(leftExp instanceof IASTName){
					IBinding astBinding = ((IASTName)leftExp).resolveBinding();
					if(astBinding instanceof CVariable){
						CVariable variableBinding = (CVariable) astBinding;
						String declBodyKey = RWTView.makeKeyForDeclBodies(astBinding.getOwner().getClass().getName(), astBinding.getOwner().getName());
						Map<String, String> variableMap = this.allVariableMap.get(declBodyKey);
						if(variableMap == null){
							variableMap = new HashMap<String, String>();
							this.allVariableMap.put(declBodyKey, variableMap);
						}
						variableMap.put(variableBinding.getName(), rightCMType);
					}
				}
			}
		}
     }
     
     private void checkComparableExp(IASTNode currentNode, IASTExpression leftExp, IASTExpression rightExp, String leftCMType, String rightCMType){
		if(leftCMType.equals(rightCMType)){
			return;
		}
		if(!leftCMType.equals(RWType.TypeLess) && !rightCMType.equals(RWType.TypeLess)){
			String returnType = this.rwtRulesManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Comparable, rightCMType);
			if(returnType != null){
				return;
			}
		}
		
	    if(!leftCMType.equalsIgnoreCase(RWType.UnknownType) &&
				!leftCMType.equalsIgnoreCase(rightCMType) && 
				!rightCMType.equalsIgnoreCase(RWType.UnknownType) ){
			addNewErrorMessage(currentNode , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);	
		}
	    
		//simple inference here
		//if we are in units checking, do inference here 
		if(this.checkingUnits){
			if(leftCMType.equals(RWType.TypeLess) && !rightCMType.equals(RWType.TypeLess) 
					&& !rightCMType.equals(RWType.error_propogate) && !rightCMType.equals(RWType.error_source)){
				if(leftExp instanceof IASTName){
					IBinding astBinding = ((IASTName)leftExp).resolveBinding();
					if(astBinding instanceof CVariable){
						CVariable variableBinding = (CVariable) astBinding;
						String declBodyKey = RWTView.makeKeyForDeclBodies(astBinding.getOwner().getClass().getName(), astBinding.getOwner().getName());
						Map<String, String> variableMap = this.allVariableMap.get(declBodyKey);
						if(variableMap == null){
							variableMap = new HashMap<String, String>();
							this.allVariableMap.put(declBodyKey, variableMap);
						}
						variableMap.put(variableBinding.getName(), rightCMType);
					}
				}
			}
		}
     }
     
     private void checkAssignPlus(){
 	    //save this function for future improvement
    	 /*
   		if(operator.equals(Assignment.Operator.PLUS_ASSIGN)){
 			String returnType = this.rwtRulesManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Plus, rightCMType);
 			if(returnType != null){
 				if(returnType.equals(leftCMType)){
 					return;
 				}else{
 					String assignableType = this.rwtRulesManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Assignable, returnType);
 					if(assignableType != null){
 						return;
 					}else{
 						addNewErrorMessage(node , ErrorUtil.unknownCalculation(), DiagnosticMessage.WARNING);
 					}
 				}
 			}else{
 				if(leftCMType.length()>0 && rightCMType.length()>0){
 					addNewErrorMessage(assignmentNode , ErrorUtil.getUndeclaredCalculation(assignmentNode.toString()), DiagnosticMessage.WARNING);	
 				}
 			}
 		}
    	  */
     }
     
     
	public void EndVisitNode(ASTNode node){
		
		else if(node instanceof ArrayAccess){
			ArrayAccess arrayAccess = (ArrayAccess)node;
			String annotatedType =  this.getAnnotatedTypeForExpression(arrayAccess.getArray());
			String resultType = annotatedType;
			this.associateAttSetsWithExp(arrayAccess, resultType);
		}
		
		
		else if(node instanceof MethodInvocation){
			MethodInvocation methodInvocationNode = (MethodInvocation)node;
			this.checkMathMethodInvocation(methodInvocationNode);
//			this.checkCollectionAccess(methodInvocationNode);
			this.checkAllInvocation(methodInvocationNode.resolveMethodBinding(),methodInvocationNode.arguments(),node);
		}

		else if(node instanceof ReturnStatement){
			ReturnStatement returnStatementNode = (ReturnStatement)node;
			if(parsingMethodDelcMode){
				String returnCMType = this.getAnnotatedTypeForExpression(returnStatementNode.getExpression());
				if(!insideBranch){
					//not inside a branch
					String thisReturnType = this.getAnnotatedTypeForExpression(returnStatementNode.getExpression());
					assignReturnTypeForMethodInv(thisReturnType);

				}else{
					if(!errorInsideBranch){
						//no error has been found in this branch: return cmtype should be valid
						String thisReturnType = this.getAnnotatedTypeForExpression(returnStatementNode.getExpression());
						assignReturnTypeForMethodInv(thisReturnType);
					}
				}
			}
		}
		
		else if(node instanceof VariableDeclarationStatement){
			VariableDeclarationStatement variableDeclarationStatementNode = (VariableDeclarationStatement)node;
			for (Iterator iter = variableDeclarationStatementNode.fragments().iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
				if(fragment.getInitializer()==null){
					continue;
				}
				String leftCMType = this.getAnnotatedTypeForExpression(fragment.getName());
				String rightCMType = this.getAnnotatedTypeForExpression(fragment.getInitializer());
				if(leftCMType.equals(rightCMType)){
					return;
				}
				if(!leftCMType.equals(RWType.TypeLess) && !rightCMType.equals(RWType.TypeLess)){
					String returnType = this.rwtRulesManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Assignable, rightCMType);
					if(returnType != null){
						return;
					}else{
						addNewErrorMessage(node , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);
					}
				}
				else if(!leftCMType.equalsIgnoreCase(RWType.UnknownType) &&
						!leftCMType.equalsIgnoreCase(rightCMType) && 
						!rightCMType.equalsIgnoreCase(RWType.UnknownType) ){
					addNewErrorMessage(node , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);	
				}
				//simple inference here
				//if we are in units checking, do inference here
				if(this.checkingUnits){
					if(leftCMType.equals(RWType.TypeLess) && !rightCMType.equals(RWType.TypeLess) 
							&& !rightCMType.equals(RWType.error_propogate) && !rightCMType.equals(RWType.error_source)){
						Expression leftExp = fragment.getName();
						if(leftExp instanceof SimpleName){
							IBinding fbinding = ((SimpleName)leftExp).resolveBinding();
							if(fbinding instanceof IVariableBinding){
								IVariableBinding variableBinding = (IVariableBinding) fbinding;
								String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
								Map<String, String> variableMap = this.allVariableMap.get(methodDeclKey);
								if(variableMap == null){
									variableMap = new HashMap<String, String>();
								}
								leftCMType = rightCMType;
								variableMap.put(variableBinding.getName(), leftCMType);
								this.allVariableMap.put(methodDeclKey, variableMap);
							}
						}
					}	
				}
			}
		}
	}

	/**
	 * polymorphic function to deal with functions without annotations ; 
	 * @param targetedCompilationUnit
	 * @param methodInvocationNode
	 * @param methodDeclKey
	 * @param argument_cmtypes
	 * @return
	 */
	/*
	private String handleGenericMethod(CompilationUnit targetedCompilationUnit, MethodInvocation methodInvocationNode, String methodDeclKey, String[] argument_cmtypes){
		CTypeCheckerVisitor methodDeclVisitor = new CTypeCheckerVisitor(this.rwtRulesManager, targetedCompilationUnit, this.checkingUnits);
		methodDeclVisitor.targetedMethodDeclKey = methodDeclKey;
		methodDeclVisitor.parsingMethodDelcMode = true;
		
		Map<String, String> variableMap = methodDeclVisitor.allVariableMap.get(methodDeclKey);
		ASTNode declaringNode = targetedCompilationUnit.findDeclaringNode(methodDeclKey);
		MethodDeclaration methodDecl = (MethodDeclaration)declaringNode;
		for (int i=0;i<argument_cmtypes.length;i++){
			SingleVariableDeclaration parameterDeclaration = (SingleVariableDeclaration)(methodDecl.parameters().get(i));
			String parameterName = parameterDeclaration.getName().getIdentifier();
			if(argument_cmtypes.length>0){
				if(variableMap == null){
					variableMap = new HashMap<String, String>();
				}
			}
			if(variableMap != null){
				if(variableMap.get(parameterName)!=null){
					String annotatedParaCMType = variableMap.get(parameterName);
					if(annotatedParaCMType.length()>0){
						RWType parameterCMtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, annotatedParaCMType);
						String parameterCMtypeAttSet = parameterCMtype.getEnabledAttributeSet();
						if(checkingUnits){
							parameterCMtypeAttSet = parameterCMtype.getUnitsAttribute();
						}
						if(!parameterCMtypeAttSet.equals(argument_cmtypes[i])){
							String tempReturnType = rwtRulesManager.getReturnType(currentProject, parameterCMtypeAttSet, RWTypeRuleCategory.Assignable,  argument_cmtypes[i]);
							if(tempReturnType==null){
								addNewErrorMessage(methodInvocationNode , ErrorUtil.methodArgumentError(), DiagnosticMessage.ERROR);
								break;
							}
						}
					}
				}
			}
			if(argument_cmtypes[i].length()>0){
				variableMap.put(parameterName, argument_cmtypes[i]);
			}
		}
		targetedCompilationUnit.accept(methodDeclVisitor);
		if(methodDeclVisitor.methodInvError){
			addNewErrorMessage(methodInvocationNode , ErrorUtil.methodInvocationError(), DiagnosticMessage.WARNING);
		}
		return methodDeclVisitor.returnCMTypesForTargetedMethod;
	}
	*/
	
	private void checkAllInvocation(IMethodBinding iMethodBinding, List arguments, ASTNode node){
		String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
		if(iMethodBinding == null || iMethodBinding.getJavaElement()==null){
			return;
		}
		String bodyDeclPath = iMethodBinding.getJavaElement().getPath().toString();
		String bodyDeclKey = iMethodBinding.getMethodDeclaration().getKey();
		
		ASTNode declaringNode = this.compilationUnit.findDeclaringNode(bodyDeclKey);
		FileAnnotations fileAnnotations = null;
		CompilationUnit targetedCompilationUnit = null;
		if(currentUnitPath.equals(bodyDeclPath)){
			fileAnnotations = this.fileAnnotations;
			targetedCompilationUnit = this.compilationUnit;
		}else {
			IFile methodDeclFile = ResourcesPlugin.getWorkspace().getRoot().getFile(iMethodBinding.getJavaElement().getPath());
			if((methodDeclFile != null) && (methodDeclFile.getFileExtension().toLowerCase().endsWith("java"))){
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(methodDeclFile);
				parser.setSource(icompilationUnit); // set source
				parser.setResolveBindings(true);
				targetedCompilationUnit = (CompilationUnit) parser.createAST(null);
				declaringNode = targetedCompilationUnit.findDeclaringNode(bodyDeclKey);
				File annotationFile = RWTSystemUtil.getAnnotationFile(methodDeclFile);
				if(annotationFile != null){
					fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
				}
			}
		}
		
		if(fileAnnotations==null 
				|| fileAnnotations.getAnnotations().get(bodyDeclKey)==null
				|| fileAnnotations.getAnnotations().get(bodyDeclKey).size()==0){
			return;		
		}
		
		if(node instanceof MethodInvocation){
			MethodInvocation methodInvocationNode = (MethodInvocation)node;
			final String expCMType = this.getAnnotatedTypeForExpression(methodInvocationNode.getExpression());
			
			//fileAnnotations is for the method declaration
			String returnType = checkReturnCMType(bodyDeclKey,methodDecl,methodInvocationNode,argumentCMTypes,fileAnnotations);
			if(returnType.equalsIgnoreCase(RWType.GenericMethod)){
				returnType = handleGenericMethod(targetedCompilationUnit,methodInvocationNode,bodyDeclKey,argumentCMTypes);
			}
			if(expCMType.equals(RWType.TypeLess)){
				associateAttSetsWithExp(methodInvocationNode, returnType);	
			}else{
				String finalReturnType = NewTypeCheckerVisitor.uniteTwoSets(expCMType, returnType, rwtRulesManager);
				associateAttSetsWithExp(methodInvocationNode, finalReturnType);
			}
		}
	}
			
	private String checkReturnCMType(String methodKey, MethodDeclaration methodDecl, MethodInvocation methodInvocationNode, String[] argument_cmtypes, FileAnnotations fileAnnotations){
		String returnCMTypeAtt = RWType.UnknownType;
		if(fileAnnotations==null){
			return RWType.UnknownType;
		}
		String returnCMtypeName = fileAnnotations.getReturnCMTypeForMethod(methodKey);
		if(returnCMtypeName.equalsIgnoreCase(RWType.GenericMethod)){
			return RWType.GenericMethod;
		}
		RWType returnCMtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, returnCMtypeName);
		if(returnCMtype!=null){
			returnCMTypeAtt = returnCMtype.getEnabledAttributeSet();
			if(checkingUnits){
				returnCMTypeAtt = returnCMtype.getUnitsAttribute();
			}
		}
		return returnCMTypeAtt;
	}
	
	//hard coding the math default invocation
	private void checkMathMethodInvocation(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		if(iMethodBinding==null || iMethodBinding.getDeclaringClass()==null){
			return;
		}
		String binaryClassName = iMethodBinding.getDeclaringClass().getBinaryName();
		if(binaryClassName.equals("java.lang.Math")){
			String methodName = "java.lang.Math."+iMethodBinding.getName();
			if(methodInvocationNode.arguments().size() == 2){
				Expression argumentOne = (Expression)(methodInvocationNode.arguments().get(0));	
				Expression argumentTwo = (Expression)(methodInvocationNode.arguments().get(1));	
				
				String argumentOneAnnotatedType = this.getAnnotatedTypeForExpression(argumentOne);
				String argumentTwoAnnotatedType = this.getAnnotatedTypeForExpression(argumentTwo);
				

				
					if(argumentTwoAnnotatedType.equalsIgnoreCase(RWType.UnknownType) && argumentOneAnnotatedType.equalsIgnoreCase(RWType.UnknownType)){
						this.associateAttSetsWithExp(methodInvocationNode, RWType.UnknownType);
						return;
					}
					
					
					if(iMethodBinding.getName().equals("max")||iMethodBinding.getName().equals("min")){
						if(argumentTwoAnnotatedType.equals(argumentOneAnnotatedType)){
							this.associateAttSetsWithExp(methodInvocationNode,  argumentTwoAnnotatedType);
							return;
						}else if(argumentOneAnnotatedType.equals(RWType.UnknownType)){
							this.associateAttSetsWithExp(methodInvocationNode,  argumentTwoAnnotatedType);
							return;
						}else if(argumentTwoAnnotatedType.equals(RWType.UnknownType)){
							this.associateAttSetsWithExp(methodInvocationNode,  argumentOneAnnotatedType);
							return;
						}
					}
				
					
					String returnType = this.rwtRulesManager.getReturnType(this.currentProject, argumentOneAnnotatedType, methodName, argumentTwoAnnotatedType); 
					if(returnType != null){
						this.associateAttSetsWithExp(methodInvocationNode,  returnType);
						return;
					}else {
						addNewErrorMessage(methodInvocationNode , ErrorUtil.getUndeclaredCalculation(methodInvocationNode.toString()),  DiagnosticMessage.WARNING);
						this.associateAttSetsWithExp(methodInvocationNode, RWType.error_source);
						return;
					}
				
			}
			if(methodInvocationNode.arguments().size() == 1){
				Expression argument = (Expression)(methodInvocationNode.arguments().get(0));
				String argumentAnnotatedType = this.getAnnotatedTypeForExpression(argument);

				if(argumentAnnotatedType.equalsIgnoreCase(RWType.UnknownType)){
					this.associateAttSetsWithExp(methodInvocationNode, RWType.UnknownType);
					return;
				}
				//abs function
				if(methodName.equalsIgnoreCase(RWTypeRuleCategory.Abosolute_Value)
				||methodName.equalsIgnoreCase(RWTypeRuleCategory.Round)
				||methodName.equalsIgnoreCase(RWTypeRuleCategory.Floor)
				||methodName.equalsIgnoreCase(RWTypeRuleCategory.Ceil)
				){
					associateAttSetsWithExp(methodInvocationNode, argumentAnnotatedType);
					return;
				}
				String returnType = null;
				returnType=this.rwtRulesManager.getReturnType(this.currentProject, argumentAnnotatedType, methodName,RWType.TypeLess); 
				if((returnType != null)){
					this.associateAttSetsWithExp(methodInvocationNode, returnType);
				}else{
					addNewErrorMessage(methodInvocationNode , ErrorUtil.getUndeclaredCalculation(methodInvocationNode.toString()),  DiagnosticMessage.WARNING);
					this.associateAttSetsWithExp(methodInvocationNode,  RWType.error_source);	
				}
			}
		}
	}
	
	private void checkInfixExpression(IASTBinaryExpression infixExpression){
		IASTExpression leftEP = infixExpression.getOperand1();			
		IASTExpression rightEP = infixExpression.getOperand2();
		if(leftEP.resolveTypeBinding().getBinaryName().equals("java.lang.String") 
				|| rightEP.resolveTypeBinding().getBinaryName().equals("java.lang.String")){
			return;
		}
		String CMTypeAnnotatedTypeOne = this.getAnnotatedTypeForExpression(leftEP);
		String CMTypeAnnotatedTypeTwo = this.getAnnotatedTypeForExpression(rightEP);
				if((CMTypeAnnotatedTypeOne.equals(RWType.error_source)) || (CMTypeAnnotatedTypeOne.equals(RWType.error_propogate))
						||	(CMTypeAnnotatedTypeTwo.equals(RWType.error_source)) || (CMTypeAnnotatedTypeTwo.equals(RWType.error_propogate))){
					this.associateAttSetsWithExp(infixExpression, RWType.error_propogate);
					return;
				}
				if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
					this.associateAttSetsWithExp(infixExpression, RWType.UnknownType);
				}

		if(thisop.equals(InfixExpression.Operator.REMAINDER)){
			check_Remander_Operation(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, infixExpression, 0);
		}
		if(thisop.equals(InfixExpression.Operator.PLUS)){
			check_Plus_Minus_Operation(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, infixExpression, 0, RWTypeRuleCategory.Plus);
		}else if(thisop.equals(InfixExpression.Operator.MINUS)){
			check_Plus_Minus_Operation(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, infixExpression, 0, RWTypeRuleCategory.Subtraction);
		}
		else if(thisop.equals(InfixExpression.Operator.TIMES)){
			check_Times_Operation(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, infixExpression, 0);
		}else if(thisop.equals(InfixExpression.Operator.DIVIDE)){
			check_Division_Operation(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, infixExpression, 0);
		}
	}
	
	private void check_Remander_Operation(String CMTypeAnnotatedTypeOne, String CMTypeAnnotatedTypeTwo, InfixExpression infixExpression, int extendedIndex){
		String infixExpressionType = RWType.UnknownType;
			//type rules part
			if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				infixExpressionType = RWType.UnknownType;
			}		
			else if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				addNewErrorMessage(infixExpression , ErrorUtil.getRemanderDimensionError(), DiagnosticMessage.ERROR);
				infixExpressionType = RWType.UnknownType;
			}
			else if(!CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				infixExpressionType = CMTypeAnnotatedTypeOne;
			}else{
				String returnType = null;
				returnType = this.rwtRulesManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.REMAINDER, CMTypeAnnotatedTypeTwo);
				if(returnType != null ){
					infixExpressionType = returnType;
				}else{
					addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
					infixExpressionType = RWType.UnknownType;
				}	
			}
			this.associateAttSetsWithExp(infixExpression, infixExpressionType);

		if(infixExpression.hasExtendedOperands()){
			if(infixExpression.extendedOperands().size()>extendedIndex){
				Expression extendedOperand = (Expression)(infixExpression.extendedOperands().get(extendedIndex));
				String CMTypeForNewOperand = this.getAnnotatedTypeForExpression(extendedOperand);
				extendedIndex++;
				check_Remander_Operation(infixExpressionType, CMTypeForNewOperand, infixExpression, extendedIndex);
			}
		}
	}
	
	private void check_Plus_Minus_Operation(String CMTypeAnnotatedTypeOne, String CMTypeAnnotatedTypeTwo, InfixExpression plusInfixExpression, int extendedIndex, String operation_type){
		
		String infixExpressionType = RWType.UnknownType;
				if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
					infixExpressionType = RWType.UnknownType;
				}		
				else if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
					infixExpressionType = CMTypeAnnotatedTypeTwo;
				}
				else if(!CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
					infixExpressionType = CMTypeAnnotatedTypeOne;
				}else{
					String returnType = null;
					returnType = this.rwtRulesManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, operation_type, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
						//if no match found for the calculations
						if(!CMTypeAnnotatedTypeOne.equalsIgnoreCase(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equalsIgnoreCase(RWType.UnknownType)){							
							addNewErrorMessage(plusInfixExpression , ErrorUtil.getUndeclaredCalculation(plusInfixExpression.toString()), DiagnosticMessage.WARNING);
							infixExpressionType = RWType.UnknownType; //change type
							
						}
					}	
				}
				this.associateAttSetsWithExp(plusInfixExpression, infixExpressionType);
		
		if(plusInfixExpression.hasExtendedOperands()){
			if(plusInfixExpression.extendedOperands().size()>extendedIndex){
				Expression extendedOperand = (Expression)(plusInfixExpression.extendedOperands().get(extendedIndex));
				String CMTypeForNewOperand = this.getAnnotatedTypeForExpression(extendedOperand);
				extendedIndex++;
				check_Plus_Minus_Operation(infixExpressionType, CMTypeForNewOperand, plusInfixExpression, extendedIndex, operation_type);
			}
		}
	}
	
	private void check_Times_Operation(String CMTypeAnnotatedTypeOne, String CMTypeAnnotatedTypeTwo, InfixExpression infixExpression, int extendedIndex){
		String infixExpressionType = RWType.UnknownType;
				//type rules part
				if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
					infixExpressionType = RWType.UnknownType;
				}		
				else if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
					infixExpressionType = CMTypeAnnotatedTypeTwo;
				}
				else if(!CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
					infixExpressionType = CMTypeAnnotatedTypeOne;
				}else{
					String returnType = null;
					returnType = this.rwtRulesManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.Multiplication, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
						if(!CMTypeAnnotatedTypeOne.equalsIgnoreCase(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equalsIgnoreCase(RWType.UnknownType)){							
							addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
							infixExpressionType = RWType.UnknownType;
						}
					}
				}
				this.associateAttSetsWithExp(infixExpression, infixExpressionType);
		if(infixExpression.hasExtendedOperands()){
			if(infixExpression.extendedOperands().size()>extendedIndex){
				Expression extendedOperand = (Expression)(infixExpression.extendedOperands().get(extendedIndex));
				String CMTypeForNewOperand = this.getAnnotatedTypeForExpression(extendedOperand);
				extendedIndex++;
				check_Times_Operation(infixExpressionType, CMTypeForNewOperand, infixExpression, extendedIndex);
			}
		}
	}
	
	private void check_Division_Operation(String CMTypeAnnotatedTypeOne, String CMTypeAnnotatedTypeTwo,InfixExpression infixExpression, int extendedIndex){	
		String infixExpressionType = RWType.UnknownType;
		//type rules part
		if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
			infixExpressionType = RWType.UnknownType;
		}		
		else if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
			String inverseType = this.rwtRulesManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeTwo, RWTypeRuleCategory.Multiplicative_Inverse, "");
			if(inverseType != null ){
				infixExpressionType = inverseType;
			}else{
				addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
				infixExpressionType = RWType.UnknownType;
			}
		}
		else if(!CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
			infixExpressionType = CMTypeAnnotatedTypeOne;
		}else{
			String returnType = null;
			returnType = this.rwtRulesManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.Division, CMTypeAnnotatedTypeTwo);
			if(returnType != null ){
				infixExpressionType = returnType;
			}else{
				addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
				infixExpressionType = RWType.UnknownType;
			}
		}
		this.associateAttSetsWithExp(infixExpression, infixExpressionType);
		if(infixExpression.hasExtendedOperands()){
			if(infixExpression.extendedOperands().size()>extendedIndex){
				Expression extendedOperand = (Expression)(infixExpression.extendedOperands().get(extendedIndex));
				String CMTypeForNewOperand = this.getAnnotatedTypeForExpression(extendedOperand);
				extendedIndex++;
				check_Division_Operation(infixExpressionType, CMTypeForNewOperand, infixExpression, extendedIndex);
			}
		}
	}

	public String getAnnotatedTypeForExpression(IASTNode exp){
		if(rwtypeTableForExp.get(exp) != null && 
				rwtypeTableForExp.get(exp).length()>0){
			this.accessRWT = true;
			return rwtypeTableForExp.get(exp);
		}else{
			rwtypeTableForExp.put(exp, RWType.TypeLess);
			return RWType.TypeLess;
		}
	}

	private void associateAttSetsWithExp(IASTNode astNode, String annotatedType){
		if(annotatedType != null && annotatedType.length()>0){
			RWType cmtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, annotatedType);
			if(cmtype!=null){
				String rwttypeAttSet =  cmtype.getEnabledAttributeSet();
				if(checkingUnits){
					rwttypeAttSet = cmtype.getUnitsAttribute();	
				}
				this.rwtypeTableForExp.put(astNode, rwttypeAttSet);
				if(!CTypeCheckerVisitor.cmtypeHashSet.contains(annotatedType)){
					CTypeCheckerVisitor.cmtypeHashSet.add(annotatedType);
				}
			}else{
				this.rwtypeTableForExp.put(astNode, annotatedType);	
			}
			if(annotatedType.length()>0){
				this.accessRWT=true;
			}
		}
	}
	
	public void setErrorReports(ArrayList<DiagnosticMessage> errorReports) {
		this.errorReports = errorReports;
	}

	private void addNewErrorMessage(IASTNode node, String errorMessageDetail, String errorType){
		DiagnosticMessage errorMessage = new DiagnosticMessage();
		errorMessage.setMessageType(errorType);
		errorMessage.setMessageDetail(errorMessageDetail);
		errorMessage.setContextInfo("");
		errorMessage.setcErrorNode(node);
		this.errorReports.add(errorMessage);
	}
	
	public boolean isAccessRWT() {
		return accessRWT;
	}

	public ArrayList<DiagnosticMessage> getErrorReports() {
		return errorReports;
	}
	
}