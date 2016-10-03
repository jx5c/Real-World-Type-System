package rwtchecker.typechecker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import rwtchecker.rwt.RWType;
import rwtchecker.rwtrules.RWTypeRule;
import rwtchecker.rwtrules.RWTypeRuleCategory;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.util.RWTSystemUtil;

public class CandidateRuleVisitor extends ASTVisitor {
	
	private static String generatedRuleMarker = "@";
	
	private RWTypeRulesManager cmTypeOperationManager;
	private RWTypeRulesManager candidateRuleManager;
	
	private HashMap<Expression, String> candidateTypeMapForExpression = new HashMap<Expression, String>();
	
	private CompilationUnit compilationUnit;
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	
	private IPath currentFilePath;
	private IFile currentFile;
	private IProject currentProject;
	
	private Map<String, ArrayList<String>> methodReturnMap = new HashMap<String, ArrayList<String>>();
	private Map<Integer, ArrayList<String>> varsCommentsMap = new HashMap<Integer, ArrayList<String>>();
	private Map<Integer, ArrayList<String>> funcCommentsMap = new HashMap<Integer, ArrayList<String>>();
	
	private ArrayList<Integer> varsCommentPositionList = new ArrayList<Integer>();
	private ArrayList<Integer> funcCommentPositionList = new ArrayList<Integer>();
	private String currentAccessMethodKey = null;
	private String currentAccessClassKey = null;
	
	public CandidateRuleVisitor(RWTypeRulesManager manager, CompilationUnit compilationUnit, 
			Map<Integer, ArrayList<String>> inputVarsCommentsMap, Map<Integer, ArrayList<String>> inputFuncCommentsMap) {
		super(true);
		this.cmTypeOperationManager = manager;
		this.compilationUnit = compilationUnit;
		currentFilePath = this.compilationUnit.getJavaElement().getPath();
		currentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(currentFilePath);
		currentProject = currentFile.getProject();
		this.candidateRuleManager = RWTypeRulesManager.getCandidateRuleManager(currentFile.getName());
		this.candidateRuleManager.clear();
		
		this.varsCommentsMap = inputVarsCommentsMap;
		this.funcCommentsMap = inputFuncCommentsMap;
		varsCommentPositionList.addAll(inputVarsCommentsMap.keySet());
		funcCommentPositionList.addAll(inputFuncCommentsMap.keySet());
		Collections.sort(varsCommentPositionList);
		Collections.sort(funcCommentPositionList);
	}
	
	private String checkReturnCMType(String[] argument_cmtypes, ArrayList<String> possibleArgumentReturnTypes){
		if(possibleArgumentReturnTypes == null){
			return RWType.UnknownType;
		}
		if(argument_cmtypes.length == 0){
			//no argument, only return types
			for(int i=0;i<possibleArgumentReturnTypes.size();i++){
				String possibleReturnType = possibleArgumentReturnTypes.get(i).trim();
				if(possibleReturnType.startsWith(":")){
					return possibleReturnType.replace(":", "");
				}
			}
		}else{
			String thisArgumentInCmtype = argument_cmtypes[0]; 
			for (int j=1;j<argument_cmtypes.length;j++){
				thisArgumentInCmtype = thisArgumentInCmtype + "," + argument_cmtypes[j];
			}
			for(int i=0;i<possibleArgumentReturnTypes.size();i++){
				String possibleArgumentReturnType = possibleArgumentReturnTypes.get(i);
				if(possibleArgumentReturnType.startsWith(thisArgumentInCmtype+":")){
					return possibleArgumentReturnType.split(":")[1];
				}
			}
			if(possibleArgumentReturnTypes.size() == 1){
				if(possibleArgumentReturnTypes.get(0).split(":").length == 2){
					return possibleArgumentReturnTypes.get(0).split(":")[1];	
				}
			}
		}
		return RWType.UnknownType;
	}
	
