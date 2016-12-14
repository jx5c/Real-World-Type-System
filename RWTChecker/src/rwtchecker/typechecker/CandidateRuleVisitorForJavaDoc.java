package rwtchecker.typechecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.rwt.RWType;
import rwtchecker.rwtrules.RWTypeRule;
import rwtchecker.rwtrules.RWTypeRuleCategory;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.DiagnosticMessage;
import rwtchecker.util.ErrorUtil;

public class CandidateRuleVisitorForJavaDoc extends ASTVisitor {
	
	private static String generatedRuleMarker = "@";
	
	public static HashSet<String> cmtypeHashSet = new HashSet<String>();
	
	private RWTypeRulesManager cmTypeOperationManager;
	private RWTypeRulesManager candidateRuleManager;
	
	private ArrayList<DiagnosticMessage> errorReports = new ArrayList<DiagnosticMessage>();
	
	private HashMap<Expression, String> annotatedTypeTableForExpression = new HashMap<Expression, String>();
	
	private CompilationUnit compilationUnit;
	
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	private ArrayList<String> usedAnnotatedVariables = new ArrayList<String>();
	
	private IPath currentFilePath;
	private IFile currentFile;
	private IProject currentProject;
	
	private ArrayList<Expression> markingNodes = new ArrayList<Expression>();
	
	private boolean parsingMethodDelcMode = false;
	private boolean insideTargetedMethod = false;
	private String targetedMethodDeclKey = "";
	
	private String returnCMTypesForTargetedMethod = RWType.TypeLess;
	private boolean methodInvError = false;
	private boolean insideBranch = false;
	private boolean errorInsideBranch = false;
	
	private int annotatedCourt = 0;
	public int getAnnotatedCourt() {
		return annotatedCourt;
	}

	private int variableCourt = 0;
	public int getVariableCourt() {
		return variableCourt;
	}
	
