package cmtypechecker.util.interval;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import cmtypechecker.CM.CMType;
import cmtypechecker.CMRules.CMTypeRuleCategory;
import cmtypechecker.CMRules.CMTypeRulesManager;
import cmtypechecker.annotation.FileAnnotations;
import cmtypechecker.annotation.RWTAnnotation;
import cmtypechecker.typechecker.NewTypeCheckerVisitor;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.util.DiagnosticMessage;
import cmtypechecker.util.ErrorUtil;

public class IntervalAnalysisVisitor extends ASTVisitor {
	
	public static HashSet<String> cmtypeHashSet = new HashSet<String>();
	
	private ArrayList<DiagnosticMessage> errorReports = new ArrayList<DiagnosticMessage>();
	private HashMap<Expression, RealInterval> expIntvalMap = new HashMap<Expression, RealInterval>();
	private CompilationUnit compilationUnit;
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	private Map<String, Map<String, RealInterval>> allVarIntvalMap = new HashMap<String, Map<String, RealInterval>>();
	
	private IPath currentFilePath;
	private IFile currentFile;
	private IProject currentProject;
	
	public IntervalAnalysisVisitor(CompilationUnit compilationUnit) {
		super(true);
		this.compilationUnit = compilationUnit;
		
		currentFilePath = this.compilationUnit.getJavaElement().getPath();
		currentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(currentFilePath);
		currentProject = currentFile.getProject();
		File annotationFile = CMModelUtil.getAnnotationFile(currentFile);
		if(annotationFile!= null && annotationFile.exists()){
			fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			if(fileAnnotations == null){
				return;
			}
			fileAnnotations.load(allVariableMap, methodReturnMap, null, null);
			loadIntvals(allVariableMap,allVarIntvalMap);
		}
		this.errorReports.clear();
	}
	
	private void loadIntvals(Map<String, Map<String, String>> allVariableMap, Map<String, Map<String, RealInterval>> allVarIntvalMap){
		Set<String> keys = allVariableMap.keySet();
		for(String bodyDelKey:keys){
			Map<String, String> varRWTMap =  allVariableMap.get(bodyDelKey);
			Map<String , RealInterval> varIntvalMap = new HashMap<String, RealInterval>();
			for(String var : varRWTMap.keySet()){
				String annotatedType = varRWTMap.get(var);
				if(annotatedType != null){
					CMType cmtype = CMModelUtil.getCMTypeFromTypeName(currentProject, annotatedType);
					if(cmtype!=null && cmtype.getInterval()!=null){
						varIntvalMap.put(var, cmtype.getInterval());
					}
				}
			}
			allVarIntvalMap.put(bodyDelKey, varIntvalMap);
		}
	}

