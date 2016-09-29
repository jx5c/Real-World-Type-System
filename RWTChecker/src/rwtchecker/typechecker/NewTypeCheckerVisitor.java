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
import java.util.HashSet;
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
import org.eclipse.jdt.internal.compiler.ast.Literal;

import rwtchecker.CM.CMType;
import rwtchecker.CMRules.CMTypeRuleCategory;
import rwtchecker.CMRules.CMTypeRulesManager;
import rwtchecker.util.CMModelUtil;
import rwtchecker.util.DiagnosticMessage;
import rwtchecker.util.ErrorUtil;

public class NewTypeCheckerVisitor extends ASTVisitor {
	
	public static HashSet<String> cmtypeHashSet = new HashSet<String>();
	
	private CMTypeRulesManager cmTypeOperationManager;
	
	private static ArrayList<DiagnosticMessage> errorReports = new ArrayList<DiagnosticMessage>();
	
	private HashMap<Expression, String> annotatedTypeTableForExpression = new HashMap<Expression, String>();
	
	private CompilationUnit compilationUnit;
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	
	private IPath currentFilePath;
	private IFile currentFile;
	private static IProject currentProject;
	
	private Map<String, ArrayList<String>> methodReturnMap = new HashMap<String, ArrayList<String>>();
	private Map<Integer, ArrayList<String>> varsCommentsMap = new HashMap<Integer, ArrayList<String>>();
	private Map<Integer, ArrayList<String>> funcCommentsMap = new HashMap<Integer, ArrayList<String>>();
	
	private Map<Integer, ArrayList<String>> varsCommentsMapClone = new HashMap<Integer, ArrayList<String>>();
	private Map<Integer, ArrayList<String>> funcCommentsMapClone = new HashMap<Integer, ArrayList<String>>();
	
	private ArrayList<Integer> varsCommentPositionList = new ArrayList<Integer>();
	private ArrayList<Integer> funcCommentPositionList = new ArrayList<Integer>();
	private String currentAccessMethodKey = null;
	private String currentAccessClassKey = null;
	
	private static String RequireAnnotation = "require_argument_annotation";
	
	private ArrayList<String> usedCMTypesInCurrentMethod = new ArrayList<String>();
	
	private ArrayList<String> usedCMTypesList = new ArrayList<String>();
	
	private int annotatedConstantCourt = 0;
	private int variableCourt = 0;
	
	public int getVariableCourt() {
		return variableCourt;
	}

	public NewTypeCheckerVisitor(CMTypeRulesManager manager, CompilationUnit compilationUnit, 
			Map<Integer, ArrayList<String>> inputVarsCommentsMap, Map<Integer, ArrayList<String>> inputFuncCommentsMap) {
		super(true);
		this.cmTypeOperationManager = manager;
		this.compilationUnit = compilationUnit;
			
		this.usedCMTypesInCurrentMethod.clear();
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
		errorReports.clear();
	}
	
