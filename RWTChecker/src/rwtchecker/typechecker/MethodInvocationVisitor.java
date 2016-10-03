package rwtchecker.typechecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import rwtchecker.rwt.RWType;
import rwtchecker.rwtrules.RWTypeRuleCategory;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.DiagnosticMessage;
import rwtchecker.util.ErrorUtil;

public class MethodInvocationVisitor extends ASTVisitor {
	
	private RWTypeRulesManager cmTypeOperationManager;
	
	private ArrayList<DiagnosticMessage> errorReports = new ArrayList<DiagnosticMessage>();
	
	private HashMap<Expression, String> annotatedTypeTableForExpression = new HashMap<Expression, String>();
	
	private CompilationUnit compilationUnit;
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	
	private IPath currentFilePath;
	private IFile currentFile;
	private IProject currentProject;
	
	private Map<String, ArrayList<String>> methodReturnMap = new HashMap<String, ArrayList<String>>();
	private Map<Integer, ArrayList<String>> varsCommentsMap = new HashMap<Integer, ArrayList<String>>();
	private Map<Integer, ArrayList<String>> funcCommentsMap = new HashMap<Integer, ArrayList<String>>();
	
	private Map<Integer, ArrayList<String>> varsCommentsMapClone = new HashMap<Integer, ArrayList<String>>();
	private Map<Integer, ArrayList<String>> funcCommentsMapClone = new HashMap<Integer, ArrayList<String>>();
	
	private ArrayList<Integer> varsCommentPositionList = new ArrayList<Integer>();
	private ArrayList<Integer> funcCommentPositionList = new ArrayList<Integer>();
	private String currentAccessMethodKey = null;
	private String currentAccessClassKey = null;

	//target methods
	private String targetAccessMethodKey = null;
	private String[] argumentTypes = null; 
	private boolean insideBranch = false;
	private boolean errorInsideBranch = false;
	
	private String methodFinalReturnType = "";
	
	private int annotatedConstantCourt = 0;
	
	public MethodInvocationVisitor(RWTypeRulesManager manager, 
			CompilationUnit compilationUnit, 
			Map<Integer, ArrayList<String>> inputVarsCommentsMap, 
			Map<Integer, ArrayList<String>> inputFuncCommentsMap, 
			String[] argumentTypes, 
			String targetedMethodKey) {
		super(true);
		this.cmTypeOperationManager = manager;
		this.compilationUnit = compilationUnit;
			
		currentFilePath = this.compilationUnit.getJavaElement().getPath();
		currentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(currentFilePath);
		currentProject = currentFile.getProject();

		this.varsCommentsMapClone.putAll(inputVarsCommentsMap);
		this.funcCommentsMapClone.putAll(inputFuncCommentsMap);
		
		this.varsCommentsMap = inputVarsCommentsMap;
		this.funcCommentsMap = inputFuncCommentsMap;
		varsCommentPositionList.addAll(inputVarsCommentsMap.keySet());
		funcCommentPositionList.addAll(inputFuncCommentsMap.keySet());
		Collections.sort(varsCommentPositionList);
		Collections.sort(funcCommentPositionList);
		this.targetAccessMethodKey = targetedMethodKey;
		this.argumentTypes = argumentTypes;
		
	}
	