	public void preVisit(ASTNode node) {
		
		if(node instanceof MethodDeclaration){
			MethodDeclaration methodDelNode = (MethodDeclaration)node;
			String methodKey = methodDelNode.resolveBinding().getKey();
			this.currentAccessMethodKey = methodKey; 
			
			if(funcCommentPositionList.size() > 0){
				//check for method annotations
				int startingLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) -1;
				int endLineNumber = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength()) - 1;
				for (int i=0;i<funcCommentPositionList.size();i++){
					int thisCommentEndingLine = funcCommentPositionList.get(i);
					if(thisCommentEndingLine > endLineNumber){
						break;
					}
					/*
					if(thisCommentLine > startingLineNumber && thisCommentLine <= endLineNumber){
						//found the correct position
						ArrayList<String> funcComments = this.funcCommentsMap.get(thisCommentLine);
						this.methodReturnMap.put(methodKey, funcComments);
						//be careful with empty parameters
						funcCommentPositionList.remove(i);
					}
					*/
					if(thisCommentEndingLine >= startingLineNumber-1 && thisCommentEndingLine< endLineNumber){
						//found the correct position
						ArrayList<String> funcComments = this.funcCommentsMap.get(thisCommentEndingLine);
						this.methodReturnMap.put(methodKey, funcComments);
						//be careful with empty parameters
						funcCommentPositionList.remove(i);
					}
				}
			}
			return;
		}
		
		if(node instanceof TypeDeclaration){
			TypeDeclaration typeDelNode = (TypeDeclaration)node;
			String classKey = typeDelNode.resolveBinding().getKey();
			this.currentAccessClassKey = classKey; 
		}
		
		if(varsCommentPositionList.size() >0){
			int currentLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) -1;
			if (currentLineNumber >= varsCommentPositionList.get(0)){
				int pastCommentLineNum = varsCommentPositionList.get(0);
				ArrayList<String> varsComments = this.varsCommentsMap.get(pastCommentLineNum);
				if(currentAccessMethodKey != null){
					Map<String, String> localVariableMap = this.allVariableMap.get(currentAccessMethodKey);
					if(localVariableMap == null){
						localVariableMap = new HashMap<String, String>();	
					}
					for (int i=0;i<varsComments.size();i++){
						String comment = varsComments.get(i);
						comment = comment.toLowerCase();
						comment = comment.replace("cmt(", "");
						String variableName =  comment.split("\\)=")[0];
						String cmtype = comment.split("\\)=")[1];
						localVariableMap.put(variableName, cmtype);	
					}
					this.allVariableMap.put(currentAccessMethodKey, localVariableMap);
				}else if(currentAccessClassKey != null){
					Map<String, String> localVariableMap = this.allVariableMap.get(currentAccessClassKey);
					if(localVariableMap == null){
						localVariableMap = new HashMap<String, String>();	
					}
					//class declaration
					for (int i=0;i<varsComments.size();i++){
						String comment = varsComments.get(i);
						comment = comment.toLowerCase();
						comment = comment.replace("cmt(", "");
						String variableName =  comment.split("\\)=")[0];
						String cmtype = comment.split("\\)=")[1];
						localVariableMap.put(variableName, cmtype);	
					}
					this.allVariableMap.put(currentAccessClassKey, localVariableMap);
				}
				varsCommentPositionList.remove(0);
			}
		}
	}
	
	@Override
	public boolean visit(QualifiedName qualifiedName){
		//debug here
		String qnameString = qualifiedName.toString();
		Map<String, String> localVariableMap = this.allVariableMap.get(currentAccessMethodKey);
		if(localVariableMap !=null){
			String cmtype = localVariableMap.get(qnameString);
			this.associateCMTypeWithExpression(qualifiedName, cmtype);
			return false;
		}
		return true;
	}
	
	@Override
	public boolean visit(FieldAccess fieldAccess){
		String fieldAccessString = fieldAccess.toString();
		Map<String, String> localVariableMap = this.allVariableMap.get(currentAccessMethodKey);
		if(localVariableMap !=null){
			String cmtype = localVariableMap.get(fieldAccessString);
			if(cmtype!=null){
				this.associateCMTypeWithExpression(fieldAccess, cmtype);
				return false;
			}			
		}
		return true;
	}
	
	@Override
	public boolean visit(final SimpleName simpleName){
		IBinding fbinding = simpleName.resolveBinding();
		if(fbinding instanceof IVariableBinding){
			final IVariableBinding variableBinding = (IVariableBinding) fbinding;
			String thisCMType = null;
			if(variableBinding.isField()){
				String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
				if(variableBinding.getJavaElement()==null){
					return true;
				}
				String classDeclPath = variableBinding.getJavaElement().getPath().toString();
				String classDeclKey = variableBinding.getDeclaringClass().getKey();
				if(currentUnitPath.equals(classDeclPath)){
					Map<String, String> fieldVariableMap = this.allVariableMap.get(classDeclKey);
					if(fieldVariableMap!=null){
						thisCMType = fieldVariableMap.get("this."+variableBinding.getName());
						this.associateCMTypeWithExpression(simpleName, thisCMType);
					}
						
				}else{
					IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(variableBinding.getJavaElement().getPath());
					if((ifile != null) && (ifile.getFileExtension().toLowerCase().endsWith("java"))){
						ASTParser parser = ASTParser.newParser(AST.JLS3);
						parser.setKind(ASTParser.K_COMPILATION_UNIT);
//						System.out.println(ifile.toString());
						ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(ifile);
						parser.setSource(icompilationUnit); // set source
						parser.setResolveBindings(true); // we need bindings later on
						final CompilationUnit otherCompilationResult = (CompilationUnit) parser.createAST(null);
						BufferedReader infile = null;
						ArrayList<String> contents = new ArrayList<String>();
						String line = "";
						try {
							infile = new BufferedReader(new FileReader(ifile.getRawLocation().toFile()));
							while((line = infile.readLine())!= null){
								contents.add(line);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						final String[] externalSourceList = contents.toArray(new String[contents.size()]);
							otherCompilationResult.accept(new ASTVisitor() {
								@Override
				        	    public boolean visit(SimpleName node) {
									IBinding fbinding = ((SimpleName)node).resolveBinding();
									if(fbinding instanceof IVariableBinding){
										IVariableBinding vBinding = (IVariableBinding) fbinding;
										if(vBinding.getKey().equals(variableBinding.getKey())){
											int startLineNumber = otherCompilationResult.getLineNumber(node.getStartPosition()) - 1;
											int possibleCommentNumber = startLineNumber-1; 
											String lastLineContents = externalSourceList[possibleCommentNumber];
											if(lastLineContents.endsWith("cm*/")){
												String targetComment = ""; 
												if(lastLineContents.startsWith("/*cm")){
													targetComment = lastLineContents.replace("/*cm "+CommentVisitor.def, "").replace(" cm*/", "");
												}else{
													int lineCourt = possibleCommentNumber;
													String blockComment = externalSourceList[lineCourt].trim();
									                while (true) {
									                	lineCourt--;
									                    String blockCommentLine = externalSourceList[lineCourt].replaceAll("\\* ", "").trim();
									                    blockComment = blockCommentLine + blockComment;
									                    if(blockCommentLine.startsWith("/*cm")){
									                    	break;
									                    }
									                }
									                targetComment = blockComment.replace("/*cm "+CommentVisitor.def, "").trim();
												}
												String[] cmtypeComments = targetComment.split(";");
												for (int i=0;i<cmtypeComments.length;i++){
													String comment = cmtypeComments[i];
													comment = comment.toLowerCase();
													comment = comment.replace("cmt(", "");
													String variableName =  comment.split(")=")[0];
													String cmtype = comment.split(")=")[1];
													if(variableName.equals("this."+variableBinding.getName())){
														associateCMTypeWithExpression(simpleName, cmtype);
													}
												}
											}
										}
									}
									return false;
								}
							});
						}
				}						
			}else{
					//local variable
					String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
					Map<String, String> variableMap = this.allVariableMap.get(methodDeclKey);
					if(variableMap!=null){
						thisCMType = variableMap.get(variableBinding.getName());
						associateCMTypeWithExpression(simpleName, thisCMType);
					}
				}
			}
		return true;
	}
	
	public void EndVisitNode(ASTNode node){
		
		if(node instanceof MethodInvocation){
			final MethodInvocation methodInvocationNode = (MethodInvocation)node;
			final IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
			String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
			String methodDeclPath = iMethodBinding.getJavaElement().getPath().toString();
			String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
			//check arguments cmtype
//			boolean hasTypedArguments = false;
			final String[] argumentCMTypes = new String[methodInvocationNode.arguments().size()];
			for (int i=0;i<methodInvocationNode.arguments().size();i++){
				Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
				String argumentCMType = this.getAnnotatedTypeForExpression(exp);
				argumentCMTypes[i] = argumentCMType;
			}
				if(currentUnitPath.equals(methodDeclPath)){
					if(methodReturnMap!=null){
						String returnType = this.checkReturnCMType(argumentCMTypes, this.methodReturnMap.get(methodDeclKey));
//						if(returnType.equals(CMType.UnknownType)){
////							# if no result being found, detect candidate cm type rules
//							String synthesizedCMType = methodDeclKey + "_"+ argumentCMTypes;
//							associateCMTypeWithExpression(methodInvocationNode, synthesizedCMType);	
//						}else{
//							associateCMTypeWithExpression(methodInvocationNode, returnType);
//						}
						associateCMTypeWithExpression(methodInvocationNode, returnType);
					}
				}else{
					//method declared in other sources
					IFile methodDeclFile = ResourcesPlugin.getWorkspace().getRoot().getFile(iMethodBinding.getJavaElement().getPath());
					if((methodDeclFile != null) && (methodDeclFile.getFileExtension().toLowerCase().endsWith("java"))){
						ASTParser parser = ASTParser.newParser(AST.JLS3);
						parser.setKind(ASTParser.K_COMPILATION_UNIT);
						ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(methodDeclFile);
						parser.setSource(icompilationUnit); // set source
						parser.setResolveBindings(true); // we need bindings later on
						final CompilationUnit otherCompilationResult = (CompilationUnit) parser.createAST(null);
						BufferedReader infile = null;
						ArrayList<String> contents = new ArrayList<String>();
						String line = "";
						try {
							infile = new BufferedReader(new FileReader(methodDeclFile.getRawLocation().toFile()));
							while((line = infile.readLine())!= null){
								contents.add(line);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						final String[] externalSourceList = contents.toArray(new String[contents.size()]);
							otherCompilationResult.accept(new ASTVisitor() {
								@Override
				        	    public boolean visit(MethodDeclaration methodDeclaration) {
									IMethodBinding methodDeclBinding = methodDeclaration.resolveBinding();
										if(iMethodBinding.getKey().equals(methodDeclBinding.getKey())){
											int startLineNumber = otherCompilationResult.getLineNumber(methodDeclaration.getStartPosition()) - 1;
											int methodNameStartNumber = otherCompilationResult.getLineNumber(methodDeclaration.getName().getStartPosition()) - 1;
											int possibleCommentNumber = startLineNumber-1;
											String targetComment = "";
											int commentLineCourt = 0;
											for(int i=possibleCommentNumber;i<methodNameStartNumber;i++){
												String lineContents = externalSourceList[i].trim();
												if(lineContents.startsWith("/*cm")){
													commentLineCourt = i;
													break;
												}
											}
											if(commentLineCourt > 0){
												String firstLineContents = externalSourceList[commentLineCourt];
												if(firstLineContents.endsWith("cm*/")){
													targetComment = firstLineContents.replace("/*cm "+CommentVisitor.func, "").replace(" cm*/", "");
												}else{
													int lineCourt = commentLineCourt;
													String blockComment = externalSourceList[lineCourt].trim();
									                while (true) {
									                	lineCourt--;
									                    String blockCommentLine = externalSourceList[lineCourt].replaceAll("\\* ", "").trim();
									                    blockComment = blockComment+blockCommentLine ;
									                    if(blockCommentLine.startsWith("/*cm")){
									                    	break;
									                    }
									                }
									                targetComment = blockComment.replace("/*cm "+CommentVisitor.func, "").replace(" cm*/", "");
												}
												String[] cmtypeComments = targetComment.split(";");
												ArrayList<String> possibleArgumentReturnTypes = new ArrayList<String>();
												for (int i=0;i<cmtypeComments.length;i++){
													possibleArgumentReturnTypes.add(cmtypeComments[i]);
												}
												String returnType = checkReturnCMType(argumentCMTypes, possibleArgumentReturnTypes);
												associateCMTypeWithExpression(methodInvocationNode, returnType);
//												if(returnType.equals(CMType.UnknownType)){
//													//# if no result being found, generate candidate cm type rules
//													String synthesizedCMType = iMethodBinding.getKey() +"_"+ argumentCMTypes;
//													associateCMTypeWithExpression(methodInvocationNode, synthesizedCMType);	
//												}else{
//													associateCMTypeWithExpression(methodInvocationNode, returnType);
//												}
											}else{
//												String synthesizedCMType = iMethodBinding.getKey() +"_"+ argumentCMTypes;
												associateCMTypeWithExpression(methodInvocationNode, RWType.UnknownType);	
											}
										}
									return false;
								}
							});
						}
				}
			this.checkMathMethodInvocation(methodInvocationNode);
//			this.checkCollectionAccess(methodInvocationNode);
		}
		
		if(node instanceof QualifiedName){
			QualifiedName qualifiedName = (QualifiedName)node;
			String qualifiedNameType =  this.getAnnotatedTypeForExpression(qualifiedName);
			String nameType =  this.getAnnotatedTypeForExpression(qualifiedName.getName());
			if(qualifiedNameType!=null){
				return;
			}else{
				this.associateCMTypeWithExpression(qualifiedName, nameType);
			}
		}
		
		else if(node instanceof FieldAccess){
			FieldAccess fieldAccessNode = (FieldAccess)node;
			String annotatedFieldAccessType = this.getAnnotatedTypeForExpression(fieldAccessNode);
			String annotatedIdentifierType = this.getAnnotatedTypeForExpression(fieldAccessNode.getName());
			//using union operation for the two types
			String fieldType = this.getAnnotatedTypeForExpression(fieldAccessNode.getExpression());  
			/*
			 * using cm type rules to get union type
			 */
			if(annotatedFieldAccessType!=null){
				return;
			}else{
				this.associateCMTypeWithExpression(fieldAccessNode, annotatedIdentifierType);
			}
		}
		
		else if(node instanceof ArrayAccess){
			ArrayAccess arrayAccess = (ArrayAccess)node;
			String annotatedArraytType =  this.getAnnotatedTypeForExpression(arrayAccess.getArray());
			this.associateCMTypeWithExpression(arrayAccess, annotatedArraytType);
		}
		
		else if(node instanceof InfixExpression){
			InfixExpression infixExpression = (InfixExpression)node;
			Expression leftEP = infixExpression.getLeftOperand();			
			Expression rightEP = infixExpression.getRightOperand();
			String CMTypeAnnotatedTypeOne = this.getAnnotatedTypeForExpression(leftEP);
			String CMTypeAnnotatedTypeTwo = this.getAnnotatedTypeForExpression(rightEP);
				if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
					this.associateCMTypeWithExpression(infixExpression, RWType.UnknownType);
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
							//candidate rule of comparison 
						String operation_type = RWTypeRuleCategory.Comparable; 
						String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker +operation_type+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
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
		
		else if(node instanceof ParenthesizedExpression){
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression)node;
			String argumentCMType = this.getAnnotatedTypeForExpression(parenthesizedExpression.getExpression());
			this.associateCMTypeWithExpression(parenthesizedExpression, argumentCMType);
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
			if(leftCMType.equals(rightCMType)){
				return;
			}
			else if(!leftCMType.equalsIgnoreCase(RWType.UnknownType) &&
					!leftCMType.equalsIgnoreCase(rightCMType) && 
					!rightCMType.equalsIgnoreCase(RWType.UnknownType) ){
//				String synthetizedResultType = leftCMType + "_"+CMTypeRuleCategory.Assignable+"_" + rightCMType;
				RWTypeRule newUVRule = new RWTypeRule(RWTypeRuleCategory.Assignable, leftCMType,  rightCMType, "", RWTypeRule.notVerified);
				this.candidateRuleManager.addCMTypeOperation(newUVRule);
			}
			//now, did not include inference here
			/*
			else if(leftCMType.equals(CMType.UnknownType) && !rightCMType.equals(CMType.UnknownType)){

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
			}*/
		}
		
		else if(node instanceof VariableDeclarationStatement){
			VariableDeclarationStatement variableDeclarationStatementNode = (VariableDeclarationStatement)node;
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
				if(leftCMType.equals(rightCMType)){
					return;
				}
				else if(!leftCMType.equalsIgnoreCase(RWType.UnknownType) &&
						!leftCMType.equalsIgnoreCase(rightCMType) && 
						!rightCMType.equalsIgnoreCase(RWType.UnknownType) ){
//					String synthetizedResultType = leftCMType + "_"+CMTypeRuleCategory.Assignable+"_" + rightCMType;
					RWTypeRule newUVRule = new RWTypeRule(RWTypeRuleCategory.Assignable, leftCMType,  rightCMType, "", RWTypeRule.notVerified);
					this.candidateRuleManager.addCMTypeOperation(newUVRule);	
				}
				/* no inference right now
				if(leftCMType.equals(CMType.UnknownType) && !rightCMType.equals(CMType.UnknownType)){
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
				*/
			}
		}
		
		else if(node instanceof PrefixExpression){
			PrefixExpression prefixExpression = (PrefixExpression)node;
			this.associateCMTypeWithExpression(prefixExpression, this.getAnnotatedTypeForExpression(prefixExpression.getOperand()));
		}
		
		else if(node instanceof ConditionalExpression){
			ConditionalExpression conditionalExpression = (ConditionalExpression)node;
			Expression thenExp = conditionalExpression.getThenExpression();
			Expression elseExp = conditionalExpression.getElseExpression();
			String thenAnnotatedType = this.getAnnotatedTypeForExpression(thenExp) ;
			String elseAnnotatedType = this.getAnnotatedTypeForExpression(elseExp) ;
//			addNewErrorMessage(conditionalExpression , ErrorUtil.getUnitInconsistencyInConditionExpression(thenAnnotatedType, elseAnnotatedType), DiagnosticMessage.ERROR);
		}
	}
	/*
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
	 */
	
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
					if(argumentTwoAnnotatedType.equalsIgnoreCase(RWType.UnknownType) && argumentOneAnnotatedType.equalsIgnoreCase(RWType.UnknownType)){
						this.associateCMTypeWithExpression(methodInvocationNode, RWType.UnknownType);
						return;
					}
					String returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, argumentOneAnnotatedType, methodName, argumentTwoAnnotatedType); 
					if(returnType != null){
						this.associateCMTypeWithExpression(methodInvocationNode,  returnType);
						return;
					}else{
						if(methodName.equals(RWTypeRuleCategory.Power)){
							String synthetizedOpName = "java.lang.Math."+methodName;
							String synthetizedResultType = argumentOneAnnotatedType;
							RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentOneAnnotatedType,  argumentTwoAnnotatedType, synthetizedResultType, RWTypeRule.notVerified);
							this.candidateRuleManager.addCMTypeOperation(newUVRule);	
							this.associateCMTypeWithExpression(methodInvocationNode,  synthetizedResultType);
							return;
						}
						String synthetizedOpName = "java.lang.Math."+methodName;
						String synthetizedResultType = argumentOneAnnotatedType + generatedRuleMarker + synthetizedOpName +generatedRuleMarker+ argumentTwoAnnotatedType;
						RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentOneAnnotatedType,  argumentTwoAnnotatedType, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);	
						this.associateCMTypeWithExpression(methodInvocationNode,  synthetizedResultType);
						return;
					}
			}
			if(methodInvocationNode.arguments().size() == 1){
				Expression argument = (Expression)(methodInvocationNode.arguments().get(0));
				String argumentAnnotatedType = this.getAnnotatedTypeForExpression(argument);

				if(argumentAnnotatedType.equalsIgnoreCase(RWType.UnknownType)){
					this.associateCMTypeWithExpression(methodInvocationNode, RWType.UnknownType);
					return;
				}
				String returnType = null;
				returnType=this.cmTypeOperationManager.getReturnType(this.currentProject, argumentAnnotatedType, methodName, RWType.TypeLess); 
				if(returnType != null){
					this.associateCMTypeWithExpression(methodInvocationNode,  returnType);
					return;
				}else{
					String synthetizedOpName = "java.lang.Math."+methodName;
					//abs function
					if(methodName.equalsIgnoreCase(RWTypeRuleCategory.Abosolute_Value)){
						associateCMTypeWithExpression(methodInvocationNode, argumentAnnotatedType);
					}
					if(methodName.equalsIgnoreCase(RWTypeRuleCategory.Sine)
							||
							methodName.equalsIgnoreCase(RWTypeRuleCategory.Cosine)
							||
							methodName.equalsIgnoreCase(RWTypeRuleCategory.Tangent)){
						String synthetizedResultType = argumentAnnotatedType.replace("unit=radians", "unit=");
						RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentAnnotatedType,  RWType.TypeLess, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);	
						this.associateCMTypeWithExpression(methodInvocationNode,  synthetizedResultType);
					}
					if(methodName.equalsIgnoreCase(RWTypeRuleCategory.RadiansToDegree)){
						String synthetizedResultType = argumentAnnotatedType.replace("unit=radians", "unit=degrees");
						RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentAnnotatedType,  RWType.TypeLess, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);	
						this.associateCMTypeWithExpression(methodInvocationNode,  synthetizedResultType);
					}
					if(methodName.equalsIgnoreCase(RWTypeRuleCategory.DegreeToRadians)){
						String synthetizedResultType = argumentAnnotatedType.replace("unit=degrees", "unit=radians");
						RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentAnnotatedType,  RWType.TypeLess, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);	
						this.associateCMTypeWithExpression(methodInvocationNode,  synthetizedResultType);
					}
					if(methodName.equalsIgnoreCase(RWTypeRuleCategory.Arc_Cosine)
							||
							methodName.equalsIgnoreCase(RWTypeRuleCategory.Arc_Sine)
							||
							methodName.equalsIgnoreCase(RWTypeRuleCategory.Arc_Tangent)){
						String synthetizedResultType = "";
						if(argumentAnnotatedType.contains("unit=")){
							synthetizedResultType = argumentAnnotatedType.replace("unit=", "unit=radians"); 
						}else{
							if(argumentAnnotatedType.equals(RWType.TypeLess)){
								synthetizedResultType = "unit=radians";	
							}else{
								synthetizedResultType = argumentAnnotatedType + ";unit=radians";
							}
						}
						RWTypeRule newUVRule = new RWTypeRule(synthetizedOpName, argumentAnnotatedType,  RWType.TypeLess, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);	
						this.associateCMTypeWithExpression(methodInvocationNode,  synthetizedResultType);
					}
				}
			}
		}
	}
	
	private void check_Remander_Operation(String CMTypeAnnotatedTypeOne, String CMTypeAnnotatedTypeTwo, InfixExpression infixExpression, int extendedIndex){
		String infixExpressionType = RWType.UnknownType;
			//type rules part
			if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				infixExpressionType = RWType.UnknownType;
			}		
			else if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				infixExpressionType = RWType.UnknownType;
			}
			else if(!CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				infixExpressionType = CMTypeAnnotatedTypeOne;
			}else{
				String returnType = null;
				returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.REMAINDER, CMTypeAnnotatedTypeTwo);
				if(returnType != null){
					infixExpressionType = returnType;
				}else{
					String operation_type = RWTypeRuleCategory.REMAINDER; 
					String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker +operation_type+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
					RWTypeRule newUVRule = new RWTypeRule(operation_type, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
					this.candidateRuleManager.addCMTypeOperation(newUVRule);
					infixExpressionType = synthetizedResultType;
				}	
			}
			this.associateCMTypeWithExpression(infixExpression, infixExpressionType);

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
					if(returnType != null){
						infixExpressionType = returnType;
					}else{
						String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker +operation_type+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
						RWTypeRule newUVRule = new RWTypeRule(operation_type, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);
						infixExpressionType = synthetizedResultType;
					}	
					
				}
				this.associateCMTypeWithExpression(plusInfixExpression, infixExpressionType);
		
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
					if(returnType != null){
						infixExpressionType = returnType;
					}else{
						String synthetizedResultType = CMTypeAnnotatedTypeOne +  generatedRuleMarker +RWTypeRuleCategory.Multiplication+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
						RWTypeRule newUVRule = new RWTypeRule(RWTypeRuleCategory.Multiplication, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
						this.candidateRuleManager.addCMTypeOperation(newUVRule);
						infixExpressionType = synthetizedResultType;
					}	
				}
				this.associateCMTypeWithExpression(infixExpression, infixExpressionType);
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
		/*
		FractionFormat ff = new FractionFormat();
		try {
			minusOneFraction = ff.parse("-1");
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		*/
			//type rules part
			if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				infixExpressionType = RWType.UnknownType;
			}		
			else if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
				String inverseType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeTwo, RWTypeRuleCategory.Multiplicative_Inverse, "");
				if(inverseType!=null){
					infixExpressionType = inverseType;
				}else{
					String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker +RWTypeRuleCategory.Division+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
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
				if(returnType!=null){
					infixExpressionType = returnType;
				}else{
					String synthetizedResultType = CMTypeAnnotatedTypeOne + generatedRuleMarker+RWTypeRuleCategory.Division+ generatedRuleMarker + CMTypeAnnotatedTypeTwo;
					RWTypeRule newUVRule = new RWTypeRule(RWTypeRuleCategory.Multiplication, CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, synthetizedResultType, RWTypeRule.notVerified);
					this.candidateRuleManager.addCMTypeOperation(newUVRule);
					infixExpressionType = synthetizedResultType;
				}
			}
			this.associateCMTypeWithExpression(infixExpression, infixExpressionType);
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
		if(candidateTypeMapForExpression.get(exp) != null){
			return candidateTypeMapForExpression.get(exp);
		}else{
			candidateTypeMapForExpression.put(exp, RWType.UnknownType);
			return RWType.UnknownType;
		}
	}
	
	@Override
	public void postVisit(ASTNode node) {
		if (node instanceof MethodDeclaration){
			currentAccessMethodKey = null;
		}
		if (node instanceof TypeDeclaration){
			currentAccessClassKey = null;
		}
		
		if( node instanceof CompilationUnit){
			this.candidateRuleManager.storeRules();
		}
		
		EndVisitNode(node);
	}

	private void associateCMTypeWithExpression(Expression exp, String annotatedType){
		if(annotatedType != null){
			RWType cmtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, annotatedType);
			if(cmtype!=null){
				this.candidateTypeMapForExpression.put(exp, cmtype.getEnabledAttributeSet());
			}else{
				this.candidateTypeMapForExpression.put(exp, annotatedType);	
			}
		}
	}
	
}