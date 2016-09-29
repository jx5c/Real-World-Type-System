package rwtchecker.typechecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import rwtchecker.CM.CMType;
import rwtchecker.CMRules.CMTypeRuleCategory;
import rwtchecker.CMRules.CMTypeRulesManager;
import rwtchecker.annotation.FileAnnotations;
import rwtchecker.util.CMModelUtil;

/**
 * Current version selects elements: 
 * 	local variables
 * 	function names (all functions being called)
 * 	data structure (datatypes? class?)
 * @author kahn13
 *
 */
public class ExtractPatternVisitor extends ASTVisitor {
	
	private CMTypeRulesManager cmTypeOperationManager;
	
	private HashMap<Expression, String> annotatedTypeTableForExpression = new HashMap<Expression, String>();
	
	private CompilationUnit compilationUnit;
	
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	private IPath currentFilePath;
	private IFile currentFile;
	private IProject currentProject;
	
	private ArrayList<String> hashedValueOfFunction = new ArrayList<String>();
	
	public ExtractPatternVisitor(CMTypeRulesManager manager, CompilationUnit compilationUnit) {
		super(true);
		this.cmTypeOperationManager = manager;
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
		}
	}
	
	public void preVisit(ASTNode node) {
			
		if(node instanceof MethodDeclaration){
			MethodDeclaration methodDelNode = (MethodDeclaration)node;
			String methodKey = methodDelNode.resolveBinding().getKey();
			//reset the hashed values of each function
			hashedValueOfFunction.clear();
			return;
		}
		
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
						File otherSourceFileAnnotationFile = CMModelUtil.getAnnotationFile(ifile);
						if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
							FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
							if(otherSourcefileAnnotation == null){
								return;
							}
							thisCMType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
						}
					}						
				}
				addTypeInfo((SimpleName)node,thisCMType);
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
					addTypeInfo((MethodInvocation)(node.getParent()),returnType);
		 		}

				else if (node.getParent() instanceof MethodDeclaration){
					String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
					if(methodReturnMap!=null){
						returnType = methodReturnMap.get(methodDeclKey);
					}
		 		}
				
			}
			
		}
		
		if(node instanceof ThisExpression){
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
					addTypeInfo((ThisExpression)node,  thisCMType);
				}
			}
	}
	
	private void addTypeInfo(Expression exp, String thisCMType){
		if(thisCMType != null){
			this.associateAnnotatedTypeWithExpression(exp, thisCMType);
			addItem(thisCMType);
		}
	}
	
	//itemize selected program elements
	private void addItem(String item){
		//add all CM types   
		if(!item.equals(CMType.TypeLess)){
			/*
			if(!hashedValueOfFunction.contains(String.valueOf(item.hashCode()))){
				hashedValueOfFunction.add(String.valueOf(item.hashCode()));
			}
			*/
			if(!hashedValueOfFunction.contains(item)){
				hashedValueOfFunction.add(item);
			}
			
		}
	}
    @Override
	public void postVisit(ASTNode node) {
    	this.EndVisitNode(node);
    }
    
	
	public void EndVisitNode(ASTNode node){
		if(node instanceof MethodDeclaration){
			MethodDeclaration methodDecl = (MethodDeclaration)node;
			String methodKey = methodDecl.resolveBinding().getKey();
			
			String[] items =(String[])hashedValueOfFunction.toArray(new String[hashedValueOfFunction.size()]);
			String itemString = "";
			for (String item : items){
				if(itemString.length() == 0){
					itemString = item;
				}else{
					itemString = itemString + "," + item;
				}
			}
			if(itemString.length() >0){
				FileWriter bw;
				try {
					bw = new FileWriter("e:\\testData.txt", true);
					bw.append(itemString+System.getProperty( "line.separator" ));
					System.out.println(itemString);
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
		}
		
		else if(node instanceof QualifiedName){
			QualifiedName qualifiedName = (QualifiedName)node;
			String nameType =  this.getAnnotatedTypeForExpression(qualifiedName.getName());
			String qualifierType = this.getAnnotatedTypeForExpression(qualifiedName.getQualifier());
			String resultType = nameType;
			if(nameType.equals(CMType.error_propogate) || nameType.equals(CMType.error_source)){
				this.associateAnnotatedTypeWithExpression(qualifiedName, CMType.error_propogate);
			}else{
				resultType = getUnionType(qualifierType,nameType);
			}
			this.associateAnnotatedTypeWithExpression(qualifiedName, resultType);
		}
		
		else if(node instanceof FieldAccess){
			FieldAccess fieldAccessNode = (FieldAccess)node;
			Expression fieldInstance = fieldAccessNode.getExpression();
			String annotatedInstanceType = this.getAnnotatedTypeForExpression(fieldInstance);
			String annotatedIdentifierType = this.getAnnotatedTypeForExpression(fieldAccessNode.getName());
			String resultType = annotatedIdentifierType;
			if(resultType.equals(CMType.error_propogate) || resultType.equals(CMType.error_source)){
				this.associateAnnotatedTypeWithExpression(fieldAccessNode, CMType.error_propogate);
			}else{
				resultType = getUnionType(annotatedInstanceType,annotatedIdentifierType);
			}
			this.associateAnnotatedTypeWithExpression(fieldAccessNode, resultType);
		}
		
		else if(node instanceof ArrayAccess){
			ArrayAccess arrayAccess = (ArrayAccess)node;
			String annotatedType =  this.getAnnotatedTypeForExpression(arrayAccess.getArray());
					String resultType = annotatedType;
					if(resultType.equals(CMType.error_propogate) || resultType.equals(CMType.error_source)){
						this.associateAnnotatedTypeWithExpression(arrayAccess, CMType.error_propogate);
					}
					this.associateAnnotatedTypeWithExpression(arrayAccess, resultType);
		}
		
		else if(node instanceof InfixExpression){
			InfixExpression infixExpressionNode = (InfixExpression)node;
			doInfixExpression(infixExpressionNode);
		}
		
		else if(node instanceof MethodInvocation){
			MethodInvocation methodInvocationNode = (MethodInvocation)node;
			for (int i=0;i<methodInvocationNode.arguments().size();i++){
				Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
				String argumentCMType = this.getAnnotatedTypeForExpression(exp);
				if(argumentCMType.equals(CMType.error_propogate) || argumentCMType.equals(CMType.error_source)){
					this.associateAnnotatedTypeWithExpression(methodInvocationNode, CMType.error_propogate);
					return;
				}
			}
			
			//itemize function calls
			//no itemize function calls 
			//addItem(methodInvocationNode.getName().getIdentifier());
			
			
			this.checkMathMethodInvocation(methodInvocationNode);
//			this.checkCollectionAccess(methodInvocationNode);
			this.checkMethodInvocation(methodInvocationNode);
		}
		else if(node instanceof ParenthesizedExpression){
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression)node;
					String argumentCMType = this.getAnnotatedTypeForExpression(parenthesizedExpression.getExpression());
					if(argumentCMType.equals(CMType.error_propogate) || argumentCMType.equals(CMType.error_source)){
						this.associateAnnotatedTypeWithExpression(parenthesizedExpression, CMType.error_propogate);
						return;
					}else{
						this.associateAnnotatedTypeWithExpression(parenthesizedExpression, argumentCMType);
					}
		}
		
		else if(node instanceof ReturnStatement){
			ReturnStatement returnStatementNode = (ReturnStatement)node;
			ASTNode parentNode = node.getParent();
            while(!(parentNode instanceof MethodDeclaration) ){
            	parentNode = parentNode.getParent();
            }
            
            if(parentNode instanceof MethodDeclaration){
            	 String parentMethodDeclarationKey = ((MethodDeclaration)parentNode).resolveBinding().getKey();
     			//for recursive function call
     			if(node instanceof MethodInvocation){
     				if(parentMethodDeclarationKey.equals(((MethodInvocation)node).resolveMethodBinding().getKey())){
     					return;
     				}
     			}
				String annotatedReturnType = null;
				if(methodReturnMap!=null){
					annotatedReturnType = methodReturnMap.get(parentMethodDeclarationKey);
				}
				if(annotatedReturnType!= null){
					String actualReturnType = this.getAnnotatedTypeForExpression(returnStatementNode.getExpression()); 
				}
            }
		}
		//error checking here
		else if(node instanceof Assignment){
			Assignment assignmentNode = (Assignment)node;
			Expression leftExp = assignmentNode.getLeftHandSide();
			Expression rightExp = assignmentNode.getRightHandSide();
			if((leftExp instanceof NumberLiteral) || (rightExp instanceof NumberLiteral)){
				return;
			}
			String leftCMType = this.getAnnotatedTypeForExpression(leftExp);
			String rightCMType = this.getAnnotatedTypeForExpression(rightExp);
			//error propagate
			if((leftCMType.equals(CMType.error_source)) || (leftCMType.equals(CMType.error_propogate))
					||	(rightCMType.equals(CMType.error_source)) || (rightCMType.equals(CMType.error_propogate))){
				return;
			}
			else if(leftCMType.equals(CMType.TypeLess) && !rightCMType.equals(CMType.TypeLess)){
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
						addTypeInfo(leftExp, leftCMType);
					}
				}
			}
		}
		
		else if(node instanceof VariableDeclarationStatement){
			VariableDeclarationStatement variableDeclarationStatementNode = (VariableDeclarationStatement)node;
			ASTNode parentNode = node.getParent();
            while(!(parentNode instanceof BodyDeclaration)){
            	parentNode = parentNode.getParent();
            }
			for (Iterator iter = variableDeclarationStatementNode.fragments().iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
				if(fragment.getInitializer()==null){
					continue;
				}
				if(fragment.getInitializer() instanceof NumberLiteral){
					continue;
				}
				
				
				String leftCMType = this.getAnnotatedTypeForExpression(fragment.getName());
				String rightCMType = this.getAnnotatedTypeForExpression(fragment.getInitializer());
				//error propagate
				if(rightCMType.equals(CMType.error_propogate) || rightCMType.equals(CMType.error_source)){
					return;
				}
				if(leftCMType.equals(CMType.error_propogate) || leftCMType.equals(CMType.error_source)){
					return;
				}
				if(leftCMType.equals(CMType.TypeLess) && !rightCMType.equals(CMType.TypeLess)){
					if(fragment.getName() instanceof SimpleName){
						IBinding fbinding = ((SimpleName)fragment.getName()).resolveBinding();
						if(fbinding instanceof IVariableBinding){
							IVariableBinding variableBinding = (IVariableBinding) fbinding;
							String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
							Map<String, String> variableMap = this.allVariableMap.get(methodDeclKey);
							if(variableMap == null){
								variableMap = new HashMap<String, String>();
							}
							leftCMType =  rightCMType;
							variableMap.put(variableBinding.getName(), leftCMType);
							this.allVariableMap.put(methodDeclKey, variableMap);
							addTypeInfo(fragment.getName(), leftCMType);
						}
					}
				}
			}
		}
		
		else if(node instanceof PrefixExpression){
			PrefixExpression prefixExpression = (PrefixExpression)node;
			this.associateAnnotatedTypeWithExpression(prefixExpression, this.getAnnotatedTypeForExpression(prefixExpression.getOperand()));
		}
		
	}
	
	private void checkCollectionAccess(MethodInvocation methodInvocationNode) {
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		//check for list access
		if(iMethodBinding.getMethodDeclaration().getName().equals("get")) {
			if(iMethodBinding.getDeclaringClass().getBinaryName().equals("java.util.ArrayList") ||
   			   iMethodBinding.getDeclaringClass().getBinaryName().equals("java.util.List")){
				addTypeInfo(methodInvocationNode, this.getAnnotatedTypeForExpression(methodInvocationNode.getExpression()));
			}
		}
	}

	private void checkMethodInvocation(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
		String methodDeclPath = iMethodBinding.getJavaElement().getPath().toString();
		String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
		
		IFile methodDeclFile = null;
		CompilationUnit targetedCompilationUnit = null;
		if(currentUnitPath.equals(methodDeclPath)){
			targetedCompilationUnit = this.compilationUnit;
			methodDeclFile = this.currentFile;
		}
		else {
			methodDeclFile = ResourcesPlugin.getWorkspace().getRoot().getFile(iMethodBinding.getJavaElement().getPath());
			if((methodDeclFile != null) && (methodDeclFile.getFileExtension().toLowerCase().endsWith("java"))){
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(methodDeclFile);
				parser.setSource(icompilationUnit); // set source
				parser.setResolveBindings(true);
				targetedCompilationUnit = (CompilationUnit) parser.createAST(null);
			}
		}
		if(targetedCompilationUnit==null){
			return;
		}
		if(methodDeclFile==null || !methodDeclFile.exists()){
			return;
		}
		//with annotated cm type
		File annotationFile = CMModelUtil.getAnnotationFile(methodDeclFile);
		if(annotationFile != null){
			FileAnnotations fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			if(fileAnnotations!=null && fileAnnotations.getReturnCMTypeForMethod(methodDeclKey)!=null){
				String returnAnnotatedCMType = fileAnnotations.getReturnCMTypeForMethod(methodDeclKey);
				addTypeInfo(methodInvocationNode, returnAnnotatedCMType );
				return;
			}else{
				boolean hasTypedArguments = false;
				for (int i=0;i<methodInvocationNode.arguments().size();i++){
					Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
					String argumentCMType = this.getAnnotatedTypeForExpression(exp);
					if(!argumentCMType.equals(CMType.TypeLess)){
						hasTypedArguments = true;
						break;
					}
				}
				if(!hasTypedArguments){
					//no annotation, no argument types; do nothing
					return;
				}
			}
		}
		
		//without annotated cm type, polymorphic
		/*
		TypeCheckingVisitor methodDeclVisitor = new TypeCheckingVisitor(this.cmTypeOperationManager, targetedCompilationUnit, 3);
		
		Map<String, String> variableMap = methodDeclVisitor.getAllVariableMap().get(methodDeclKey);
		ASTNode declaringNode = methodDeclVisitor.getCompilationUnit().findDeclaringNode(iMethodBinding.getKey());
		MethodDeclaration methodDecl = (MethodDeclaration)declaringNode;
		for (int i=0;i<methodInvocationNode.arguments().size();i++){
			Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
			
			String argumentCMType = this.getAnnotatedTypeForExpression(exp);
			SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)(methodDecl.parameters().get(i));
			
			if(argumentCMType.length()>0){
				if(variableMap == null){
					variableMap = new HashMap<String, String>();
				}
			}
			if(variableMap != null){
				if(variableMap.get(singleVariableDeclaration.getName().getIdentifier())!=null){
					String annotatedCMType = variableMap.get(singleVariableDeclaration.getName().getIdentifier());
					if(annotatedCMType.length()>0){
					}
				}
			}
			if(argumentCMType.length()>0){
				variableMap.put(singleVariableDeclaration.getName().getIdentifier(), argumentCMType);
			}
		}
		addTypeInfo(methodInvocationNode, methodDeclVisitor.getMethodReturnMap().get(methodDeclKey));
		*/
	}
		
	//hard coding the math default invocation
	private void checkMathMethodInvocation(MethodInvocation methodInvocationNode){
		IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
		String binaryClassName = iMethodBinding.getDeclaringClass().getBinaryName();
		if(binaryClassName.equals("java.lang.Math")){
			String methodName = iMethodBinding.getName();
			if(methodInvocationNode.arguments().size() == 2){
				Expression argumentOne = (Expression)(methodInvocationNode.arguments().get(0));	
				Expression argumentTwo = (Expression)(methodInvocationNode.arguments().get(1));	
				
				String argumentOneAnnotatedType = this.getAnnotatedTypeForExpression(argumentOne);
				String argumentTwoAnnotatedType = this.getAnnotatedTypeForExpression(argumentTwo);

				if(argumentTwoAnnotatedType.equalsIgnoreCase(CMType.TypeLess) && argumentOneAnnotatedType.equalsIgnoreCase(CMType.TypeLess)){
						this.associateAnnotatedTypeWithExpression(methodInvocationNode, CMType.TypeLess);
						return;
				}
				String returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, argumentOneAnnotatedType, methodName, argumentTwoAnnotatedType); 
				if(returnType != null){
					this.associateAnnotatedTypeWithExpression(methodInvocationNode,  returnType);
					return;
				}else {
					this.associateAnnotatedTypeWithExpression(methodInvocationNode, CMType.error_source);
					return;
				}
			}
			if(methodInvocationNode.arguments().size() == 1){
				Expression argument = (Expression)(methodInvocationNode.arguments().get(0));
				String argumentAnnotatedType = this.getAnnotatedTypeForExpression(argument);

				if(argumentAnnotatedType.equalsIgnoreCase(CMType.TypeLess)){
					this.associateAnnotatedTypeWithExpression(methodInvocationNode, CMType.TypeLess);
					return;
				}
				String returnType = null;
				returnType=this.cmTypeOperationManager.getReturnType(this.currentProject, argumentAnnotatedType, methodName, CMType.TypeLess); 
				if((returnType != null)){
					this.associateAnnotatedTypeWithExpression(methodInvocationNode, returnType);
				}else{
					this.associateAnnotatedTypeWithExpression(methodInvocationNode,  CMType.error_source);	
				}

				//abs function
				if(methodName.equalsIgnoreCase(CMTypeRuleCategory.Abosolute_Value)){
					addTypeInfo(methodInvocationNode,argumentAnnotatedType);
					return;
				}
				
			}
		}
	}
	
	private void doInfixExpression(InfixExpression infixExpression){
		Expression leftEP = infixExpression.getLeftOperand();			
		Expression rightEP = infixExpression.getRightOperand();
		String CMTypeAnnotatedTypeOne = this.getAnnotatedTypeForExpression(leftEP);
		String CMTypeAnnotatedTypeTwo = this.getAnnotatedTypeForExpression(rightEP);
		if((CMTypeAnnotatedTypeOne.equals(CMType.error_source)) || (CMTypeAnnotatedTypeOne.equals(CMType.error_propogate))
				||	(CMTypeAnnotatedTypeTwo.equals(CMType.error_source)) || (CMTypeAnnotatedTypeTwo.equals(CMType.error_propogate))){
			this.associateAnnotatedTypeWithExpression(infixExpression, CMType.error_propogate);
			return;
		}
		if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
			this.associateAnnotatedTypeWithExpression(infixExpression, CMType.TypeLess);
		}
	}
	
	private void check_Remander_Operation(String CMTypeAnnotatedTypeOne, String CMTypeAnnotatedTypeTwo, InfixExpression infixExpression, int extendedIndex){
		String infixExpressionType = CMType.TypeLess;
				//type rules part
				if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMType.TypeLess;
				}		
				else if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && !CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMType.TypeLess;
				}
				else if(!CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMTypeAnnotatedTypeOne;
				}else{
					String returnType = null;
					returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, CMTypeRuleCategory.REMAINDER, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
						infixExpressionType = CMType.TypeLess;
					}	
				}
				this.associateAnnotatedTypeWithExpression(infixExpression, infixExpressionType);

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
		String infixExpressionType = CMType.TypeLess;
				//type rules part
				if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMType.TypeLess;
				}		
				else if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && !CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMTypeAnnotatedTypeTwo;
				}
				else if(!CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMTypeAnnotatedTypeOne;
				}else{
					String returnType = null;
					returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, operation_type, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
						if(!CMTypeAnnotatedTypeOne.equalsIgnoreCase(CMType.TypeLess) && !CMTypeAnnotatedTypeTwo.equalsIgnoreCase(CMType.TypeLess)){							
							infixExpressionType = CMType.TypeLess;
						}
					}	
				}
				this.associateAnnotatedTypeWithExpression(plusInfixExpression, infixExpressionType);
		
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
		String infixExpressionType = CMType.TypeLess;
				//type rules part
				if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMType.TypeLess;
				}		
				else if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && !CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMTypeAnnotatedTypeTwo;
				}
				else if(!CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
					infixExpressionType = CMTypeAnnotatedTypeOne;
				}else{
					String returnType = null;
					returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, CMTypeRuleCategory.Multiplication, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
						if(!CMTypeAnnotatedTypeOne.equalsIgnoreCase(CMType.TypeLess) && !CMTypeAnnotatedTypeTwo.equalsIgnoreCase(CMType.TypeLess)){							
							infixExpressionType = CMType.error_source;
						}
					}
				}
				this.associateAnnotatedTypeWithExpression(infixExpression, infixExpressionType);
		if(infixExpression.hasExtendedOperands()){
			if(infixExpression.extendedOperands().size()>extendedIndex){
				Expression extendedOperand = (Expression)(infixExpression.extendedOperands().get(extendedIndex));
				String CMTypeForNewOperand = this.getAnnotatedTypeForExpression(extendedOperand);
				extendedIndex++;
				check_Times_Operation(infixExpressionType, CMTypeForNewOperand, infixExpression, extendedIndex);
			}
		}
	}
	
	private void check_Division_Operation(String CMTypeAnnotatedTypeOne, String CMTypeAnnotatedTypeTwo, InfixExpression infixExpression, int extendedIndex){	
		String infixExpressionType = CMType.TypeLess;
			//type rules part
			if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
				infixExpressionType = CMType.TypeLess;
			}		
			else if(CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && !CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
				String inverseType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeTwo, CMTypeRuleCategory.Multiplicative_Inverse, "");
				if(inverseType != null ){
					infixExpressionType = inverseType;
				}else{
					infixExpressionType = CMType.error_source;
				}
			}
			else if(!CMTypeAnnotatedTypeOne.equals(CMType.TypeLess) && CMTypeAnnotatedTypeTwo.equals(CMType.TypeLess)){
				infixExpressionType = CMTypeAnnotatedTypeOne;
			}else{
				String returnType = null;
				returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, CMTypeRuleCategory.Division, CMTypeAnnotatedTypeTwo);
				if(returnType != null ){
					infixExpressionType = returnType;
				}else{
					infixExpressionType = CMType.error_source;
				}
			}
			this.associateAnnotatedTypeWithExpression(infixExpression, infixExpressionType);
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
			annotatedTypeTableForExpression.put(exp, CMType.TypeLess);
			return CMType.TypeLess;
		}
	}
	
	private void associateAnnotatedTypeWithExpression(Expression exp, String annotatedType){
		this.annotatedTypeTableForExpression.put(exp, annotatedType);
	}
	


	private String getUnionType(String contextTypeName, String typeName){
		if(contextTypeName.equals(CMType.TypeLess)){
			return typeName;
		}else{
			String combinedType = this.cmTypeOperationManager.getReturnType(currentProject, contextTypeName, CMTypeRuleCategory.TypeContext, typeName);
			if(combinedType!=null){
				return combinedType;
			}else{
				return typeName;
			}
		}
	}
}