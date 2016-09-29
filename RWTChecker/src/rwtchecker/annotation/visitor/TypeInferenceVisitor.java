package rwtchecker.annotation.visitor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.dom.*;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.annotation.RWTAnnotation;
import rwtchecker.util.CMModelUtil;
import rwtchecker.views.RWTView;

public class TypeInferenceVisitor extends ASTVisitor {
	
	private CompilationUnit compilationUnit;
	
	private Map<String, Map<String, String>> allVariableMap = new HashMap<String, Map<String, String>>();
	private Map<String, String> methodReturnMap = new HashMap<String, String>();
	
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	
	private IPath currentFilePath;
	private IFile currentFile;
	
	public TypeInferenceVisitor(CompilationUnit compilationUnit) {
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
	
	protected String getRWTypeForVarLikeExp(Expression exp){
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