	public CandidateRuleVisitorForJavaDoc(RWTypeRulesManager manager, CompilationUnit compilationUnit) {
		super(true);
		this.cmTypeOperationManager = manager;
		this.compilationUnit = compilationUnit;
		
		currentFilePath = this.compilationUnit.getJavaElement().getPath();
		currentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(currentFilePath);
		currentProject = currentFile.getProject();
		File annotationFile = RWTSystemUtil.getAnnotationFile(currentFile);
		if(annotationFile!= null && annotationFile.exists()){
			fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			if(fileAnnotations == null){
				annotatedCourt = 0;
				return;
			}
			fileAnnotations.load(allVariableMap, methodReturnMap, null, null);
			annotatedCourt = fileAnnotations.getAllAnnotationsCount();
		}else{
			annotatedCourt = 0;
		}
		this.errorReports.clear();
		this.candidateRuleManager = RWTypeRulesManager.getCandidateRuleManager(currentFile.getName());
		this.candidateRuleManager.clear();
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
				}else{
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
				}
				associateAttSetsWithExp((SimpleName)node, thisCMType);
			}
				
			else if(fbinding instanceof IMethodBinding){
				IMethodBinding iMethodBinding = (IMethodBinding) fbinding;
				String returnType = null;
				if(node.getParent() instanceof MethodInvocation){
					String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
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
				String unitedSetsType = NewTypeCheckerVisitor.uniteTwoSets(qualifierType, nameType, cmTypeOperationManager);
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
				String unitedSetsType = NewTypeCheckerVisitor.uniteTwoSets(annotatedFieldAccessType, annotatedIdentifierType, cmTypeOperationManager);
				associateAttSetsWithExp(fieldAccessNode, unitedSetsType);
			}
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
			this.checkMethodInvocation(methodInvocationNode);
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
					String returnType = this.cmTypeOperationManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Assignable, rightCMType);
					if(returnType != null){
						return;
					}
				}
				
			    if(!leftCMType.equalsIgnoreCase(RWType.UnknownType) &&
						!leftCMType.equalsIgnoreCase(rightCMType) && 
						!rightCMType.equalsIgnoreCase(RWType.UnknownType) ){
					RWTypeRule newUVRule = new RWTypeRule(RWTypeRuleCategory.Assignable, leftCMType,  rightCMType, "", RWTypeRule.notVerified);
					this.candidateRuleManager.addCMTypeOperation(newUVRule);
				}
			}else if(operator.equals(Assignment.Operator.PLUS_ASSIGN)){
				String returnType = this.cmTypeOperationManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Plus, rightCMType);
				if(returnType != null){
					if(returnType.equals(leftCMType)){
						return;
					}else{
						String assignableType = this.cmTypeOperationManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Assignable, returnType);
						if(assignableType != null){
							return;
						}else{
							addNewErrorMessage(node , ErrorUtil.unknownCalculation(), DiagnosticMessage.WARNING);
						}
					}
				}
			}
			
			//right now, we do not include type inference here
			/*
			if(leftCMType.equals(CMType.TypeLess) && !rightCMType.equals(CMType.TypeLess)){
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
						String dimensionInfo = this.getDimensionInfoForExpression(rightExp);
						String unitInfo = this.getUnitsInfoForExpression(rightExp);
						addTypeInfo(leftExp, dimensionInfo, unitInfo, leftCMType);
					}
				}
			}
			*/
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
					String returnType = this.cmTypeOperationManager.getReturnType(currentProject, leftCMType, RWTypeRuleCategory.Assignable, rightCMType);
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
				/*
				if(leftCMType.equals(CMType.TypeLess) && !rightCMType.equals(CMType.TypeLess)){
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
							String dimensionInfo = this.getDimensionInfoForExpression(rightExp);
							String unitInfo = this.getUnitsInfoForExpression(rightExp);
							addTypeInfo(leftExp, dimensionInfo, unitInfo, leftCMType);
						}
					}
				}
				*/
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
			String returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, prefixExpressionType, operatorType, RWType.TypeLess);				
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
		CandidateRuleVisitorForJavaDoc methodDeclVisitor = new CandidateRuleVisitorForJavaDoc(this.cmTypeOperationManager, targetedCompilationUnit);
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
						if(!parameterCMtype.getEnabledAttributeSet().equals(argument_cmtypes[i])){
							String parameterCMtypeAttSet = parameterCMtype.getEnabledAttributeSet();
							String tempReturnType = cmTypeOperationManager.getReturnType(currentProject, parameterCMtypeAttSet, RWTypeRuleCategory.Assignable,  argument_cmtypes[i]);
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
		}
		for (int i=0;i<methodInvocationNode.arguments().size();i++){
			SingleVariableDeclaration parameterDeclaration = (SingleVariableDeclaration)(methodDecl.parameters().get(i));
			String parameterTypeName = fileAnnotations.getCMTypeInBodyDecl(methodKey, parameterDeclaration.getName().getIdentifier());
			if(parameterTypeName == null){
				continue;
			}
			if(parameterTypeName != null){
				RWType parameterCMtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, parameterTypeName);
				if(parameterCMtype!=null){
					if(argument_cmtypes[i].length()==0){
						//do we add error here?
						addNewErrorMessage(methodInvocationNode , ErrorUtil.methodArgumentError(), DiagnosticMessage.WARNING);
						break;
					}
					if(!parameterCMtype.getEnabledAttributeSet().equals(argument_cmtypes[i])){
						String parameterCMtypeAttSet = parameterCMtype.getEnabledAttributeSet();
						String tempReturnType = cmTypeOperationManager.getReturnType(currentProject, parameterCMtypeAttSet, RWTypeRuleCategory.Assignable,  argument_cmtypes[i]);
						if(tempReturnType==null){
							addNewErrorMessage(methodInvocationNode , ErrorUtil.methodArgumentError(), DiagnosticMessage.ERROR);
							break;
						}
					}
				}
			}
		}
		return returnCMTypeAtt;
	}
	
	private void checkMethodInvocation(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
		String methodDeclPath = iMethodBinding.getJavaElement().getPath().toString();
		String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
		final String[] argumentCMTypes = new String[methodInvocationNode.arguments().size()];
		for (int i=0;i<methodInvocationNode.arguments().size();i++){
			Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
			String argumentCMType = this.getAnnotatedTypeForExpression(exp);
			argumentCMTypes[i] = argumentCMType;
		}
		ASTNode declaringNode = this.compilationUnit.findDeclaringNode(methodDeclKey);
		
		final String expCMType = this.getAnnotatedTypeForExpression(methodInvocationNode.getExpression());
		FileAnnotations fileAnnotations = null;
		CompilationUnit targetedCompilationUnit = null;
		if(currentUnitPath.equals(methodDeclPath)){
			fileAnnotations = this.fileAnnotations;
			targetedCompilationUnit = this.compilationUnit;
		}
		else {
			IFile methodDeclFile = ResourcesPlugin.getWorkspace().getRoot().getFile(iMethodBinding.getJavaElement().getPath());
			if((methodDeclFile != null) && (methodDeclFile.getFileExtension().toLowerCase().endsWith("java"))){
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(methodDeclFile);
				parser.setSource(icompilationUnit); // set source
				parser.setResolveBindings(true);
				targetedCompilationUnit = (CompilationUnit) parser.createAST(null);
				declaringNode = targetedCompilationUnit.findDeclaringNode(methodDeclKey);
				File annotationFile = RWTSystemUtil.getAnnotationFile(methodDeclFile);
				if(annotationFile != null){
					fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
				}
			}
			
		}
		MethodDeclaration methodDecl = (MethodDeclaration)declaringNode;
		if(fileAnnotations!=null && fileAnnotations.getReturnCMTypeForMethod(methodDeclKey)!=null){
			String returnType = checkReturnCMType(methodDeclKey,methodDecl,methodInvocationNode,argumentCMTypes,fileAnnotations);
			if(returnType.equalsIgnoreCase(RWType.GenericMethod)){
				returnType = handleGenericMethod(targetedCompilationUnit,methodInvocationNode,methodDeclKey,argumentCMTypes);
			}
			if(expCMType.equals(RWType.TypeLess)){
				associateAttSetsWithExp(methodInvocationNode, returnType);	
			}else{
				String finalReturnType = NewTypeCheckerVisitor.uniteTwoSets(expCMType, returnType, cmTypeOperationManager);
				associateAttSetsWithExp(methodInvocationNode, finalReturnType);
			}
			return;
		}
	}
		
	//hard coding the math default invocation
	private void checkMathMethodInvocation(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
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
					String returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, argumentOneAnnotatedType, methodName, argumentTwoAnnotatedType); 
					if(returnType != null){
						this.associateAttSetsWithExp(methodInvocationNode,  returnType);
						return;
					}else {
						if(methodName.equals(RWTypeRuleCategory.Power)){
							String synthetizedOpName = methodName;
							String synthetizedResultType = argumentOneAnnotatedType;
							RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentOneAnnotatedType,  argumentTwoAnnotatedType, synthetizedResultType, RWTypeRule.notVerified);
							this.candidateRuleManager.addCMTypeOperation(newUVRule);	
							this.associateAttSetsWithExp(methodInvocationNode,  synthetizedResultType);
							return;
						}
						
						String synthetizedOpName = methodName;
						String synthetizedResultType = synthetizedOpName+ "(" + argumentOneAnnotatedType + generatedRuleMarker + argumentTwoAnnotatedType + ")";
						RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentOneAnnotatedType,  argumentTwoAnnotatedType, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);	
						this.associateAttSetsWithExp(methodInvocationNode,  synthetizedResultType);
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
				String returnType = null;
				returnType=this.cmTypeOperationManager.getReturnType(this.currentProject, argumentAnnotatedType, methodName,RWType.TypeLess); 
				if((returnType != null)){
					this.associateAttSetsWithExp(methodInvocationNode, returnType);
				}else{
					String synthetizedOpName = methodName;
					//abs function
					if(methodName.equalsIgnoreCase(RWTypeRuleCategory.Abosolute_Value)){
						associateAttSetsWithExp(methodInvocationNode, argumentAnnotatedType);
					}
					String synthetizedResultType = argumentAnnotatedType;
					RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentAnnotatedType,  RWType.TypeLess, synthetizedResultType, RWTypeRule.notVerified);
					this.candidateRuleManager.addCMTypeOperation(newUVRule);	
					this.associateAttSetsWithExp(methodInvocationNode,  synthetizedResultType);
				}
			}
		}
	}
	
	private void checkInfixExpression(InfixExpression infixExpression){
		Expression leftEP = infixExpression.getLeftOperand();			
		Expression rightEP = infixExpression.getRightOperand();
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
			if((!CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) ) 
					&& !CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)
					&& !CMTypeAnnotatedTypeOne.equals(CMTypeAnnotatedTypeTwo)){
				String operation_type = RWTypeRuleCategory.Comparable; 
				String synthetizedResultType = "";
				RWTypeRule newUVRule = new RWTypeRule(operation_type, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
				this.candidateRuleManager.addCMTypeOperation(newUVRule);	
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
				returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.REMAINDER, CMTypeAnnotatedTypeTwo);
				if(returnType != null ){
					infixExpressionType = returnType;
				}else{
					String synthetizedOpName = RWTypeRuleCategory.REMAINDER; 
//					String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker +operation_type+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
					String synthetizedResultType = synthetizedOpName+ "(" + CMTypeAnnotatedTypeOne + generatedRuleMarker + CMTypeAnnotatedTypeTwo + ")";
					RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
					this.candidateRuleManager.addCMTypeOperation(newUVRule);
					infixExpressionType = synthetizedResultType;
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
					returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, operation_type, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
//						String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker +operation_type+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
						String synthetizedResultType = operation_type+ "(" + CMTypeAnnotatedTypeOne + generatedRuleMarker + CMTypeAnnotatedTypeTwo + ")";
						RWTypeRule newUVRule = new RWTypeRule(operation_type, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);
						infixExpressionType = synthetizedResultType;
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
					returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.Multiplication, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
//						String synthetizedResultType = CMTypeAnnotatedTypeOne +  generatedRuleMarker +CMTypeRuleCategory.Multiplication+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
						String synthetizedResultType = RWTypeRuleCategory.Multiplication+ "(" + CMTypeAnnotatedTypeOne + generatedRuleMarker + CMTypeAnnotatedTypeTwo + ")";
						RWTypeRule newUVRule = new RWTypeRule(RWTypeRuleCategory.Multiplication, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);
						infixExpressionType = synthetizedResultType;
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
			String inverseType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeTwo, RWTypeRuleCategory.Multiplicative_Inverse, "");
			if(inverseType != null ){
				infixExpressionType = inverseType;
			}else{
//				String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker +CMTypeRuleCategory.Division+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
				String synthetizedResultType = RWTypeRuleCategory.Division+ "(" + CMTypeAnnotatedTypeOne + generatedRuleMarker + CMTypeAnnotatedTypeTwo + ")";
				RWTypeRule newUVRule = new RWTypeRule(RWTypeRuleCategory.Division, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
				this.candidateRuleManager.addCMTypeOperation(newUVRule);
				infixExpressionType = synthetizedResultType;
			}
		}
		else if(!CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
			infixExpressionType = CMTypeAnnotatedTypeOne;
		}else{
			String returnType = null;
			returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.Division, CMTypeAnnotatedTypeTwo);
			if(returnType != null ){
				infixExpressionType = returnType;
			}else{
//				String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker+CMTypeRuleCategory.Division+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
				String synthetizedResultType = RWTypeRuleCategory.Division+ "(" + CMTypeAnnotatedTypeOne + generatedRuleMarker + CMTypeAnnotatedTypeTwo + ")";
				RWTypeRule newUVRule = new RWTypeRule(RWTypeRuleCategory.Division, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
				this.candidateRuleManager.addCMTypeOperation(newUVRule);
				infixExpressionType = synthetizedResultType;
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
		if(annotatedTypeTableForExpression.get(exp) != null){
			return annotatedTypeTableForExpression.get(exp);
		}else{
			annotatedTypeTableForExpression.put(exp, RWType.TypeLess);
			return RWType.TypeLess;
		}
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
		
		if( node instanceof CompilationUnit){
			this.candidateRuleManager.storeRules();
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
		errorMessage.setJavaErrorNode(node);
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
				this.annotatedTypeTableForExpression.put(exp, cmtype.getEnabledAttributeSet());
				if(!CandidateRuleVisitorForJavaDoc.cmtypeHashSet.contains(annotatedType)){
					CandidateRuleVisitorForJavaDoc.cmtypeHashSet.add(annotatedType);
				}
			}else{
				this.annotatedTypeTableForExpression.put(exp, annotatedType);	
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
	
}