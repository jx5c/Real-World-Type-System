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
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CBasicType;
import org.eclipse.cdt.internal.core.dom.parser.c.CVariable;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPArrayType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPBasicType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPVariable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.rwt.RWT_Attribute;
import rwtchecker.rwt.RWType;
import rwtchecker.rwtrules.RWTypeRuleCategory;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.typechecker.NewTypeCheckerVisitor;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.DiagnosticMessage;
import rwtchecker.util.ErrorUtil;

public class CTypeCheckerVisitor extends ASTVisitor {
	
	public static HashSet<String> cmtypeHashSet = new HashSet<String>();
	
	private RWTypeRulesManager rwtRulesManager;
	
	private ArrayList<DiagnosticMessage> errorReports = new ArrayList<DiagnosticMessage>();
	
	private HashMap<IASTExpression, String> rwtypeTableForExp = new HashMap<IASTExpression, String>();
	
	private IASTTranslationUnit compilationUnit;
	
	private boolean accessRWT = false;
	
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	
	//for annotations
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	//for invariants checking
	Map<String, Map<String, HashSet<String>>> allInvAttToRecordMap = new HashMap<String, Map<String, HashSet<String>>>();
	Map<String, ArrayList<String>> allInvariantsMap = new HashMap<String, ArrayList<String>>();
	
	private IFile currentFile;
	private IProject currentProject;
	
	private boolean parsingMethodDelcMode = false;
	private boolean insideTargetedMethod = false;
	private String targetedMethodDeclKey = "";
	