	public void preVisit(ASTNode node) {
		
		
		if(node instanceof NumberLiteral){
			NumberLiteral num = (NumberLiteral)node;
			RealInterval intval = null;
			String t = num.getToken();
			if (t.endsWith("L") || t.endsWith("l")) {
				try{
					t = Long.toString(Long.parseLong(num.getToken().substring(0, num.getToken().length() - 1)));	
				}catch(java.lang.NumberFormatException e){
				}
			}
			/*
			if(binding.getKey().equals("J")){
				long lNum = Long.valueOf(num.getToken());
				intval = new RealInterval(lNum);		
			}else{
			*/
				try{
					double dNum = Double.parseDouble(t);
					intval = new RealInterval(dNum);
				}catch(java.lang.NumberFormatException e){
				}
			this.bindIntervalWithExp(num, intval);
		}
		
		//read intervals
		if(node instanceof SimpleName){
			SimpleName SN =(SimpleName)node; 
			if(SN.resolveConstantExpressionValue()!=null){
				try{
					double val = Double.parseDouble(SN.resolveConstantExpressionValue().toString());
					if(!String.valueOf(val).equals("NaN") && !String.valueOf(val).equals("Infinity") && !String.valueOf(val).equals("-Infinity")){
						this.bindIntervalWithExp((SimpleName)node, new RealInterval(val));	
					}
				}catch(java.lang.NumberFormatException e){
				}
			}
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
						Map<String, RealInterval> varIntvalMap = this.allVarIntvalMap.get(classDeclKey);
						if(varIntvalMap!=null){
							RealInterval intval = varIntvalMap.get(variableBinding.getName());
							this.bindIntervalWithExp((SimpleName)node, intval);
						}
					}else{
						IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(variableBinding.getJavaElement().getPath());
						File otherSourceFileAnnotationFile = CMModelUtil.getAnnotationFile(ifile);
						if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
							FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
							if(otherSourcefileAnnotation == null){
								return;
							}
							thisCMType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
						}
						if(thisCMType != null){
							CMType cmtype = CMModelUtil.getCMTypeFromTypeName(currentProject, thisCMType);
							if(cmtype!=null && cmtype.getInterval()!=null){
								bindIntervalWithExp((SimpleName)node, cmtype.getInterval());
							}
						}
					}
				}else{
					if(variableBinding.getDeclaringMethod()!=null){
						String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
						Map<String, RealInterval> varIntvalMap = this.allVarIntvalMap.get(methodDeclKey);
						if(varIntvalMap!=null){
							RealInterval intval = varIntvalMap.get(variableBinding.getName());
							//bind the expression with interval
							this.bindIntervalWithExp((SimpleName)node, intval);
						}	
					}
				}					
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
						File otherSourceFileAnnotationFile = CMModelUtil.getAnnotationFile(ifile);
						if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
							FileAnnotations otherSourcefileAnnotationsClone = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
							if(otherSourcefileAnnotationsClone == null){
								return;
							}
							returnType = otherSourcefileAnnotationsClone.getReturnCMTypeForMethod(methodDeclKey);
						}
					}
		 		}
				if(returnType != null){
					CMType cmtype = CMModelUtil.getCMTypeFromTypeName(currentProject, returnType);
					if(cmtype!=null && cmtype.getInterval()!=null){
						bindIntervalWithExp((SimpleName)node, cmtype.getInterval());
					}
				}
			}
		}
	}
	
	public void EndVisitNode(ASTNode node){
		
		if(node instanceof CastExpression){
			CastExpression ce = (CastExpression)node;
			bindIntervalWithExp(ce,getIntervalForExpression(ce.getExpression()));
		}
		
		if(node instanceof QualifiedName){
			QualifiedName qualifiedName = (QualifiedName)node;
			//for IA arithmetic
			bindIntervalWithExp(qualifiedName,getIntervalForExpression(qualifiedName.getName()));
		}
		
		else if(node instanceof FieldAccess){
			FieldAccess fieldAccessNode = (FieldAccess)node;
			//for IA arithmetic
			bindIntervalWithExp(fieldAccessNode,getIntervalForExpression(fieldAccessNode.getName()));
		}
		
		else if(node instanceof ArrayAccess){
			ArrayAccess arrayAccess = (ArrayAccess)node;
			//for IA arithmetic
			bindIntervalWithExp(arrayAccess,getIntervalForExpression(arrayAccess.getArray()));
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
			bindIntervalWithExp(parenthesizedExpression, this.getIntervalForExpression(parenthesizedExpression.getExpression()));
		}
		
		else if(node instanceof ReturnStatement){
			ReturnStatement returnStatementNode = (ReturnStatement)node;
		}
		//error checking here
		else if(node instanceof Assignment){
			Assignment assignmentNode = (Assignment)node;
			Expression leftExp = assignmentNode.getLeftHandSide();
			Expression rightExp = assignmentNode.getRightHandSide();
			RealInterval leftIntval = this.getIntervalForExpression(leftExp);
			RealInterval rightIntval = this.getIntervalForExpression(rightExp);
			Assignment.Operator operator = assignmentNode.getOperator();
			
			if(rightIntval==null){
				return;
			}
			if(leftIntval == null){
				//simple inference here
				if(leftExp instanceof SimpleName){
					IBinding fbinding = ((SimpleName)leftExp).resolveBinding();
					if(fbinding instanceof IVariableBinding){
						IVariableBinding variableBinding = (IVariableBinding) fbinding;
						if(variableBinding.getDeclaringMethod()!=null){
							String methodDeclKey = null;
							if(variableBinding.isField()){
								methodDeclKey = variableBinding.getDeclaringClass().getKey();
							}else{
								methodDeclKey = variableBinding.getDeclaringMethod().getKey();
							}
							Map<String, RealInterval> varIntvalMap = this.allVarIntvalMap.get(methodDeclKey);
							if(varIntvalMap == null){
								varIntvalMap = new HashMap<String, RealInterval>();
							}
							leftIntval = rightIntval;
							varIntvalMap.put(variableBinding.getName(), leftIntval);
							this.allVarIntvalMap.put(methodDeclKey, varIntvalMap);
							this.bindIntervalWithExp(leftExp, leftIntval);							
						}
					}
				}
				return;
			}
			
			if(operator.equals(Assignment.Operator.ASSIGN)){
				if(rightIntval == null || leftIntval==null){
					return;
				}
				else if(leftIntval.lo > rightIntval.lo || leftIntval.hi < rightIntval.hi){
					if(leftExp instanceof SimpleName){
						IBinding fbinding = ((SimpleName)leftExp).resolveBinding();
						if(fbinding instanceof IVariableBinding){
							IVariableBinding variableBinding = (IVariableBinding) fbinding;
							if(hasRWT(variableBinding)){
								//leave it now, maybe later
								//addNewErrorMessage(node , ErrorUtil.inconsistAssignment(leftIntval, rightIntval), DiagnosticMessage.WARNING);			
							}
						}
					}					
				}
			}else if(operator.equals(Assignment.Operator.PLUS_ASSIGN) ||
					operator.equals(Assignment.Operator.MINUS_ASSIGN) ||
					operator.equals(Assignment.Operator.TIMES_ASSIGN) ||
					operator.equals(Assignment.Operator.DIVIDE_ASSIGN)){
				//how to deal with these?
				if(rightIntval != null && leftIntval!=null){
					//leave it for now
					//addNewErrorMessage(node , ErrorUtil.possibleOutRangeAssignment(leftIntval, rightIntval), DiagnosticMessage.WARNING);
					return;
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
				RealInterval leftIntval = this.getIntervalForExpression(fragment.getName());
				RealInterval rightIntval = this.getIntervalForExpression(fragment.getInitializer());
				if(rightIntval == null){
					return;
				}else if(leftIntval==null){
					//simple inference here
					if(fragment.getName() instanceof SimpleName){
						IBinding fbinding = ((SimpleName)fragment.getName()).resolveBinding();
						if(fbinding instanceof IVariableBinding){
							IVariableBinding variableBinding = (IVariableBinding) fbinding;
							String methodDeclKey = null;
							if(variableBinding.isField()){
								methodDeclKey = variableBinding.getDeclaringClass().getKey();
							}else{
								methodDeclKey = variableBinding.getDeclaringMethod().getKey();
							}
							Map<String, RealInterval> varIntvalMap = this.allVarIntvalMap.get(methodDeclKey);
							if(varIntvalMap == null){
								varIntvalMap = new HashMap<String, RealInterval>();
							}
							leftIntval = rightIntval;
							varIntvalMap.put(variableBinding.getName(), leftIntval);
							this.allVarIntvalMap.put(methodDeclKey, varIntvalMap);
							this.bindIntervalWithExp(fragment.getName(), leftIntval);								
						}
					}
					return;
				}else if(leftIntval.lo > rightIntval.lo || leftIntval.hi < rightIntval.hi){
					if(fragment.getName() instanceof SimpleName){
						IBinding fbinding = ((SimpleName)fragment.getName()).resolveBinding();
						if(fbinding instanceof IVariableBinding){
							IVariableBinding variableBinding = (IVariableBinding) fbinding;
							if(hasRWT(variableBinding)){
								addNewErrorMessage(node , ErrorUtil.inconsistAssignment(leftIntval, rightIntval), DiagnosticMessage.WARNING);			
							}
						}
					}
				}
			}
		}
		
		else if(node instanceof FieldDeclaration){
			FieldDeclaration fieldDeclaration = (FieldDeclaration)node;
			for (Iterator iter = fieldDeclaration.fragments().iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
				if(fragment.getInitializer()==null){
					continue;
				}
				RealInterval leftIntval = this.getIntervalForExpression(fragment.getName());
				RealInterval rightIntval = this.getIntervalForExpression(fragment.getInitializer());
				if(rightIntval == null){
					return;
				}else if(leftIntval==null){
					//simple inference here
					if(fragment.getName() instanceof SimpleName){
						IBinding fbinding = ((SimpleName)fragment.getName()).resolveBinding();
						if(fbinding instanceof IVariableBinding){
							IVariableBinding variableBinding = (IVariableBinding) fbinding;
							String methodDeclKey = null;
							if(variableBinding.isField()){
								methodDeclKey = variableBinding.getDeclaringClass().getKey();
							}else{
								methodDeclKey = variableBinding.getDeclaringMethod().getKey();
							}
							Map<String, RealInterval> varIntvalMap = this.allVarIntvalMap.get(methodDeclKey);
							if(varIntvalMap == null){
								varIntvalMap = new HashMap<String, RealInterval>();
							}
							leftIntval = rightIntval;
							varIntvalMap.put(variableBinding.getName(), leftIntval);
							this.allVarIntvalMap.put(methodDeclKey, varIntvalMap);
							this.bindIntervalWithExp(fragment.getName(), leftIntval);
						}
					}
					return;
				}else if(leftIntval.lo > rightIntval.lo || leftIntval.hi < rightIntval.hi){
					if(fragment.getName() instanceof SimpleName){
						IBinding fbinding = ((SimpleName)fragment.getName()).resolveBinding();
						if(fbinding instanceof IVariableBinding){
							IVariableBinding variableBinding = (IVariableBinding) fbinding;
							if(hasRWT(variableBinding)){
								addNewErrorMessage(node , ErrorUtil.inconsistAssignment(leftIntval, rightIntval), DiagnosticMessage.WARNING);			
							}
						}
					}
				}
			}
		}
		
		
		else if(node instanceof PrefixExpression){
			PrefixExpression prefixExp = (PrefixExpression)node;
			PrefixExpression.Operator operator = prefixExp.getOperator();
			RealInterval arugmentIntval = this.getIntervalForExpression(prefixExp.getOperand());
			if(arugmentIntval==null){
				return;
			}
			String operatorType = "";
			if(operator.equals(PrefixExpression.Operator.INCREMENT)){
				RealInterval resultIntval = IAMath.add(arugmentIntval, new RealInterval(1));
				this.bindIntervalWithExp(prefixExp, resultIntval);
			}else if(operator.equals(PrefixExpression.Operator.DECREMENT)){
				RealInterval resultIntval = IAMath.add(arugmentIntval, new RealInterval(1));
				this.bindIntervalWithExp(prefixExp, resultIntval);
			}
		}
		
		else if(node instanceof ConditionalExpression){
			ConditionalExpression conditionalExpression = (ConditionalExpression)node;
			Expression thenExp = conditionalExpression.getThenExpression();
			Expression elseExp = conditionalExpression.getElseExpression();
			RealInterval thenIntval = this.getIntervalForExpression(thenExp);
			RealInterval elseIntval = this.getIntervalForExpression(elseExp);
			if(thenIntval == null || elseIntval==null){
				return;
			}
			this.bindIntervalWithExp(conditionalExpression, IAMath.union(thenIntval,elseIntval));
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
	
	private RealInterval checkReturnCMType(String methodKey, MethodDeclaration methodDecl, MethodInvocation methodInvocationNode, RealInterval[] argumentsIntvals, FileAnnotations fileAnnotations){
		if(fileAnnotations==null){
			return null;
		}
		String returnCMtypeName = fileAnnotations.getReturnCMTypeForMethod(methodKey);
		if(returnCMtypeName.equalsIgnoreCase(CMType.GenericMethod)){
			return null;
		}
		CMType returnCMtype = CMModelUtil.getCMTypeFromTypeName(currentProject, returnCMtypeName);		
		for (int i=0;i<methodInvocationNode.arguments().size();i++){
			SingleVariableDeclaration parameterDeclaration = (SingleVariableDeclaration)(methodDecl.parameters().get(i));
			String parameterTypeName = fileAnnotations.getCMTypeInBodyDecl(methodKey, parameterDeclaration.getName().getIdentifier());
			if(parameterTypeName == null){
				continue;
			}
			if(parameterTypeName != null){
				CMType parameterCMtype = CMModelUtil.getCMTypeFromTypeName(currentProject, parameterTypeName);
				if(parameterCMtype!=null){
					RealInterval paraIntval = parameterCMtype.getInterval();
					RealInterval arguIntval = argumentsIntvals[i]; 
					if(arguIntval == null || paraIntval == null){
						//do we add error here?
						//addNewErrorMessage(methodInvocationNode , ErrorUtil.methodArgumentError(), DiagnosticMessage.WARNING);
						continue;
					}
					if(arguIntval.lo < paraIntval.lo || arguIntval.hi > paraIntval.hi){
						addNewErrorMessage(methodInvocationNode , ErrorUtil.methodIntervalError(), DiagnosticMessage.WARNING);
						break;
					}
				}
			}
		}
		if(returnCMtype!= null){
			return returnCMtype.getInterval();
		}
		return null;
	}
	
	private void checkMethodInvocation(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
		String methodDeclPath = iMethodBinding.getJavaElement().getPath().toString();
		String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
		final RealInterval[] argumentIntervals = new RealInterval[methodInvocationNode.arguments().size()];
		for (int i=0;i<methodInvocationNode.arguments().size();i++){
			Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
			RealInterval argumentIntval = this.getIntervalForExpression(exp);
			argumentIntervals[i] = argumentIntval;
		}
		ASTNode declaringNode = this.compilationUnit.findDeclaringNode(methodDeclKey);
		
		//final String expCMType = this.getIntervalForExpression(methodInvocationNode.getExpression());
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
				File annotationFile = CMModelUtil.getAnnotationFile(methodDeclFile);
				if(annotationFile != null){
					fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
				}
			}
		}
		MethodDeclaration methodDecl = (MethodDeclaration)declaringNode;
		if(fileAnnotations!=null && fileAnnotations.getReturnCMTypeForMethod(methodDeclKey)!=null){
			RealInterval returnIntval = checkReturnCMType(methodDeclKey,methodDecl,methodInvocationNode,argumentIntervals,fileAnnotations);
			this.bindIntervalWithExp(methodInvocationNode, returnIntval);
		}
	}
		
	//hard coding the math default invocation
	private void checkMathMethodInvocation(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		String binaryClassName = iMethodBinding.getDeclaringClass().getBinaryName();
		RealInterval resultIntval = null;
		if(binaryClassName.equals("java.lang.Math")){
			String methodName = "java.lang.Math."+iMethodBinding.getName();
			if(methodInvocationNode.arguments().size() == 2){
				Expression argumentOne = (Expression)(methodInvocationNode.arguments().get(0));	
				Expression argumentTwo = (Expression)(methodInvocationNode.arguments().get(1));	
				
				RealInterval argumentOneIntval = this.getIntervalForExpression(argumentOne);
				RealInterval argumentTwoIntval = this.getIntervalForExpression(argumentTwo);
					if(argumentOneIntval == null || argumentTwoIntval==null){
						return;
					}
					
					if(methodName.equals(CMTypeRuleCategory.Power)){
						try{
							resultIntval = IAMath.power(argumentOneIntval, argumentTwoIntval);
						}catch(IAException e){
							//do not know what to do; if argument one is negative
						}
					}else if(methodName.equals(CMTypeRuleCategory.Arc_Tangent2)){
						try{
							RealInterval tmp = IAMath.div(argumentOneIntval, argumentTwoIntval);
							resultIntval = IAMath.atan(tmp);
						}catch(IAException e){
							resultIntval = new RealInterval(Math.PI/2);
						}
						
					}else if(methodName.equals(CMTypeRuleCategory.Max)){
						resultIntval = IAMath.max(argumentOneIntval, argumentTwoIntval);
					}else if(methodName.equals(CMTypeRuleCategory.Min)){
						resultIntval = IAMath.min(argumentOneIntval, argumentTwoIntval);
					}
					
			}
			if(methodInvocationNode.arguments().size() == 1){
				Expression argument = (Expression)(methodInvocationNode.arguments().get(0));
				RealInterval argumentIntval = this.getIntervalForExpression(argument);

				if(argumentIntval==null){
					return;
				}
				
				if(methodName.equals(CMTypeRuleCategory.Natural_Logarithm)){
					resultIntval = IAMath.log(argumentIntval);
				}else if(methodName.equals(CMTypeRuleCategory.Abosolute_Value)){
					resultIntval = argumentIntval;
				}else if(methodName.equals(CMTypeRuleCategory.Arc_Cosine)){
					resultIntval = IAMath.acos(argumentIntval);
				}else if(methodName.equals(CMTypeRuleCategory.Arc_Sine)){
					resultIntval = IAMath.asin(argumentIntval);
				}else if(methodName.equals(CMTypeRuleCategory.Arc_Tangent)){
					resultIntval = IAMath.atan(argumentIntval);
				}else if(methodName.equals(CMTypeRuleCategory.Ceil)){
					resultIntval = argumentIntval;
				}else if(methodName.equals(CMTypeRuleCategory.Cosine)){
					resultIntval = IAMath.cos(argumentIntval);
				}else if(methodName.equals(CMTypeRuleCategory.Exp)){
					resultIntval = IAMath.exp(argumentIntval);
				}else if(methodName.equals(CMTypeRuleCategory.Floor)){
					resultIntval = argumentIntval;
				}else if(methodName.equals(CMTypeRuleCategory.Sine)){
					resultIntval = IAMath.sin(argumentIntval);
				}else if(methodName.equals(CMTypeRuleCategory.Sqrt)){
					resultIntval = IAMath.oddPower(argumentIntval,0.5);
				}else if(methodName.equals(CMTypeRuleCategory.RadiansToDegree)){
					resultIntval = IAMath.mul(argumentIntval, new RealInterval(180/Math.PI));
				}else if(methodName.equals(CMTypeRuleCategory.DegreeToRadians)){
					resultIntval = IAMath.mul(argumentIntval, new RealInterval(Math.PI/180));
				}else if(methodName.equals(CMTypeRuleCategory.Tangent)){
					resultIntval = IAMath.tan(argumentIntval);
				}
			}
		}
		this.bindIntervalWithExp(methodInvocationNode, resultIntval);
	}
	
	private void checkInfixExpression(InfixExpression infixExpression){
		Expression leftEP = infixExpression.getLeftOperand();			
		Expression rightEP = infixExpression.getRightOperand();
		RealInterval intvalOne = this.getIntervalForExpression(leftEP);
		RealInterval intvalTwo = this.getIntervalForExpression(rightEP);

		Operator thisop = infixExpression.getOperator();
		if(intvalTwo == null){
			return;
		}
		if(intvalOne == null && !thisop.equals(InfixExpression.Operator.DIVIDE)){
			return;
		}
//		
//		if(thisop.equals(InfixExpression.Operator.LESS)
//		|| thisop.equals(InfixExpression.Operator.LESS_EQUALS)){
//			if(intvalOne.hi > intvalTwo.hi){
//				addNewErrorMessage(infixExpression , ErrorUtil.intvalInconsistency(intvalOne, intvalTwo), DiagnosticMessage.WARNING);
//			}
//		}
//		if(thisop.equals(InfixExpression.Operator.GREATER)
//		|| thisop.equals(InfixExpression.Operator.GREATER_EQUALS)){
//			if(intvalOne.hi < intvalTwo.hi){
//				addNewErrorMessage(infixExpression , ErrorUtil.intvalInconsistency(intvalOne, intvalTwo), DiagnosticMessage.WARNING);
//			}
//		}
//		
//		if(thisop.equals(InfixExpression.Operator.EQUALS)
//		||thisop.equals(InfixExpression.Operator.NOT_EQUALS)){
//			if(intvalOne.hi < intvalTwo.lo || intvalOne.lo > intvalTwo.hi){
//				addNewErrorMessage(infixExpression , ErrorUtil.intvalInconsistency(intvalOne, intvalTwo), DiagnosticMessage.WARNING);
//			}
//		}		
		infixOperations(intvalOne, intvalTwo, infixExpression, thisop, 0);
	}
	
	private void infixOperations(RealInterval intvalOne, RealInterval intvalTwo, InfixExpression infixExpression, Operator op, int extendedIndex){
		
		if(intvalOne == null && intvalTwo!=null && op.equals(InfixExpression.Operator.DIVIDE)){
			if(intvalTwo.hi>0 && intvalTwo.lo<0){
				addNewErrorMessage(infixExpression, ErrorUtil.divisionOfZero(intvalOne, intvalTwo), DiagnosticMessage.WARNING);
			}
		}
		if(intvalTwo == null || intvalOne == null){
			return;
		}
		
		RealInterval resultIntval = null;
		if(op.equals(InfixExpression.Operator.REMAINDER)){
			if(intvalOne.hi<=intvalTwo.lo){
				resultIntval = intvalOne;
			}else if(intvalOne.lo>=intvalTwo.hi){
				resultIntval = intvalTwo;
			}else{
				resultIntval = IAMath.intersect(intvalOne, intvalTwo);	
			}
		}else if(op.equals(InfixExpression.Operator.PLUS)){
			resultIntval = IAMath.add(intvalOne, intvalTwo);
		}else if(op.equals(InfixExpression.Operator.MINUS)){
			resultIntval = IAMath.sub(intvalOne, intvalTwo);
		}else if(op.equals(InfixExpression.Operator.TIMES)){
			resultIntval = IAMath.mul(intvalOne, intvalTwo);
		}else if(op.equals(InfixExpression.Operator.DIVIDE)){
			try{
				resultIntval = IAMath.div(intvalOne, intvalTwo);
			}catch (IAException e){
				addNewErrorMessage(infixExpression, ErrorUtil.divisionOfZero(intvalOne, intvalTwo), DiagnosticMessage.WARNING);
			}
		}
		this.bindIntervalWithExp(infixExpression, resultIntval);
		if(infixExpression.hasExtendedOperands()){
			if(infixExpression.extendedOperands().size()>extendedIndex){
				Expression extendedOperand = (Expression)(infixExpression.extendedOperands().get(extendedIndex));
				RealInterval intvalNewOperand = this.getIntervalForExpression(extendedOperand);
				extendedIndex++;
				infixOperations(resultIntval, intvalNewOperand, infixExpression, op, extendedIndex);
			}
		}
	}
		
	public RealInterval getIntervalForExpression(Expression exp){
		if(expIntvalMap.get(exp) != null){
			return expIntvalMap.get(exp);
		}
		return null;
	}

	public ArrayList<DiagnosticMessage> getAnalysisReports() {
		return errorReports;
	}
	
	@Override
	public void postVisit(ASTNode node) {
		EndVisitNode(node);
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
	}
	
	/**
	private void bindIntervalWithExp(Expression exp, String annotatedType){
		if(annotatedType != null){
			CMType cmtype = CMModelUtil.getCMTypeFromTypeName(currentProject, annotatedType);
			if(cmtype!=null && cmtype.getInterval()!=null){
				this.expIntvalMap.put(exp, cmtype.getInterval());
			}
		}
	}
	**/
	private void bindIntervalWithExp(Expression exp, RealInterval intval){
		if(intval != null){
			if(String.valueOf(intval.lo).equals("NaN") || String.valueOf(intval.hi).equals("NaN")){
				addNewErrorMessage(exp, ErrorUtil.NaNWarning(intval, exp), DiagnosticMessage.WARNING);
			}else if(intval.lo == Double.NEGATIVE_INFINITY || intval.lo == Double.POSITIVE_INFINITY || intval.hi == Double.NEGATIVE_INFINITY || intval.hi == Double.POSITIVE_INFINITY){
				addNewErrorMessage(exp, ErrorUtil.InfinityWarning(intval, exp), DiagnosticMessage.WARNING);
			}else{
				this.expIntvalMap.put(exp, intval);
			}
		}
	}
	
	private boolean hasRWT(IVariableBinding variableBinding ){
		String thisCMType = null;
		if(variableBinding.isField()){
			String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
			if(variableBinding.getJavaElement()==null){
				return false;
			}
			String classDeclPath = variableBinding.getJavaElement().getPath().toString();
			String classDeclKey = variableBinding.getDeclaringClass().getKey();
			if(currentUnitPath.equals(classDeclPath)){
				Map<String, String> varTypeMap = this.allVariableMap.get(classDeclKey);
				if(varTypeMap!=null){
					thisCMType = varTypeMap.get(variableBinding.getName());
				}
			}else{
				IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(variableBinding.getJavaElement().getPath());
				File otherSourceFileAnnotationFile = CMModelUtil.getAnnotationFile(ifile);
				if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
					FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
					if(otherSourcefileAnnotation == null){
						return false;
					}
					thisCMType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
				}
			}
			if(thisCMType != null){
				CMType cmtype = CMModelUtil.getCMTypeFromTypeName(currentProject, thisCMType);
				if(cmtype!=null){
					return true;
				}
			}
		}else{
			String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
			Map<String, String> varTypeMap = this.allVariableMap.get(methodDeclKey);
			if(varTypeMap!=null){
				return varTypeMap.containsKey(variableBinding.getName());
			}
		}
		return false;
	}
	
}