	/**
	 * get return cmtype for a method invocation with arguments in cmtypes; if no match found, return cmtype.error
	 * @param argument_cmtypes
	 * @param methodKey
	 * @return
	 */
	private String checkReturnCMType(MethodInvocation methodInvocationNode, String[] argument_cmtypes, ArrayList<String> functionAnnotations){
		String returnCMTypeAtt = CMType.UnknownType;
		if(functionAnnotations==null){
			return CMType.UnknownType;
		}
		
		if(functionAnnotations.size() == 1){
			if(functionAnnotations.get(0).equalsIgnoreCase(CMType.GenericMethod)){
				return CMType.GenericMethod;
			}
			if(argument_cmtypes.length == 0){
				//no argument, only return types
				for(int i=0;i<functionAnnotations.size();i++){
					String possibleReturnType = functionAnnotations.get(i).trim();
					if(possibleReturnType.startsWith(":")){
						String returnCMTypeName = functionAnnotations.get(0).split(":")[1].trim();
						CMType returnCMtype = CMModelUtil.getCMTypeFromTypeName(currentProject, returnCMTypeName);
						if(returnCMtype!=null){
							returnCMTypeAtt = returnCMtype.getEnabledAttributeSet();
							return returnCMTypeAtt;
						}
					}
				}
			}
			if(functionAnnotations.get(0).contains(":")){
				String parameterCMTypeInString = functionAnnotations.get(0).split(":")[0].trim();
				String[] parameterCMTypeArray = parameterCMTypeInString.split(",");
				
				boolean validParameterCMType = false;
				for (int i=0;i<parameterCMTypeArray.length;i++){
					String returnCMTypeName = parameterCMTypeArray[i].trim();	
					CMType returnCMtype = CMModelUtil.getCMTypeFromTypeName(currentProject, returnCMTypeName);
					if(returnCMtype!=null){
						if(!returnCMtype.getEnabledAttributeSet().equals(CMType.UnknownType)){
							validParameterCMType = true;	
						}
					}
				}
				String returnCMTypeName = functionAnnotations.get(0).substring(functionAnnotations.get(0).indexOf(":")+1);	
				CMType returnCMtype = CMModelUtil.getCMTypeFromTypeName(currentProject, returnCMTypeName);
				if(returnCMtype!=null){
					returnCMTypeAtt = returnCMtype.getEnabledAttributeSet();
				} 
				if(!validParameterCMType){
					//valid parameter cmtype: with non-empty attributes; if all empty type, return results without checking errors;
					return returnCMTypeAtt;
				}else{
					//if there is any valid cmtype as input arguments
					boolean validArgumentCMType = false;
					for (int j=0;j<argument_cmtypes.length;j++){
						if(!argument_cmtypes[j].equals(CMType.UnknownType)){
							validArgumentCMType = true;
						}
					}
					if(!validArgumentCMType){
						//no annotation; but has type annotation for method declaration
						addNewErrorMessage(methodInvocationNode , ErrorUtil.methodArgumentMissingAnnotation(), DiagnosticMessage.WARNING);	
					}else{
						String functionAnnotation = functionAnnotations.get(0);
						String inputsAsString = functionAnnotation.split(":")[0].trim();
						if(inputsAsString.length()==0){
							addNewErrorMessage(methodInvocationNode , ErrorUtil.methodInproperArgumentAnnotation(), DiagnosticMessage.WARNING);
						}else{
							String[] inputsAnnotated = inputsAsString.split(",");	
							if(inputsAnnotated.length != argument_cmtypes.length){
								addNewErrorMessage(methodInvocationNode , ErrorUtil.methodInvocationError(), DiagnosticMessage.ERROR);
							}else{
								for(int i=0;i<inputsAnnotated.length;i++){
									String annotatedTypeName =  inputsAnnotated[i].trim();
									if(annotatedTypeName.equalsIgnoreCase(CMType.NonType) || 
											inputsAnnotated[i].trim().length()==0){
										if(argument_cmtypes[i].length()>0){
											addNewErrorMessage(methodInvocationNode , ErrorUtil.methodInproperArgumentAnnotation(), DiagnosticMessage.ERROR);
										}
										continue;
									}
									CMType tempCMtype = CMModelUtil.getCMTypeFromTypeName(currentProject, annotatedTypeName);
									if(tempCMtype!=null){
										if(!tempCMtype.getEnabledAttributeSet().equals(argument_cmtypes[i])){
											String annotatedTypeAttSet = tempCMtype.getEnabledAttributeSet();
											String tempReturnType = cmTypeOperationManager.getReturnType(currentProject, annotatedTypeAttSet, CMTypeRuleCategory.Assignable,  argument_cmtypes[i]);
											if(tempReturnType==null){
												addNewErrorMessage(methodInvocationNode , ErrorUtil.methodInvocationError(), DiagnosticMessage.ERROR);	
											}
										}
									}else{
										addNewErrorMessage(methodInvocationNode , ErrorUtil.methodInproperArgumentAnnotation(), DiagnosticMessage.ERROR);
									}
								}
							}
						}
					}
				}
			}
		}
		return returnCMTypeAtt;
	}
	