	private String returnCMTypesForTargetedMethod = RWType.TypeLess;
	private boolean methodInvError = false;
	private boolean insideBranch = false;
	private boolean errorInsideBranch = false;

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
		
	}

	@Override
	public int visit(IASTExpression exp){
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
	
	public int visit(IASTName name){
		IBinding fbinding = name.resolveBinding();
		if (fbinding instanceof CVariable){
			CVariable varName = (CVariable)fbinding;
			System.out.println(varName.getDefinition());
			IType aType = varName.getType();
			if(aType instanceof CBasicType){
				CPPBasicType basicType = (CPPBasicType)aType;
				if(basicType.getKind() == Kind.eInt || basicType.getKind() == Kind.eInt128 ){
//					System.out.println("Integer");
					IASTExpression exp = (IASTExpression)(varName.getPhysicalNode());
				}else if(basicType.getKind() == Kind.eFloat ){
//					System.out.println("float");
				}else if(basicType.getKind() == Kind.eDouble ){
//					System.out.println("double");
				}
			}
			if(aType instanceof CPPArrayType){
				System.out.println("array");
			}
		}
		return 3;
	}

	public int visit(IASTDeclaration declaration){
	System.out.println("declaration: " + declaration + " ->  " + declaration.getRawSignature());

	if ((declaration instanceof IASTSimpleDeclaration)) {
		IASTSimpleDeclaration ast = (IASTSimpleDeclaration)declaration;
		try
		{
			System.out.println("--- type: " + ast.getSyntax() + " (childs: " + ast.getChildren().length + ")");
			IASTNode typedef = ast.getChildren().length == 1 ? ast.getChildren()[0] : ast.getChildren()[1];
	             System.out.println("------- typedef: " + typedef);
	             IASTNode[] children = typedef.getChildren();
	             if ((children != null) && (children.length > 0))
	               System.out.println("------- typedef-name: " + children[0].getRawSignature());
       }
       catch (ExpansionOverlapsBoundaryException e)
       {
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
	    IScope scope = ast.getScope();
	    try{
	             System.out.println("### function() - Parent = " + scope.getParent().getScopeName());
	             System.out.println("### function() - Syntax = " + ast.getSyntax());
	    }catch (DOMException e) {
	             e.printStackTrace();
	    } catch (ExpansionOverlapsBoundaryException e) {
	             e.printStackTrace();
	    }
	    ICPPASTFunctionDeclarator typedef = (ICPPASTFunctionDeclarator)ast.getDeclarator();
	    System.out.println("------- typedef: " + typedef.getName());
	 }
	 return 3;
	 }
	 
       public int visit(IASTTypeId typeId)
       {
         System.out.println("typeId: " + typeId.getRawSignature());
         return 3;
       }
 
       public int visit(IASTStatement statement)
       {
         System.out.println("statement: " + statement.getRawSignature());
         return 3;
      }
 
    public int visit(IASTAttribute attribute){
         return 3;
    }	       
	
	
	public void preVisit(ASTNode node) {
		if(parsingMethodDelcMode){
			if(this.fileAnnotations.getAnnotations().size() == 0){
				return;
			}
		}
		
		if(node instanceof MethodDeclaration){
			MethodDeclaration methodDelNode = (MethodDeclaration)node;
			String methodKey = methodDelNode.resolveBinding().getKey();
			//if method declaration has a return type binding
			if(!this.methodReturnMap.containsKey(methodKey)
					||
					this.methodReturnMap.get(methodKey).equals(RWType.GenericMethod)){
				if(this.parsingMethodDelcMode){
					if(methodKey.equals(this.targetedMethodDeclKey)){
						this.insideTargetedMethod = true;
					}
				}
			}
			return;
		}
		
		//read annotations in
		if(node instanceof SimpleName){
			IBinding fbinding = ((SimpleName)node).resolveBinding();
			if(fbinding instanceof IVariableBinding){
				IVariableBinding variableBinding = (IVariableBinding) fbinding;
				String thisCMType = null;
				if(variableBinding.isField()){
					String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
					if(variableBinding.getJavaElement()==null){
						return;
					}
					String classDeclPath = variableBinding.getJavaElement().getPath().toString();
					String classDeclKey = variableBinding.getDeclaringClass().getKey();
					if(currentUnitPath.equals(classDeclPath)){
						Map<String, String> variableMap = this.allVariableMap.get(classDeclKey);
						if(variableMap!=null){
							thisCMType = variableMap.get(variableBinding.getName());
						}
					}else{
						IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(variableBinding.getJavaElement().getPath());
						File otherSourceFileAnnotationFile = RWTSystemUtil.getAnnotationFile(ifile);
						if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
							FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
							if(otherSourcefileAnnotation == null){
								return;
							}
							thisCMType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
						}
					}
					this.setInvariantVal(node.toString(), thisCMType, classDeclKey);
				}else{
					if(variableBinding.getDeclaringMethod()!=null){
						String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
						if(parsingMethodDelcMode && !insideTargetedMethod){
							return;
						}else{
							Map<String, String> variableMap = this.allVariableMap.get(methodDeclKey);
							if(variableMap!=null){
								thisCMType = variableMap.get(variableBinding.getName());
								if(thisCMType != null){
									if(!usedAnnotatedVariables.contains(variableBinding.getName())){
										usedAnnotatedVariables.add(variableBinding.getName());	
									}
								}
							}
						}
						this.setInvariantVal(node.toString(), thisCMType, methodDeclKey);
					}
				}
				associateAttSetsWithExp((SimpleName)node, thisCMType);
			}
				
			else if(fbinding instanceof IMethodBinding){
				IMethodBinding iMethodBinding = (IMethodBinding) fbinding;
				String returnType = null;
				if(node.getParent() instanceof MethodInvocation){
					String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
					if(iMethodBinding.getJavaElement()!=null){
						String methodDeclUnitPath = iMethodBinding.getJavaElement().getPath().toString();
						String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
						if(currentUnitPath.equals(methodDeclUnitPath)){
							if(methodReturnMap!=null){
								returnType = methodReturnMap.get(methodDeclKey);
							}
						}else{
							//Method declared in other file; 
							IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(iMethodBinding.getJavaElement().getPath());
							File otherSourceFileAnnotationFile = RWTSystemUtil.getAnnotationFile(ifile);
							if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
								FileAnnotations otherSourcefileAnnotationsClone = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
								if(otherSourcefileAnnotationsClone == null){
									return;
								}
								returnType = otherSourcefileAnnotationsClone.getReturnCMTypeForMethod(methodDeclKey);
							}
						}						
					}
		 		}
			}
		}
		
		if(node instanceof ThisExpression){
			usedAnnotatedVariables.add("this");
			String thisCMType = null;
			ASTNode parentNode = node.getParent();
            while(!(parentNode instanceof MethodDeclaration) && !(parentNode instanceof TypeDeclaration)){
            	parentNode = parentNode.getParent();
            }
            if(parentNode instanceof MethodDeclaration){
            	MethodDeclaration methodDeclaration = (MethodDeclaration)parentNode;
    			Map<String, String> methodVariableMap = this.allVariableMap.get(methodDeclaration.resolveBinding().getKey());
    			if(methodVariableMap!=null){
    				if(methodVariableMap.containsKey("this")){
    					thisCMType = methodVariableMap.get("this");
    				}
    			}
    			if(thisCMType == null){
    				ThisExpression thisExp = (ThisExpression)node;
    				ITypeBinding declaredClassBinding = thisExp.resolveTypeBinding();
    				Map<String, String> variableMap = this.allVariableMap.get(declaredClassBinding.getKey());
        			if(variableMap!=null){
        				if(variableMap.containsKey("this")){
        					thisCMType = variableMap.get("this");
        				}
        			}
    			}
            }else if(parentNode instanceof TypeDeclaration){
        		TypeDeclaration typeDeclaration = (TypeDeclaration)parentNode;
    			Map<String, String> classVariableMap = this.allVariableMap.get(typeDeclaration.resolveBinding().getKey());
    			if(classVariableMap!=null){
    				if(classVariableMap.containsKey("this")){
    					thisCMType = classVariableMap.get("this");	
    				}
    			}
            }
			if(thisCMType!=null){
				this.associateAttSetsWithExp((ThisExpression)node,  thisCMType);
			}
		}
	}
	
	@Override
	public boolean visit(MethodDeclaration methodDeclaration){
		String methodKey = methodDeclaration.resolveBinding().getKey();
		if(parsingMethodDelcMode){
			if(!methodKey.equals(targetedMethodDeclKey)){
				//not target method body; do not proceed
				return false;
			}			
		}
		return true;
	}
	
	@Override
	public boolean visit(IfStatement ifStatement){
		if(parsingMethodDelcMode){
			//already in a branch and errors are found
			if(insideBranch && errorInsideBranch){
				return false;
			}else{
				insideBranch = true;	
			}
		}
		return true;
	}
	
	public void EndVisitNode(ASTNode node){
		if(node instanceof MethodDeclaration){
			MethodDeclaration methodDecl = (MethodDeclaration)node;
			String methodKey = methodDecl.resolveBinding().getKey();
			if(methodKey.equals(this.targetedMethodDeclKey)){
				//out of the method
				this.insideTargetedMethod = false;
			}
			if(methodDecl != null){
				Map<String, String> variableMap = this.allVariableMap.get(methodKey);
				if(variableMap!=null){
					Set<String> variables = variableMap.keySet();
					for(String variable:variables){
						if(this.usedAnnotatedVariables.contains(variable)){
							continue;
						}else{
							addNewErrorMessage(methodDecl.getJavadoc() , "The variable "+ variable + " annotated is not used in the method declaration", DiagnosticMessage.ERROR);
						}
					}
				}
			}
		}
		
		else if(node instanceof QualifiedName){
			QualifiedName qualifiedName = (QualifiedName)node;
			String qualifierType =  this.getAnnotatedTypeForExpression(qualifiedName.getQualifier());
			String nameType =  this.getAnnotatedTypeForExpression(qualifiedName.getName());
			if(!NewTypeCheckerVisitor.checkConsistency(qualifierType, nameType)){
				//inconsistent attributes
				this.addNewErrorMessage(qualifiedName, ErrorUtil.getInconsistentAttributeError(),DiagnosticMessage.ERROR);
				associateAttSetsWithExp(qualifiedName, nameType);
			}else{
				String unitedSetsType = NewTypeCheckerVisitor.uniteTwoSets(qualifierType, nameType, rwtRulesManager);
				associateAttSetsWithExp(qualifiedName, unitedSetsType);
			}
		}
		
		else if(node instanceof FieldAccess){
			FieldAccess fieldAccessNode = (FieldAccess)node;
			String annotatedFieldAccessType = this.getAnnotatedTypeForExpression(fieldAccessNode.getExpression());
			String annotatedIdentifierType = this.getAnnotatedTypeForExpression(fieldAccessNode.getName());
			//using union operation for the two types
			if(!NewTypeCheckerVisitor.checkConsistency(annotatedFieldAccessType, annotatedIdentifierType)){
				//inconsistent attributes
				this.addNewErrorMessage(fieldAccessNode, ErrorUtil.getInconsistentAttributeError(),DiagnosticMessage.ERROR);
				associateAttSetsWithExp(fieldAccessNode, annotatedIdentifierType);
			}else{
				String unitedSetsType = NewTypeCheckerVisitor.uniteTwoSets(annotatedFieldAccessType, annotatedIdentifierType, rwtRulesManager);
				associateAttSetsWithExp(fieldAccessNode, unitedSetsType);
			}
		}
		
		else if(node instanceof CastExpression){
			CastExpression castExp = (CastExpression)node;
			this.associateAttSetsWithExp(castExp, this.getAnnotatedTypeForExpression(castExp.getExpression()));
		}
		
		else if(node instanceof ArrayAccess){
			ArrayAccess arrayAccess = (ArrayAccess)node;
			String annotatedType =  this.getAnnotatedTypeForExpression(arrayAccess.getArray());
			String resultType = annotatedType;
			this.associateAttSetsWithExp(arrayAccess, resultType);
		}
		
		else if(node instanceof InfixExpression){
			InfixExpression infixExpressionNode = (InfixExpression)node;
			checkInfixExpression(infixExpressionNode);
		}
		
		else if(node instanceof MethodInvocation){
			MethodInvocation methodInvocationNode = (MethodInvocation)node;
			this.checkMathMethodInvocation(methodInvocationNode);
//			this.checkCollectionAccess(methodInvocationNode);
			this.checkAllInvocation(methodInvocationNode.resolveMethodBinding(),methodInvocationNode.arguments(),node);
		}
		else if(node instanceof ParenthesizedExpression){
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression)node;
			String argumentCMType = this.getAnnotatedTypeForExpression(parenthesizedExpression.getExpression());
			this.associateAttSetsWithExp(parenthesizedExpression, argumentCMType);
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
		
		//constructor calling
		if(node instanceof ClassInstanceCreation){
			ClassInstanceCreation cic = (ClassInstanceCreation)node;
			this.checkAllInvocation(cic.resolveConstructorBinding(),cic.arguments(),node);
		}
		
		if(node instanceof SuperConstructorInvocation){
			SuperConstructorInvocation sci = (SuperConstructorInvocation)node;
			this.checkAllInvocation(sci.resolveConstructorBinding(),sci.arguments(),node);
		}
		
		if(node instanceof ConstructorInvocation){
			ConstructorInvocation cic = (ConstructorInvocation)node;
			this.checkAllInvocation(cic.resolveConstructorBinding(),cic.arguments(),node);
		}
		
		
		//error checking here
		else if(node instanceof Assignment){
			Assignment assignmentNode = (Assignment)node;
			Expression leftExp = assignmentNode.getLeftHandSide();
			Expression rightExp = assignmentNode.getRightHandSide();
			String leftCMType = this.getAnnotatedTypeForExpression(leftExp);
			String rightCMType = this.getAnnotatedTypeForExpression(rightExp);
			Assignment.Operator operator = assignmentNode.getOperator();
			if(operator.equals(Assignment.Operator.ASSIGN)){
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
					addNewErrorMessage(node , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);	
				}
			}else if(operator.equals(Assignment.Operator.PLUS_ASSIGN)){
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
			
			//simple inference here
			//if we are in units checking, do inference here 
			if(this.checkingUnits){
				if(leftCMType.equals(RWType.TypeLess) && !rightCMType.equals(RWType.TypeLess) 
						&& !rightCMType.equals(RWType.error_propogate) && !rightCMType.equals(RWType.error_source)){
					if(leftExp instanceof SimpleName){
						IBinding fbinding = ((SimpleName)leftExp).resolveBinding();
						if(fbinding instanceof IVariableBinding){
							IVariableBinding variableBinding = (IVariableBinding) fbinding;
							if(variableBinding.getDeclaringMethod()!=null){
								String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
								Map<String, String> variableMap = this.allVariableMap.get(methodDeclKey);
								if(variableMap == null){
									variableMap = new HashMap<String, String>();
									this.allVariableMap.put(methodDeclKey, variableMap);
								}
								variableMap.put(variableBinding.getName(), rightCMType);	
							}
						}
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
		
		else if(node instanceof PrefixExpression){
			PrefixExpression prefixExp = (PrefixExpression)node;
			PrefixExpression.Operator operator = prefixExp.getOperator();
			String argumentCMType = this.getAnnotatedTypeForExpression(prefixExp.getOperand());
			String prefixExpressionType = argumentCMType;
			if(prefixExpressionType.equals(RWType.TypeLess)){
				associateAttSetsWithExp(prefixExp, prefixExpressionType);
				return;
			}
			String operatorType = "";
			if(operator.equals(PrefixExpression.Operator.MINUS)){
				operatorType = RWTypeRuleCategory.Unary_minus;

			}else if(operator.equals(PrefixExpression.Operator.PLUS)){
				operatorType = RWTypeRuleCategory.Unary_plus;
			}
			String returnType = this.rwtRulesManager.getReturnType(this.currentProject, prefixExpressionType, operatorType, RWType.TypeLess);				
			if(returnType != null ){
				associateAttSetsWithExp(prefixExp, returnType);	
			}else{
				associateAttSetsWithExp(prefixExp, prefixExpressionType);
			}
		}
		
		else if(node instanceof ConditionalExpression){
			ConditionalExpression conditionalExpression = (ConditionalExpression)node;
			Expression thenExp = conditionalExpression.getThenExpression();
			Expression elseExp = conditionalExpression.getElseExpression();
			String thenAnnotatedType = this.getAnnotatedTypeForExpression(thenExp);
			String elseAnnotatedType = this.getAnnotatedTypeForExpression(elseExp);
		}
	}
	
//	private void checkCollectionAccess(MethodInvocation methodInvocationNode) {
//		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
//		//check for list access
//		if(iMethodBinding.getMethodDeclaration().getName().equals("get")) {
//			if(iMethodBinding.getDeclaringClass().getBinaryName().equals("java.util.ArrayList") ||
//   			   iMethodBinding.getDeclaringClass().getBinaryName().equals("java.util.List")){
//				String dimensionInfo = this.getDimensionInfoForExpression(methodInvocationNode.getExpression());
//				String unitInfo = this.getUnitsInfoForExpression(methodInvocationNode.getExpression());
//				addTypeInfo(methodInvocationNode, dimensionInfo, unitInfo, this.getAnnotatedTypeForExpression(methodInvocationNode.getExpression()));
//			}
//		}
//	}

	private String handleGenericMethod(CompilationUnit targetedCompilationUnit, MethodInvocation methodInvocationNode, String methodDeclKey, String[] argument_cmtypes){
		//without annotated cm type, polymorphic
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
	
	private void organizeInvAttValMap(HashSet<String> invariants, HashMap<String, ArrayList<String>> invAttValMap){
		for(String invariant : invariants){
			if(invariant.split("=")[1].indexOf("(")==-1){
				//"unit(lat1)=radians";
				String leftPart = invariant.split("=")[0];
				int pos = leftPart.indexOf("(");
				String varName = FileAnnotations.getRidOfParenthesis(leftPart.substring(pos));
				String attName = invariant.substring(0, pos);
				if(!invAttValMap.containsKey(varName)){
					invAttValMap.put(varName, new ArrayList<String>());
				}
				invAttValMap.get(varName).add(attName);
			}else{
				//"unit(lat1)=unit(lat2)"
				String leftPart = invariant.split("=")[0];
				int pos1 = leftPart.indexOf("(");
				String var1 = FileAnnotations.getRidOfParenthesis(leftPart.substring(pos1));
				String att1 = leftPart.substring(0, pos1);
				if(!invAttValMap.containsKey(var1)){
					invAttValMap.put(var1, new ArrayList<String>());
				}
				invAttValMap.get(var1).add(att1);
				String rightPart = invariant.split("=")[1];
				int pos2 = rightPart.indexOf("(");
				String var2 = FileAnnotations.getRidOfParenthesis(rightPart.substring(pos2));
				String att2 = rightPart.substring(0, pos2);
				if(!invAttValMap.containsKey(var2)){
					invAttValMap.put(var2, new ArrayList<String>());
				}
				invAttValMap.get(var2).add(att2);
			}
		}
	}
	
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
		
		MethodDeclaration methodDecl = (MethodDeclaration)declaringNode;
		final String[] argumentCMTypes = new String[arguments.size()];

		//prepare for checking invariants
		HashSet<String> invariants = fileAnnotations.getInvariantsOfBodyDecl(bodyDeclKey);
		HashMap<String, ArrayList<String>> valToRecordMap = new HashMap<String, ArrayList<String>>();
		organizeInvAttValMap(invariants,valToRecordMap);
		
		//for checking invariants
		HashMap<String, String> invAttValMap = new HashMap<String, String>();
		
		for (int i=0;i<arguments.size();i++){
			Expression exp = (Expression)(arguments.get(i));
			String argumentCMType = this.getAnnotatedTypeForExpression(exp);
			argumentCMTypes[i] = argumentCMType;
			SingleVariableDeclaration parameterDeclaration = (SingleVariableDeclaration)(methodDecl.parameters().get(i));
			String parameterName = parameterDeclaration.getName().getIdentifier();
			String parameterTypeName = fileAnnotations.getCMTypeInBodyDecl(bodyDeclKey, parameterName);
			if(parameterTypeName == null || parameterTypeName.length()==0){
				continue;
			}
			RWType parameterCMtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, parameterTypeName);
			if(parameterCMtype!=null){
				String parameterCMtypeAttSet = parameterCMtype.getEnabledAttributeSet();
				if(checkingUnits){
					parameterCMtypeAttSet = parameterCMtype.getUnitsAttribute();
				}
				if(argumentCMType.length()==0){
					//do we add error here? when we don't have the type information for the arguments
					//addNewErrorMessage(node , ErrorUtil.methodArgumentError(), DiagnosticMessage.WARNING);
					//break;
					continue;
				}
				if(!parameterCMtypeAttSet.equals(argumentCMType)){
					String tempReturnType = rwtRulesManager.getReturnType(currentProject, parameterCMtypeAttSet, RWTypeRuleCategory.Assignable,  argumentCMType);
					if(tempReturnType==null){
						addNewErrorMessage(node , ErrorUtil.methodArgumentError(), DiagnosticMessage.ERROR);
						break;
					}
				}	
			}
			//save contenst for checking invariants
			if(valToRecordMap.containsKey(parameterName)){
				//get the value of the attributes
				String attStr = argumentCMTypes[i];
				//get the value of the needed attribute
				if(attStr.length()>0){
					 String[] attVals = attStr.split(";");
					 ArrayList<String> atts = valToRecordMap.get(parameterName);
					 for(String attValCombo : attVals){
						 if(attValCombo.split("=").length==2){
							 String attName = attValCombo.split("=")[0];
							 String attVal = attValCombo.split("=")[1];
							 if(atts.contains(attName)){
								 invAttValMap.put(attName+"("+parameterName+")", attVal);
							 }
						 }
					 }
				}
			}
		}
		
		//check invariants
		for(String invariant :invariants){
			if(invariant.split("=")[1].indexOf("(")==-1){
				//"unit(lat1)=radians";
				String leftPart = invariant.split("=")[0];
				String rightPart = invariant.split("=")[1];
				if(invAttValMap.containsKey(leftPart) && !invAttValMap.get(leftPart).equals(rightPart)){
					//invariant is being violated
					addNewErrorMessage(node, ErrorUtil.invariantViolation(invariant),  DiagnosticMessage.ERROR);
				}
			}else{
				//"unit(lat1)=unit(lat2)"
				String leftPart = invariant.split("=")[0];
				String rightPart = invariant.split("=")[1];
				if(invAttValMap.containsKey(leftPart) && invAttValMap.containsKey(rightPart)){
					if(!invAttValMap.get(leftPart).equals(invAttValMap.get(rightPart))){
						addNewErrorMessage(node, ErrorUtil.invariantViolation(invariant),  DiagnosticMessage.ERROR);
					}
				}
			}
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
	
	private void checkInfixExpression(InfixExpression infixExpression){
		Expression leftEP = infixExpression.getLeftOperand();			
		Expression rightEP = infixExpression.getRightOperand();
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
		Operator thisop = infixExpression.getOperator();
		if((thisop.equals(InfixExpression.Operator.LESS))
		||(thisop.equals(InfixExpression.Operator.LESS_EQUALS))
		||(thisop.equals(InfixExpression.Operator.GREATER))
		||(thisop.equals(InfixExpression.Operator.GREATER_EQUALS))
		||(thisop.equals(InfixExpression.Operator.EQUALS))
		||(thisop.equals(InfixExpression.Operator.NOT_EQUALS))
		){
			if(CMTypeAnnotatedTypeOne.equals(CMTypeAnnotatedTypeTwo)||
					CMTypeAnnotatedTypeOne.equals(RWType.UnknownType)||
					CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				return;
			}else{
				String returnType = null;
				returnType = this.rwtRulesManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.Comparable, CMTypeAnnotatedTypeTwo);
				if(returnType != null ){
					return;
				}else{
					addNewErrorMessage(infixExpression , ErrorUtil.typeInconsistency(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo), DiagnosticMessage.ERROR);
				}
			}
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

	public String getAnnotatedTypeForExpression(Expression exp){
		if(annotatedTypeTableForExpression.get(exp) != null 
				&& 
				annotatedTypeTableForExpression.get(exp).length()>0){
			this.accessRWT = true;
			return annotatedTypeTableForExpression.get(exp);
		}else{
			annotatedTypeTableForExpression.put(exp, RWType.TypeLess);
			return RWType.TypeLess;
		}
	}
	
	public boolean isAccessRWT() {
		return accessRWT;
	}

	public ArrayList<DiagnosticMessage> getErrorReports() {
		return errorReports;
	}
	
	@Override
	public void postVisit(ASTNode node) {
		if(parsingMethodDelcMode){
			if(this.fileAnnotations.getAnnotations().size() == 0){
				return;
			}
		}
		EndVisitNode(node);
		
		//court the variables
		if(node instanceof SingleVariableDeclaration){
			variableCourt ++;
//			System.out.println(node);
		}
		
		if(node instanceof NumberLiteral){
			variableCourt ++;
//			System.out.println(node);
		}
		
		if(node instanceof FieldDeclaration){
			variableCourt ++;
//			System.out.println(node);
		}
		
		if(node instanceof MethodDeclaration){
			variableCourt ++;
//			System.out.println(node);
		}
		
		if(node instanceof VariableDeclarationStatement){
			VariableDeclarationStatement variableDeclarationStatementNode = (VariableDeclarationStatement)node;
			variableCourt = variableCourt + variableDeclarationStatementNode.fragments().size();
//			System.out.println(node);
		}
	}
	
	public void setErrorReports(ArrayList<DiagnosticMessage> errorReports) {
		this.errorReports = errorReports;
	}

	private void addNewErrorMessage(ASTNode node, String errorMessageDetail, String errorType){
		DiagnosticMessage errorMessage = new DiagnosticMessage();
		errorMessage.setMessageType(errorType);
		errorMessage.setMessageDetail(errorMessageDetail);
		errorMessage.setContextInfo("");
		errorMessage.setErrorNode(node);
		this.errorReports.add(errorMessage);
		if(insideBranch){
			//in a branch, error found
			errorInsideBranch = true;
		}
	}
	
	private void associateAttSetsWithExp(Expression exp, String annotatedType){
		if(annotatedType != null){
			RWType cmtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, annotatedType);
			if(cmtype!=null){
				String rwttypeAttSet =  cmtype.getEnabledAttributeSet();
				if(checkingUnits){
					rwttypeAttSet = cmtype.getUnitsAttribute();	
				}
				this.annotatedTypeTableForExpression.put(exp, rwttypeAttSet);
				if(!CTypeCheckerVisitor.cmtypeHashSet.contains(annotatedType)){
					CTypeCheckerVisitor.cmtypeHashSet.add(annotatedType);
				}
			}else{
				this.annotatedTypeTableForExpression.put(exp, annotatedType);	
			}
			if(annotatedType.length()>0){
				this.accessRWT=true;
			}
		}
	}
	public ArrayList<Expression> getMarkingNodes() {
		return markingNodes;
	}
	
	private void assignReturnTypeForMethodInv(String newReturnType){
		if(newReturnType.equals(RWType.TypeLess)){
			return;
		}
		if(this.returnCMTypesForTargetedMethod.length()>0  && !this.returnCMTypesForTargetedMethod.equals(newReturnType) ){
			this.methodInvError = true;
			return;
		}
		if(this.returnCMTypesForTargetedMethod.length()== 0){
			this.returnCMTypesForTargetedMethod = newReturnType;
		}
	}
	
	private void setInvariantVal(String variable, String typeName, String bodyDeclKey){
		if(typeName == null || typeName.length() == 0){
			return;
		}
		if(allInvAttToRecordMap.containsKey(bodyDeclKey) && allInvAttToRecordMap.get(bodyDeclKey).containsKey(variable)){
			HashSet<String> invAttsToRecord = allInvAttToRecordMap.get(bodyDeclKey).get(variable);
			RWType cmtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, typeName);
			if(cmtype!=null){
				for(RWT_Attribute att : cmtype.getSemanticType().getSemanticTypeAttributes()){
					if(invAttsToRecord.contains(att.getAttributeName())){
						String key = att.getAttributeName()+"("+variable+")";
						String val = att.getAttributeValue();
						if(!allInvAttValMap.containsKey(bodyDeclKey)){
							allInvAttValMap.put(bodyDeclKey, new HashMap<String, String>());
						}
						this.allInvAttValMap.get(bodyDeclKey).put(key, val);
					}
				}
			}else{
				String[] atts = typeName.split(";");
				for(String attValCombo : atts){
					if(attValCombo.split("=").length==2){
						String attName = attValCombo.split("=")[0];
						String attVal = attValCombo.split("=")[1];
						if(invAttsToRecord.contains(attName)){
							String key = attName+"("+variable+")";
							String val = attVal;
							if(!allInvAttValMap.containsKey(bodyDeclKey)){
								allInvAttValMap.put(bodyDeclKey, new HashMap<String, String>());
							}
							this.allInvAttValMap.get(bodyDeclKey).put(key, val);
						}
					}
				}
			}
		}
	}
}