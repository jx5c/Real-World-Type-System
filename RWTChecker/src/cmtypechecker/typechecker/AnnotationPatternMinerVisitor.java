package cmtypechecker.typechecker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.dom.*;

import cmtypechecker.CM.CMType;
import cmtypechecker.annotation.AnnotationLearner;
import cmtypechecker.annotation.FileAnnotations;
import cmtypechecker.util.CMModelUtil;

public class AnnotationPatternMinerVisitor extends ASTVisitor {
	
	private CompilationUnit compilationUnit;
	
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	private Queue<String> current_itemset = new LinkedList<String>();
	
	private ArrayList<String> itemsets_lhv = new ArrayList<String>();
	private ArrayList<String> itemsets_rhv = new ArrayList<String>();
	private ArrayList<String> statement_list = new ArrayList<String>();
	
	private boolean mining = false;
	private boolean hasRWType = false;
	
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	
	private IPath currentFilePath;
	private IFile currentFile;
	public AnnotationPatternMinerVisitor(CompilationUnit compilationUnit) {
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
	
	public boolean visit(MethodInvocation mi){
		IMethodBinding iMethodBinding = mi.resolveMethodBinding();
		String rwtype = FileAnnotations.getRWTypeForMethod(iMethodBinding);
		if(rwtype.length()>0){
			current_itemset.add(rwtype);
			hasRWType = true;
			return false;
		}
		else{
			current_itemset.add(iMethodBinding.getKey());	
		}
		return true;
	}
	
	public void preVisit(ASTNode node){
		if(node instanceof Assignment || node instanceof VariableDeclarationStatement){
			mining = true;
			hasRWType = false;
			current_itemset.clear();
		}
	}

	public void postVisit(ASTNode node){
		if(mining){
			if (node instanceof NumberLiteral){
				current_itemset.add(((NumberLiteral)node).toString());
			}
			if(node instanceof InfixExpression){
				current_itemset.add(((InfixExpression)node).getOperator().toString());
			}
			/*
			if(node instanceof MethodInvocation){
				IMethodBinding iMethodBinding = ((MethodInvocation)node).resolveMethodBinding();
				String rwtype = FileAnnotations.getRWTypeForMethod(iMethodBinding);
				if(rwtype.length()>0){
					current_itemset.add(rwtype);
					hasRWType = true;
				}
				else{
					current_itemset.add(iMethodBinding.getKey());	
				}
			}
			*/
			if(node instanceof SimpleName){
				IBinding binding= ((SimpleName)node).resolveBinding();
				if (binding.getKind() == IBinding.VARIABLE) {
					String rwtype = getRWTypeForVarLikeExp((SimpleName)node);
					if(rwtype.length()>0&& !rwtype.equals(CMType.GenericMethod)){
						current_itemset.add(rwtype);
						hasRWType = true;
					}else{
						current_itemset.add("var");
					}
				}
			}
			if(node instanceof Assignment){
				if(hasRWType){
					if(current_itemset.size()>=2){
						itemsets_lhv.add(current_itemset.poll());
						StringBuffer temp = new StringBuffer();
						for (String item : current_itemset){
							temp.append(item);
							//separator
							temp.append("&&");
						}
						itemsets_rhv.add(temp.toString());
						statement_list.add(node.toString());
					}	
				}
				mining = false;
				hasRWType = false;
			}
			
			if(node instanceof VariableDeclarationStatement){
				if(hasRWType){
					if(current_itemset.size()>=2){
						VariableDeclarationStatement vDeclSt = (VariableDeclarationStatement)node;
						for (Iterator iter = vDeclSt.fragments().iterator(); iter.hasNext();iter.next()) {
							itemsets_lhv.add(current_itemset.poll());
						}
						StringBuffer temp = new StringBuffer();
						for (String item : current_itemset){
							temp.append(item);
							//separator
							temp.append("&&");
						}
						for(int i=0;i<vDeclSt.fragments().size();i++){
							itemsets_rhv.add(temp.toString());
							statement_list.add(node.toString().trim());
						}
						
					}	
				}
				mining = false;
				hasRWType = false;
			}
		}
		if(node instanceof CompilationUnit){
			AnnotationLearner learner = AnnotationLearner.getInstance();
//			if(statement_list.size()!=itemsets_rhv.size()){
//				System.out.println("error here, debug me");
//			}
//			for(String i:statement_list){
//				System.out.println("Debug: " + i);
//			}
			learner.addData(itemsets_lhv, itemsets_rhv, statement_list);
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
		if(thisRWType ==null){
			return "";
		}else{
			return thisRWType;
		}
	}
	
}