	public void preVisit(ASTNode node) {
		
		if(node instanceof MethodDeclaration){
			MethodDeclaration methodDelNode = (MethodDeclaration)node;
			String methodKey = methodDelNode.resolveBinding().getKey();
			this.currentAccessMethodKey = methodKey; 
			
			this.usedCMTypesInCurrentMethod.clear();
			
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
//					/****deal with constant annotation*****/
//					String[] keys = localVariableMap.keySet().toArray(new String[localVariableMap.size()]);
//					for (String key : keys){
//						if(key.contains(CMType.annotation_const)){
//							localVariableMap.remove(key);
//						}
//					}
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
			String annotatedConstantName = CMType.annotation_const+String.valueOf(annotatedConstantCourt);
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
	public boolean visit(ArrayAccess arrayAccess){
		ASTNode parentNode = arrayAccess.getParent();
		while (true){
			if(parentNode instanceof MethodDeclaration){
				MethodDeclaration methodDel = (MethodDeclaration) parentNode;
				String methodKey = methodDel.resolveBinding().getKey();
				Map<String, String> fieldVariableMap = this.allVariableMap.get(methodKey);
				if(fieldVariableMap!=null){
					String thisCMType = fieldVariableMap.get(arrayAccess.toString());
					this.associateCMTypeWithExpression(arrayAccess, thisCMType);
				}
				break;
			}
			if(parentNode instanceof TypeDeclaration){
				TypeDeclaration typeDeclaration = (TypeDeclaration) parentNode;
				String classKey = typeDeclaration.resolveBinding().getKey();
				Map<String, String> fieldVariableMap = this.allVariableMap.get(classKey);
				if(fieldVariableMap!=null){
					String thisCMType = fieldVariableMap.get(arrayAccess.toString());
					this.associateCMTypeWithExpression(arrayAccess, thisCMType);
				}
				break;
			}
			parentNode = parentNode.getParent();
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
				final String classDeclKey = variableBinding.getDeclaringClass().getKey();
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
						final Map<Integer, ArrayList<String>> outsideClassVarsCommentsMap = new HashMap<Integer, ArrayList<String>>();
						
						for (Comment comment : (List<Comment>) otherCompilationResult.getCommentList()) {
							CommentVisitor thisCommentVisitor = new CommentVisitor(otherCompilationResult, externalSourceList);
				            comment.accept(thisCommentVisitor);
				            if(thisCommentVisitor.isDefComment()){
				            	outsideClassVarsCommentsMap.put(thisCommentVisitor.getLineCourt(), thisCommentVisitor.getCommentConents());
				            }
				        }
						final ArrayList<Integer> outsideClassVarsCommentPositionList = new ArrayList<Integer>();
						outsideClassVarsCommentPositionList.addAll(outsideClassVarsCommentsMap.keySet());
						Collections.sort(outsideClassVarsCommentPositionList);
							otherCompilationResult.accept(new ASTVisitor() {
								boolean accessingtargetClass = false; 
								boolean accessMethodDel = false;
								public void preVisit(ASTNode node) {
									if(node instanceof TypeDeclaration){
										TypeDeclaration typeDelNode = (TypeDeclaration)node;
										String classKey = typeDelNode.resolveBinding().getKey();
										if(classKey.equals(classDeclKey)){
											accessingtargetClass = true;
										}
									}
									if(node instanceof MethodDeclaration){
										accessMethodDel = true;
									}
									//consider only target class annotations
									if(accessingtargetClass && !accessMethodDel){
										if(outsideClassVarsCommentPositionList.size() >0){
											int currentLineNumber = otherCompilationResult.getLineNumber(node.getStartPosition()) -1;
											if (currentLineNumber >= outsideClassVarsCommentPositionList.get(0)){
												int pastCommentLineNum = outsideClassVarsCommentPositionList.get(0);
												ArrayList<String> varsComments = outsideClassVarsCommentsMap.get(pastCommentLineNum);
												//class declaration
												for (int i=0;i<varsComments.size();i++){
													String comment = varsComments.get(i);
													comment = comment.replace("cmt(", "");
													String variableName =  comment.split("\\)=")[0];
													String cmtype = comment.split("\\)=")[1];
													if(variableName.equals(variableBinding.getName())){
														associateCMTypeWithExpression(simpleName, cmtype);
													}
												}
												outsideClassVarsCommentPositionList.remove(0);
											}
										}
									}
								}
								public void postVisit(ASTNode node) {
									if(node instanceof TypeDeclaration){
										TypeDeclaration typeDelNode = (TypeDeclaration)node;
										String classKey = typeDelNode.resolveBinding().getKey();
										if(classKey.equals(classDeclKey)){
											accessingtargetClass = false;
										}
									}
									if(node instanceof MethodDeclaration){
										accessMethodDel = false;
									}
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
	
	private String handleGenericMethod(CompilationUnit compilationResult, Map<Integer, ArrayList<String>> inputVarsCommentsMap, Map<Integer, ArrayList<String>> inputFuncCommentsMap, String[] argumentCMTypes, String targetedMethodKey){
		boolean validType = false;
		for(String argumentType:argumentCMTypes){
			if(argumentType.length()>0){
				validType = true;
				break;
			}
		}
		if(!validType){
			return CMType.TypeLess;
		}
	    MethodInvocationVisitor miv = new MethodInvocationVisitor(this.cmTypeOperationManager, compilationResult, inputVarsCommentsMap, inputFuncCommentsMap, argumentCMTypes, targetedMethodKey);
		compilationResult.accept(miv);
		String genericReturnType = miv.getMethodFinalReturnType();
		return genericReturnType;
	}
	
	public void EndVisitNode(ASTNode node){
		if(node instanceof MethodInvocation){
			final MethodInvocation methodInvocationNode = (MethodInvocation)node;
			final IMethodBinding iMethodBinding = methodInvocationNode.resolveMethodBinding();
			String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
			String methodDeclPath = iMethodBinding.getJavaElement().getPath().toString();
			String methodDeclKey = iMethodBinding.getMethodDeclaration().getKey();
			//check arguments cmtype
			final String expCMType = this.getAnnotatedTypeForExpression(methodInvocationNode.getExpression());
			final String[] argumentCMTypes = new String[methodInvocationNode.arguments().size()];
			for (int i=0;i<methodInvocationNode.arguments().size();i++){
				Expression exp = (Expression)(methodInvocationNode.arguments().get(i));
				String argumentCMType = this.getAnnotatedTypeForExpression(exp);
				argumentCMTypes[i] = argumentCMType;
			}
				if(currentUnitPath.equals(methodDeclPath)){
					if(methodReturnMap!=null){
						String returnType = checkReturnCMType(methodInvocationNode, argumentCMTypes, this.methodReturnMap.get(methodDeclKey));
						if(returnType.equalsIgnoreCase(CMType.GenericMethod)){
							returnType = handleGenericMethod(this.compilationUnit, this.varsCommentsMapClone, this.funcCommentsMapClone, argumentCMTypes, methodDeclKey);
						}
						if(expCMType.equals(CMType.TypeLess)){
							associateCMTypeWithExpression(methodInvocationNode, returnType);	
						}else{
							String finalReturnType = uniteTwoSets(expCMType, returnType, cmTypeOperationManager);
							associateCMTypeWithExpression(methodInvocationNode, finalReturnType);
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
												String returnType = checkReturnCMType(methodInvocationNode, argumentCMTypes, possibleArgumentReturnTypes);
												if(returnType.equals(CMType.GenericMethod)){
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
												if(expCMType.equals(CMType.TypeLess)){
													associateCMTypeWithExpression(methodInvocationNode, returnType);	
												}else{
													String finalReturnType = uniteTwoSets(expCMType, returnType, cmTypeOperationManager);
													associateCMTypeWithExpression(methodInvocationNode, finalReturnType);
												}
											}else{
												associateCMTypeWithExpression(methodInvocationNode, CMType.UnknownType);
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
			String qualifierType =  this.getAnnotatedTypeForExpression(qualifiedName.getQualifier());
			String nameType =  this.getAnnotatedTypeForExpression(qualifiedName.getName());
			if(!checkConsistency(qualifierType, nameType)){
				//inconsistent attributes
				this.addNewErrorMessage(qualifiedName, ErrorUtil.getInconsistentAttributeError(),DiagnosticMessage.ERROR);
			}else{
				String unitedSetsType = uniteTwoSets(qualifierType, nameType, cmTypeOperationManager);
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
				String unitedSetsType = uniteTwoSets(annotatedFieldAccessType, annotatedIdentifierType, cmTypeOperationManager);
				this.associateCMTypeWithExpression(fieldAccessNode, unitedSetsType);
			}
		}
		
		else if(node instanceof ArrayAccess){
			ArrayAccess arrayAccess = (ArrayAccess)node;
			String annotatedArraytType =  this.getAnnotatedTypeForExpression(arrayAccess.getArray());
			this.associateCMTypeWithExpression(arrayAccess, annotatedArraytType);
			if(annotatedArraytType.equals(CMType.error_propogate) || annotatedArraytType.equals(CMType.error_source)){
				this.associateCMTypeWithExpression(arrayAccess, CMType.error_propogate);
			}
		}
		
		else if(node instanceof InfixExpression){
			InfixExpression infixExpression = (InfixExpression)node;
			Expression leftEP = infixExpression.getLeftOperand();			
			Expression rightEP = infixExpression.getRightOperand();
			String CMTypeAnnotatedTypeOne = this.getAnnotatedTypeForExpression(leftEP);
			String CMTypeAnnotatedTypeTwo = this.getAnnotatedTypeForExpression(rightEP);
					if((CMTypeAnnotatedTypeOne.equals(CMType.error_source)) || (CMTypeAnnotatedTypeOne.equals(CMType.error_propogate))
							||	(CMTypeAnnotatedTypeTwo.equals(CMType.error_source)) || (CMTypeAnnotatedTypeTwo.equals(CMType.error_propogate))){
						this.associateCMTypeWithExpression(infixExpression, CMType.error_propogate);
						return;
					}
					if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
						this.associateCMTypeWithExpression(infixExpression, CMType.UnknownType);
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
						CMTypeAnnotatedTypeOne.equals(CMType.UnknownType)||
						CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
					return;
				}else{
					String returnType = null;
					returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, CMTypeRuleCategory.Comparable, CMTypeAnnotatedTypeTwo);
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
				check_Plus_Minus_Operation(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, infixExpression, 0, CMTypeRuleCategory.Plus);
			}else if(thisop.equals(InfixExpression.Operator.MINUS)){
				check_Plus_Minus_Operation(CMTypeAnnotatedTypeOne, CMTypeAnnotatedTypeTwo, infixExpression, 0, CMTypeRuleCategory.Subtraction);
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
			if(argumentCMType.equals(CMType.error_propogate) || argumentCMType.equals(CMType.error_source)){
				this.associateCMTypeWithExpression(parenthesizedExpression, CMType.error_propogate);
				return;
			}else{
				this.associateCMTypeWithExpression(parenthesizedExpression, argumentCMType);
			}
		}

		else if(node instanceof PrefixExpression){
			PrefixExpression prefixExp = (PrefixExpression)node;
			PrefixExpression.Operator operator = prefixExp.getOperator();
			String argumentCMType = this.getAnnotatedTypeForExpression(prefixExp.getOperand());
			String prefixExpressionType = argumentCMType;
			if(prefixExpressionType.equals(CMType.TypeLess)){
				this.associateCMTypeWithExpression(prefixExp, prefixExpressionType);
				return;
			}
			String operatorType = "";
			if(operator.equals(PrefixExpression.Operator.MINUS)){
				operatorType = CMTypeRuleCategory.Unary_minus;

			}else if(operator.equals(PrefixExpression.Operator.PLUS)){
				operatorType = CMTypeRuleCategory.Unary_plus;
			}
			String returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, prefixExpressionType, operatorType, CMType.TypeLess);				
			if(returnType != null ){
				this.associateCMTypeWithExpression(prefixExp, returnType);	
			}else{
				this.associateCMTypeWithExpression(prefixExp, prefixExpressionType);
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
				if(!leftCMType.equals(CMType.TypeLess) && !rightCMType.equals(CMType.TypeLess)){
					String returnType = this.cmTypeOperationManager.getReturnType(currentProject, leftCMType, CMTypeRuleCategory.Assignable, rightCMType);
					if(returnType != null){
						return;
					}
				}
				
			    if(!leftCMType.equalsIgnoreCase(CMType.UnknownType) &&
						!leftCMType.equalsIgnoreCase(rightCMType) && 
						!rightCMType.equalsIgnoreCase(CMType.UnknownType) ){
					addNewErrorMessage(node , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);	
				}
			}else if(operator.equals(Assignment.Operator.PLUS_ASSIGN)){
				String returnType = this.cmTypeOperationManager.getReturnType(currentProject, leftCMType, CMTypeRuleCategory.Plus, rightCMType);
				if(returnType != null){
					if(returnType.equals(leftCMType)){
						return;
					}else{
						String assignableType = this.cmTypeOperationManager.getReturnType(currentProject, leftCMType, CMTypeRuleCategory.Assignable, returnType);
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
						this.associateCMTypeWithExpression(leftExp, leftCMType);
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
				if(!leftCMType.equals(CMType.TypeLess) && !rightCMType.equals(CMType.TypeLess)){
					String returnType = this.cmTypeOperationManager.getReturnType(currentProject, leftCMType, CMTypeRuleCategory.Assignable, rightCMType);
					if(returnType != null){
						return;
					}
				}
				//error propagate
				if(rightCMType.equals(CMType.error_propogate) || rightCMType.equals(CMType.error_source)){
					return;
				}
				if(leftCMType.equals(CMType.error_propogate) || leftCMType.equals(CMType.error_source)){
					return;
				}
				else if(!leftCMType.equalsIgnoreCase(CMType.UnknownType) &&
						!leftCMType.equalsIgnoreCase(rightCMType) && 
						!rightCMType.equalsIgnoreCase(CMType.UnknownType) ){
					addNewErrorMessage(node , ErrorUtil.typeInconsistency(leftCMType, rightCMType), DiagnosticMessage.ERROR);	
				}
				//simple inference here
				/*
				if(leftCMType.equals(CMType.TypeLess) && !rightCMType.equals(CMType.UnknownType)){
					if(fragment.getName() instanceof SimpleName){
						IBinding fbinding = ((SimpleName)fragment.getName()).resolveBinding();
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
							this.associateCMTypeWithExpression(fragment.getName(), leftCMType);
						}
					}
				}
				*/
			}
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
					if(argumentTwoAnnotatedType.equalsIgnoreCase(CMType.UnknownType) && argumentOneAnnotatedType.equalsIgnoreCase(CMType.UnknownType)){
						this.associateCMTypeWithExpression(methodInvocationNode, CMType.UnknownType);
						return;
					}
					String returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, argumentOneAnnotatedType, methodName, argumentTwoAnnotatedType); 
					if(returnType != null){
						this.associateCMTypeWithExpression(methodInvocationNode,  returnType);
						return;
					}else {
						addNewErrorMessage(methodInvocationNode , ErrorUtil.getUndeclaredCalculation(methodInvocationNode.toString()),  DiagnosticMessage.WARNING);
						this.associateCMTypeWithExpression(methodInvocationNode, CMType.error_source);
						return;
					}
				
			}
			if(methodInvocationNode.arguments().size() == 1){
				Expression argument = (Expression)(methodInvocationNode.arguments().get(0));
				String argumentAnnotatedType = this.getAnnotatedTypeForExpression(argument);

				if(argumentAnnotatedType.equalsIgnoreCase(CMType.UnknownType)){
					this.associateCMTypeWithExpression(methodInvocationNode, CMType.UnknownType);
					return;
				}
				String returnType = null;
					returnType=this.cmTypeOperationManager.getReturnType(this.currentProject, argumentAnnotatedType, methodName,CMType.TypeLess); 
					if((returnType != null)){
						this.associateCMTypeWithExpression(methodInvocationNode, returnType);
					}else{
						addNewErrorMessage(methodInvocationNode , ErrorUtil.getUndeclaredCalculation(methodInvocationNode.toString()),  DiagnosticMessage.WARNING);
						this.associateCMTypeWithExpression(methodInvocationNode,  CMType.error_source);	
					}
				//abs function
				if(methodName.equalsIgnoreCase(CMTypeRuleCategory.Abosolute_Value)){
					associateCMTypeWithExpression(methodInvocationNode, argumentAnnotatedType);
				}
			}
		}
	}
	
	private void check_Remander_Operation(String CMTypeAnnotatedTypeOne, String CMTypeAnnotatedTypeTwo, InfixExpression infixExpression, int extendedIndex){
		String infixExpressionType = CMType.UnknownType;
			//type rules part
			if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
				infixExpressionType = CMType.UnknownType;
			}		
			else if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
				addNewErrorMessage(infixExpression , ErrorUtil.getRemanderDimensionError(), DiagnosticMessage.ERROR);
				infixExpressionType = CMType.UnknownType;
			}
			else if(!CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
				infixExpressionType = CMTypeAnnotatedTypeOne;
			}else{
				String returnType = null;
				returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, CMTypeRuleCategory.REMAINDER, CMTypeAnnotatedTypeTwo);
				if(returnType != null ){
					infixExpressionType = returnType;
				}else{
					addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
					infixExpressionType = CMType.UnknownType;
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
		
		String infixExpressionType = CMType.UnknownType;
				if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
					infixExpressionType = CMType.UnknownType;
				}		
				else if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
					infixExpressionType = CMTypeAnnotatedTypeTwo;
				}
				else if(!CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
					infixExpressionType = CMTypeAnnotatedTypeOne;
				}else{
					String returnType = null;
					returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, operation_type, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
						//if no match found for the calculations
						if(!CMTypeAnnotatedTypeOne.equalsIgnoreCase(CMType.UnknownType) && !CMTypeAnnotatedTypeTwo.equalsIgnoreCase(CMType.UnknownType)){							
							addNewErrorMessage(plusInfixExpression , ErrorUtil.getUndeclaredCalculation(plusInfixExpression.toString()), DiagnosticMessage.WARNING);
							infixExpressionType = CMType.UnknownType; //change type
							
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
		String infixExpressionType = CMType.UnknownType;
				//type rules part
				if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
					infixExpressionType = CMType.UnknownType;
				}		
				else if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
					infixExpressionType = CMTypeAnnotatedTypeTwo;
				}
				else if(!CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
					infixExpressionType = CMTypeAnnotatedTypeOne;
				}else{
					String returnType = null;
					returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, CMTypeRuleCategory.Multiplication, CMTypeAnnotatedTypeTwo);
					if(returnType != null ){
						infixExpressionType = returnType;
					}else{
						if(!CMTypeAnnotatedTypeOne.equalsIgnoreCase(CMType.UnknownType) && !CMTypeAnnotatedTypeTwo.equalsIgnoreCase(CMType.UnknownType)){							
							addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
							infixExpressionType = CMType.UnknownType;
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
		String infixExpressionType = CMType.UnknownType;
		//type rules part
		if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
			infixExpressionType = CMType.UnknownType;
		}		
		else if(CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && !CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
			String inverseType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeTwo, CMTypeRuleCategory.Multiplicative_Inverse, "");
			if(inverseType != null ){
				infixExpressionType = inverseType;
			}else{
				addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
				infixExpressionType = CMType.UnknownType;
			}
		}
		else if(!CMTypeAnnotatedTypeOne.equals(CMType.UnknownType) && CMTypeAnnotatedTypeTwo.equals(CMType.UnknownType)){
			infixExpressionType = CMTypeAnnotatedTypeOne;
		}else{
			String returnType = null;
			returnType = this.cmTypeOperationManager.getReturnType(this.currentProject, CMTypeAnnotatedTypeOne, CMTypeRuleCategory.Division, CMTypeAnnotatedTypeTwo);
			if(returnType != null ){
				infixExpressionType = returnType;
			}else{
				addNewErrorMessage(infixExpression , ErrorUtil.getUndeclaredCalculation(infixExpression.toString()), DiagnosticMessage.WARNING);
				infixExpressionType = CMType.UnknownType;
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
			annotatedTypeTableForExpression.put(exp, CMType.UnknownType);
			return CMType.UnknownType;
		}
	}
	
	public ArrayList<DiagnosticMessage> getErrorReports() {
		return errorReports;
	}
	@Override
	public void postVisit(ASTNode node) {
		if (node instanceof MethodDeclaration){
			Collections.sort(usedCMTypesInCurrentMethod);
			if(this.usedCMTypesInCurrentMethod.size()>0){
				String usedCMTypesString = usedCMTypesInCurrentMethod.get(0);			
				for(int i=1;i<usedCMTypesInCurrentMethod.size();i++){
					usedCMTypesString = usedCMTypesString + "," + usedCMTypesInCurrentMethod.get(i);
				}
				usedCMTypesList.add(usedCMTypesString);
			}
			currentAccessMethodKey = null;
		}
		if (node instanceof TypeDeclaration){
			currentAccessClassKey = null;
		}
		if ( node instanceof CompilationUnit){
			File file = CMModelUtil.getCMTypePatternFile(currentProject, currentFile.getName());
			try {
				file.createNewFile();
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				for (int i=0;i<usedCMTypesList.size();i++){
					bw.write(usedCMTypesList.get(i)+"\n");
				}
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
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

	private static void addNewErrorMessage(ASTNode node, String errorMessageDetail, String errorType){
		DiagnosticMessage errorMessage = new DiagnosticMessage();
		errorMessage.setMessageType(errorType);
		errorMessage.setMessageDetail(errorMessageDetail);
		errorMessage.setContextInfo("");
		errorMessage.setErrorNode(node);
		errorReports.add(errorMessage);
	}
	
	
	private void associateCMTypeWithExpression(Expression exp, String annotatedType){
		if(annotatedType != null){
			CMType cmtype = CMModelUtil.getCMTypeFromTypeName(currentProject, annotatedType);
			if(cmtype!=null){
				if(!NewTypeCheckerVisitor.cmtypeHashSet.contains(annotatedType)){
					NewTypeCheckerVisitor.cmtypeHashSet.add(annotatedType);
				}
				this.annotatedTypeTableForExpression.put(exp, cmtype.getEnabledAttributeSet());
			}else{
				this.annotatedTypeTableForExpression.put(exp, annotatedType);	
			}
			if(!annotatedType.equals(CMType.UnknownType)){
				if(!this.usedCMTypesInCurrentMethod.contains(annotatedType)){
					usedCMTypesInCurrentMethod.add(annotatedType);
				}
			}
		}
	}
	
	public static boolean checkConsistency(String setA, String setB){
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
	
	public static String uniteTwoSets(String setA, String setB, CMTypeRulesManager manager){
		if(setA.length()==0){
			return setB;
		}
		if(setB.length()==0){
			return setA;
		}
		
		String returnType = manager.getReturnType(currentProject, setA, CMTypeRuleCategory.setUnion, setB);
		if(returnType!=null){
			return returnType;
		}else{
			String unitSet = setA + ";" + setB;
			String[] unitedSetArray = unitSet.split(";");
			ArrayList<String> setList = new ArrayList<String>();
			String basicInfo = "";
			for(String unitedSet: unitedSetArray){
				if(unitedSet.startsWith("basic")){
					basicInfo = unitedSet;
					continue;
				}
				setList.add(unitedSet);
			}

			if(basicInfo.length()>0){
				setList.add(basicInfo);	
			}
			Collections.sort(setList);
			String result = setList.get(0);
			for(int i=1;i<setList.size();i++){
				result = result+";"+setList.get(i);
			}
			return result;
		}
		
	}

	
}