	/**
	 * get return cmtype for a method invocation with arguments in cmtypes; if no match found, return cmtype.error
	 * @param argument_cmtypes
	 * @param methodKey
	 * @return
	 */
	public static String checkReturnCMType(String[] argument_cmtypes, ArrayList<String> possibleArgumentReturnTypes){
		if(possibleArgumentReturnTypes==null){
			return RWType.UnknownType;
		}
		if(possibleArgumentReturnTypes.size() == 1){
			if(possibleArgumentReturnTypes.get(0).equalsIgnoreCase(RWType.GenericMethod)){
				return RWType.GenericMethod;
			}
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
			//if there is any valid cmtype other than type less
			boolean validCMType = false;
			String thisArgumentInCmtype = argument_cmtypes[0]; 
			if(!thisArgumentInCmtype.equals(RWType.UnknownType)){
				validCMType = true;
			}
			for (int j=1;j<argument_cmtypes.length;j++){
				thisArgumentInCmtype = thisArgumentInCmtype + "," + argument_cmtypes[j];
				if(!argument_cmtypes[j].equals(RWType.UnknownType)){
					validCMType = true;
				}
			}
			for(int i=0;i<possibleArgumentReturnTypes.size();i++){
				String possibleArgumentReturnType = possibleArgumentReturnTypes.get(i);
				if(possibleArgumentReturnType.startsWith(thisArgumentInCmtype+":")){
					return possibleArgumentReturnType.split(":")[1];
				}
			}
			
			if(validCMType && possibleArgumentReturnTypes.size()>0 ){
				//error here, no match cmtype argment found
					return RWType.error_source;	
			}
			
			if(possibleArgumentReturnTypes.size() == 1){
				if(possibleArgumentReturnTypes.get(0).split(":").length == 2){
					return possibleArgumentReturnTypes.get(0).split(":")[1];	
				}
			}
		}
		return RWType.UnknownType;
	}
	
	private String handleGenericMethod(CompilationUnit compilationResult, Map<Integer, ArrayList<String>> inputVarsCommentsMap, Map<Integer, ArrayList<String>> inputFuncCommentsMap, String[] argumentCMTypes, String targetedMethodKey){
	    MethodInvocationVisitor miv = new MethodInvocationVisitor(this.cmTypeOperationManager, compilationResult, inputVarsCommentsMap, inputFuncCommentsMap, argumentCMTypes, targetedMethodKey);
		compilationResult.accept(miv);
		String genericReturnType = miv.getMethodFinalReturnType();
		return genericReturnType;
	}
	
	public void preVisit(ASTNode node) {
		
		if(node instanceof TypeDeclaration){
			TypeDeclaration typeDelNode = (TypeDeclaration)node;
			String classKey = typeDelNode.resolveBinding().getKey();
			this.currentAccessClassKey = classKey; 
		}
		
		if(node instanceof MethodDeclaration){
			MethodDeclaration methodDec = (MethodDeclaration)node;
			String methodKey = methodDec.resolveBinding().getKey();
			this.currentAccessMethodKey = methodKey; 
			
			if(funcCommentPositionList.size() > 0){
				//check for method annotations
				int startingLineNumber = compilationUnit.getLineNumber(methodDec.getStartPosition()) -1;
				int endLineNumber = compilationUnit.getLineNumber(methodDec.getStartPosition() + methodDec.getLength()) - 1;
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
					annotatedConstantCourt = 0;
					
					/****read annotation*****/
					for (int i=0;i<varsComments.size();i++){
						String comment = varsComments.get(i);
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
		
		if(node instanceof NumberLiteral){
			/***read constant annotation***/
			annotatedConstantCourt++;
			String annotatedConstantName = RWType.annotation_const+String.valueOf(annotatedConstantCourt);
			Map<String, String> localVariableMap = this.allVariableMap.get(currentAccessMethodKey);
			if(localVariableMap !=null){
				String cmtype = localVariableMap.get(annotatedConstantName);
				if(localVariableMap.containsKey(annotatedConstantName)){
					localVariableMap.remove(annotatedConstantName);
				}
				this.associateCMTypeWithExpression((NumberLiteral)node, cmtype);
			}
		}
	}
	
	//if not the correct method, ignore it
	@Override
	public boolean visit(MethodDeclaration methodDeclaration){
		String methodKey = methodDeclaration.resolveBinding().getKey();
		if(!methodKey.equals(targetAccessMethodKey)){
			//not target method body
			return false;
		}
		Map<String, String> localVariableMap = this.allVariableMap.get(methodKey);
		if(localVariableMap == null){
			localVariableMap = new HashMap<String, String>();	
		}
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		for(int i=0;i<parameters.size();i++){
			SingleVariableDeclaration parameter = parameters.get(i);
			String paraName = parameter.getName().getIdentifier();
			String cmtype = this.argumentTypes[i];
			if(cmtype.length()>0){
				localVariableMap.put(paraName, cmtype);	
			}
		}
		this.allVariableMap.put(methodKey, localVariableMap);
		return true;
	}
	
	@Override
	public boolean visit(ThisExpression thisExpression){
		Map<String, String> localVariableMap = this.allVariableMap.get(currentAccessMethodKey);
		if(localVariableMap !=null){
			String thisExpressionType = localVariableMap.get("this");
			this.associateCMTypeWithExpression(thisExpression, thisExpressionType);
		}
		return true;
	}
	
	@Override
	public boolean visit(IfStatement ifStatement){
		//already in a branch and errors are found
		if(insideBranch && errorInsideBranch){
			return false;
		}else{
			insideBranch = true;	
		}
		return true;
	}
	
	@Override
	public boolean visit(FieldAccess fieldAccess){
		Map<String, String> localVariableMap = this.allVariableMap.get(currentAccessMethodKey);
		if(localVariableMap !=null){
			if(localVariableMap.containsKey(fieldAccess.toString())){
				this.associateCMTypeWithExpression(fieldAccess, localVariableMap.get(fieldAccess.toString()));
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean visit(QualifiedName qualifiedName){
		Map<String, String> localVariableMap = this.allVariableMap.get(currentAccessMethodKey);
		if(localVariableMap !=null){
			if(localVariableMap.containsKey(qualifiedName.toString())){
				this.associateCMTypeWithExpression(qualifiedName, localVariableMap.get(qualifiedName.toString()));
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
						thisCMType = fieldVariableMap.get(variableBinding.getName());
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
													if(variableName.equals(variableBinding.getName())){
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
	
	private void assignFinalReturnType(String newReturnType){
		if(newReturnType.equals(RWType.TypeLess)){
			return;
		}
		if(this.methodFinalReturnType.equals(RWType.error_source)){
			return;
		}
		if(this.methodFinalReturnType.length()>0  && !this.methodFinalReturnType.equals(newReturnType) ){
			this.methodFinalReturnType = RWType.error_source;
			return;
		}
		if(this.methodFinalReturnType.length()== 0){
			this.methodFinalReturnType = newReturnType;
		}
		
	}
	
	public void EndVisitNode(ASTNode node){
		
		if(node instanceof IfStatement){
			insideBranch = false;
			errorInsideBranch = false;
		}
		
		if(node instanceof ReturnStatement){
			ReturnStatement returnStatement = (ReturnStatement)node;
			if(!insideBranch){
				//not inside a branch
				String thisReturnType = this.getAnnotatedTypeForExpression(returnStatement.getExpression());
				assignFinalReturnType(thisReturnType);

			}else{
				if(!errorInsideBranch){
					//no error has been found in this branch: return cmtype should be valid
					String thisReturnType = this.getAnnotatedTypeForExpression(returnStatement.getExpression());
					assignFinalReturnType(thisReturnType);
				}
			}
		}
		
		if(node instanceof MethodInvocation){
			final MethodInvocation methodInvocationNode = (MethodInvocation)node;
			final IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
			String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
			String methodDeclPath = iMethodBinding.getJavaElement().getPath().toString();
			String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
			//check arguments cmtype
			final String[] argumentCMTypes = new String[methodInvocationNode.arguments().size()];
			for (int i=0;i<methodInvocationNode.arguments().size();i++){
				Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
				String argumentCMType = this.getAnnotatedTypeForExpression(exp);
				argumentCMTypes[i] = argumentCMType;
//				if(!argumentCMType.equals(CMType.UnknownType)){
//					hasTypedArguments = true;
//				}
			}
		
			/*
			if(!hasTypedArguments){
				//no annotation, no argument types; do nothing
				return;
			}else{
			*/
				if(currentUnitPath.equals(methodDeclPath)){
					if(methodReturnMap!=null){
						String returnType = this.checkReturnCMType(argumentCMTypes, this.methodReturnMap.get(methodDeclKey));
						associateCMTypeWithExpression(methodInvocationNode, returnType);
						if(returnType.equals(RWType.GenericMethod)){
							returnType = handleGenericMethod(this.compilationUnit, this.varsCommentsMapClone, this.funcCommentsMapClone, argumentCMTypes, methodDeclKey);
						}
						associateCMTypeWithExpression(methodInvocationNode, returnType);
//						if(returnType.equals(CMType.UnknownType)){
//							//# if no result being found, detect candidate cm type rules
//							String synthesizedCMType = methodDeclKey;
//							associateCandidateCMTypeWithExp(methodInvocationNode, synthesizedCMType);	
//						}else{
//							associateCandidateCMTypeWithExp(methodInvocationNode, returnType);
//						}
						if(returnType.equals(RWType.error_source)){
							addNewErrorMessage(node , ErrorUtil.methodInvocationError(), DiagnosticMessage.ERROR);
						}
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
											int methodNameStartNumber = otherCompilationResult.getLineNumber(methodDeclaration.getName().getStartPosition()) - 1;
											//the line before method body; annotation ends here
											int possibleCommentNumber = methodNameStartNumber-1;
											String targetComment = "";
											int commentEndingLine = 0;
											String lineContents = externalSourceList[possibleCommentNumber].trim();
											if(lineContents.endsWith("cm*/")){
												commentEndingLine = possibleCommentNumber; 
											}
											if(commentEndingLine > 0){
												String firstLineContents = externalSourceList[commentEndingLine];
												if(firstLineContents.endsWith("cm*/")){
													targetComment = firstLineContents.replace("/*cm "+CommentVisitor.func, "").replace(" cm*/", "");
												}else{
													int lineCourt = commentEndingLine;
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
												if(returnType.equals(RWType.GenericMethod)){
													Map<Integer, ArrayList<String>> varsCommentsMap = new HashMap<Integer, ArrayList<String>>();
													Map<Integer, ArrayList<String>> funcCommentsMap = new HashMap<Integer, ArrayList<String>>();
													for (Comment comment : (List<Comment>) otherCompilationResult.getCommentList()) {
														CommentVisitor thisCommentVisitor = new CommentVisitor(otherCompilationResult, externalSourceList);
											            comment.accept(thisCommentVisitor);
											            if(thisCommentVisitor.isDefComment()){
											            	varsCommentsMap.put(thisCommentVisitor.getLineCourt(), thisCommentVisitor.getCommentConents());
											            }
											            if(thisCommentVisitor.isFuncComment()){
											            	funcCommentsMap.put(thisCommentVisitor.getLineCourt(), thisCommentVisitor.getCommentConents());
											            }
											        }
													returnType = handleGenericMethod(otherCompilationResult, varsCommentsMap, funcCommentsMap, argumentCMTypes, methodDeclBinding.getKey());
												}
												associateCMTypeWithExpression(methodInvocationNode, returnType);
												if(returnType.equals(RWType.error_source)){
													addNewErrorMessage(methodInvocationNode , ErrorUtil.methodInvocationError(), DiagnosticMessage.ERROR);
												}
											}else{
												associateCMTypeWithExpression(methodInvocationNode, RWType.UnknownType);
											}
										}
									return false;
								}
							});
						}
				}
//			}
			for (int i=0;i<methodInvocationNode.arguments().size();i++){
				Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
				String argumentCMType = this.getAnnotatedTypeForExpression(exp);
				if(argumentCMType.equals(RWType.error_propogate) || argumentCMType.equals(RWType.error_source)){
					this.associateCMTypeWithExpression(methodInvocationNode, RWType.error_propogate);
					return;
				}
			}
			this.checkMathMethodInvocation(methodInvocationNode);
//			this.checkCollectionAccess(methodInvocationNode);
		}
		
		if(node instanceof QualifiedName){
			QualifiedName qualifiedName = (QualifiedName)node;
			String qualifierType =  this.getAnnotatedTypeForExpression(qualifiedName.getQualifier());
			String nameType =  this.getAnnotatedTypeForExpression(qualifiedName.getName());
			if(!checkConsistency(qualifierType, nameType)){
				//inconsistent attributes
				this.addNewErrorMessage(qualifiedName, ErrorUtil.getInconsistentAttributeError(),DiagnosticMessage.ERROR);
			}else{
				String unitedSetsType = NewTypeCheckerVisitor.uniteTwoSets(qualifierType, nameType, cmTypeOperationManager);
				this.associateCMTypeWithExpression(qualifiedName, unitedSetsType);
			}
		}
		
		else if(node instanceof FieldAccess){
			FieldAccess fieldAccessNode = (FieldAccess)node;
			String annotatedFieldAccessType = this.getAnnotatedTypeForExpression(fieldAccessNode.getExpression());
			String annotatedIdentifierType = this.getAnnotatedTypeForExpression(fieldAccessNode.getName());
			//using union operation for the two types
			if(!checkConsistency(annotatedFieldAccessType, annotatedIdentifierType)){
				//inconsistent attributes
				this.addNewErrorMessage(fieldAccessNode, ErrorUtil.getInconsistentAttributeError(),DiagnosticMessage.ERROR);
			}else{
				String unitedSetsType = NewTypeCheckerVisitor.uniteTwoSets(annotatedFieldAccessType, annotatedIdentifierType, cmTypeOperationManager);
				this.associateCMTypeWithExpression(fieldAccessNode, unitedSetsType);
			}
		}
		
		else if(node instanceof ArrayAccess){
			ArrayAccess arrayAccess = (ArrayAccess)node;
			String annotatedArraytType =  this.getAnnotatedTypeForExpression(arrayAccess.getArray());
			this.associateCMTypeWithExpression(arrayAccess, annotatedArraytType);
			if(annotatedArraytType.equals(RWType.error_propogate) || annotatedArraytType.equals(RWType.error_source)){
				this.associateCMTypeWithExpression(arrayAccess, RWType.error_propogate);
			}
		}
		
		else if(node instanceof InfixExpression){
			InfixExpression infixExpression = (InfixExpression)node;
			Expression leftEP = infixExpression.getLeftOperand();			
			Expression rightEP = infixExpression.getRightOperand();
			String CMTypeAnnotatedTypeOne = this.getAnnotatedTypeForExpression(leftEP);
			String CMTypeAnnotatedTypeTwo = this.getAnnotatedTypeForExpression(rightEP);
					if((CMTypeAnnotatedTypeOne.equals(RWType.error_source)) || (CMTypeAnnotatedTypeOne.equals(RWType.error_propogate))
							||	(CMTypeAnnotatedTypeTwo.equals(RWType.error_source)) || (CMTypeAnnotatedTypeTwo.equals(RWType.error_propogate))){
						this.associateCMTypeWithExpression(infixExpression, RWType.error_propogate);
						return;
					}
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
							addNewErrorMessage(infixExpression , ErrorUtil.typeInconsistency(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo), DiagnosticMessage.ERROR);	
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
			if(argumentCMType.equals(RWType.error_propogate) || argumentCMType.equals(RWType.error_source)){
				this.associateCMTypeWithExpression(parenthesizedExpression, RWType.error_propogate);
				return;
			}else{
				this.associateCMTypeWithExpression(parenthesizedExpression, argumentCMType);
			}
		}

		//error checking here
		else if(node instanceof Assignment){
			Assignment assignmentNode = (Assignment)node;
			Expression leftExp = assignmentNode.getLeftHandSide();
			Expression rightExp = assignmentNode.getRightHandSide();
			String leftCMType = this.getAnnotatedTypeForExpression(leftExp);
			String rightCMType = this.getAnnotatedTypeForExpression(rightExp);
			if(leftCMType.equals(rightCMType)){
				return;
			}
			//error propagate
			if((leftCMType.equals(RWType.error_source)) || (leftCMType.equals(RWType.error_propogate))
					||	(rightCMType.equals(RWType.error_source)) || (rightCMType.equals(RWType.error_propogate))){
				return;
			}
			else if(!leftCMType.equalsIgnoreCase(RWType.UnknownType) &&
					!leftCMType.equalsIgnoreCase(rightCMType) && 
					!rightCMType.equalsIgnoreCase(RWType.UnknownType) ){
				addNewErrorMessage(node , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);	
			}
			//now, no inference included here
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
				//error propagate
				if(rightCMType.equals(RWType.error_propogate) || rightCMType.equals(RWType.error_source)){
					return;
				}
				if(leftCMType.equals(RWType.error_propogate) || leftCMType.equals(RWType.error_source)){
					return;
				}
				else if(!leftCMType.equalsIgnoreCase(RWType.UnknownType) &&
						!leftCMType.equalsIgnoreCase(rightCMType) && 
						!rightCMType.equalsIgnoreCase(RWType.UnknownType) ){
					addNewErrorMessage(node , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);	
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
			String methodName = "java.lang.Math."+iMethodBinding.getName();
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
					}else {
						addNewErrorMessage(methodInvocationNode , ErrorUtil.getUndeclaredCalculation(methodInvocationNode.toString()),  DiagnosticMessage.WARNING);
						this.associateCMTypeWithExpression(methodInvocationNode, RWType.error_source);
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
					if((returnType != null)){
						this.associateCMTypeWithExpression(methodInvocationNode, returnType);
					}else{
						this.associateCMTypeWithExpression(methodInvocationNode,  RWType.error_source);	
					}
				//abs function
				if(methodName.equalsIgnoreCase(RWTypeRuleCategory.Abosolute_Value)){
					associateCMTypeWithExpression(methodInvocationNode, argumentAnnotatedType);
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
					addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
					infixExpressionType = RWType.UnknownType;
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
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
						if(!CMTypeAnnotatedTypeOne.equalsIgnoreCase(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equalsIgnoreCase(RWType.UnknownType)){							
							addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
							infixExpressionType = RWType.UnknownType;
						}
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
		//type rules part
		if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
			infixExpressionType = RWType.UnknownType;
		}		
		else if(CMTypeAnnotatedTypeOne.equals(RWType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(RWType.UnknownType)){
			String inverseType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeTwo, RWTypeRuleCategory.Multiplicative_Inverse, "");
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
			returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, RWTypeRuleCategory.Division, CMTypeAnnotatedTypeTwo);
			if(returnType != null ){
				infixExpressionType = returnType;
			}else{
				addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
				infixExpressionType = RWType.UnknownType;
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
		if(annotatedTypeTableForExpression.get(exp) != null){
			return annotatedTypeTableForExpression.get(exp);
		}else{
			annotatedTypeTableForExpression.put(exp, RWType.UnknownType);
			return RWType.UnknownType;
		}
	}
	
	public ArrayList<DiagnosticMessage> getErrorReports() {
		return errorReports;
	}
	
	
	@Override
	public void postVisit(ASTNode node) {

		if (node instanceof TypeDeclaration){
			currentAccessClassKey = null;
		}
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
		if(insideBranch){
			//in a branch, error found
			errorInsideBranch = true;
		}
	}
	
	
	private void associateCMTypeWithExpression(Expression exp, String annotatedType){
		if(annotatedType != null){
			RWType cmtype = RWTSystemUtil.getCMTypeFromTypeName(currentProject, annotatedType);
			if(cmtype!=null){
				this.annotatedTypeTableForExpression.put(exp, cmtype.getEnabledAttributeSet());
			}else{
				this.annotatedTypeTableForExpression.put(exp, annotatedType);	
			}
		}
	}
	
	private boolean checkConsistency(String setA, String setB){
		if(setA.length()==0){
			return true;
		}
		if(setB.length()==0){
			return true;
		}
		String unitSet = setA + ";" + setB;
		Pattern searchP = Pattern.compile("((.+=).*?);.*(\2.*)[;|$]");
		Matcher m = searchP.matcher(unitSet);
		while (m.find()) {
			String attributeInA = m.group(1);
			String attributeInB = m.group(3);
			if(!attributeInA.endsWith("=") &&
			   !attributeInB.endsWith("=") &&
			   !attributeInA.equals(attributeInB)){
				return false;
			}
		}
		return true;
	}
	
	public String getMethodFinalReturnType() {
		return methodFinalReturnType;
	}
}