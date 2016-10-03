package rwtchecker.typechecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;

public class RevisionVisitor extends ASTVisitor{
	
	private CompilationUnit compilationUnit;
	private String[] source;
	private StringBuffer revisedSource = new StringBuffer();
	private FileAnnotations fileAnnotations;
	private int lastUnReadLine = 0;
	
	public RevisionVisitor(CompilationUnit compilationUnit, FileAnnotations fileAnnotations, String[] source) {
        super();
        this.source = source;
        this.compilationUnit = compilationUnit;
        this.fileAnnotations = fileAnnotations;
	}
	
	public boolean visit( VariableDeclarationStatement variableDeclarationStatement){
		int startingLineNumber = compilationUnit.getLineNumber(variableDeclarationStatement.getStartPosition()) -1;
		int endLineNumber = compilationUnit.getLineNumber(variableDeclarationStatement.getStartPosition() + variableDeclarationStatement.getLength()) - 1;
		Map<String, String> nameToType = new HashMap<String, String>();
		String initilizierString = 	null;
		for (Iterator iter = variableDeclarationStatement.fragments().iterator(); iter.hasNext();){
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
			String variableName = fragment.getName().getIdentifier();
			IVariableBinding variableBinding = fragment.resolveBinding();
			String variableCMType = null;
			if(variableBinding.isField()){
				String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
				if(variableBinding.getJavaElement()==null){
					continue;
				}
				String classDeclPath = variableBinding.getJavaElement().getPath().toString();
				String classDeclKey = variableBinding.getDeclaringClass().getKey();
				if(currentUnitPath.equals(classDeclPath)){
					variableCMType = fileAnnotations.getCMTypeInBodyDecl(classDeclKey, variableName);
				}else{
					IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(variableBinding.getJavaElement().getPath());
					File otherSourceFileAnnotationFile = RWTSystemUtil.getAnnotationFile(ifile);
					if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
						FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
						if(otherSourcefileAnnotation == null){
							continue;
						}
						variableCMType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
					}
				}						
			}else{
				String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
				variableCMType = fileAnnotations.getCMTypeInBodyDecl(methodDeclKey, variableName);
			}
			if(variableCMType!=null){
				nameToType.put(variableName, variableCMType);
			}
			if(fragment.getInitializer()!=null){
				initilizierString = fragment.getInitializer().toString();
			}
		}
		if(lastUnReadLine<=startingLineNumber){
			for(int i=lastUnReadLine;i<startingLineNumber;i++){
				this.revisedSource.append(source[i]+"\n"); 
			}
		}
		//proceed only if contains annotations
		if(nameToType.size()>0){
			String wholeStatementString = source[startingLineNumber];
			for(int i=startingLineNumber+1;i<endLineNumber;i++){
				wholeStatementString = wholeStatementString + source[i];
			}
			if(!wholeStatementString.contains(variableDeclarationStatement.toString())){
				//error here
				return true;
			}
			Set<String> keyset = nameToType.keySet();
			int index = wholeStatementString.indexOf(variableDeclarationStatement.toString());
			for(String key: keyset){
				String contentsBefore = wholeStatementString.substring(0, index);
				this.revisedSource.append(contentsBefore+"\n");
				List<IExtendedModifier> modifiers = variableDeclarationStatement.modifiers();
				String thisFragmentStatement = "";
				String modifierString = "";
				for (IExtendedModifier modifier: modifiers){
					modifierString = modifierString + " " + modifier.toString();
				}
				if(initilizierString!=null){
					thisFragmentStatement = modifierString + " "+nameToType.get(key)+" "+key+" = " + initilizierString + ";";
				}else{
					thisFragmentStatement = modifierString + " "+nameToType.get(key)+" "+ key + ";";
				}
				this.revisedSource.append(thisFragmentStatement + "\n");
			}
			String contentsAfter = wholeStatementString.substring(index, index+variableDeclarationStatement.toString().length());
			this.revisedSource.append(contentsAfter + "\n");
		}else{
			for(int i=startingLineNumber;i<=endLineNumber;i++){
				this.revisedSource.append(source[i]+"\n");
			}
		}
		lastUnReadLine = endLineNumber+1;
		return true;
	}
	
	
	
	public boolean visit( SingleVariableDeclaration SingleVariableDeclaration){
		int startingLineNumber = compilationUnit.getLineNumber(SingleVariableDeclaration.getStartPosition()) -1;
		int endLineNumber = compilationUnit.getLineNumber(SingleVariableDeclaration.getStartPosition() + SingleVariableDeclaration.getLength()) - 1;
		if(lastUnReadLine<=startingLineNumber){
			for(int i=lastUnReadLine;i<startingLineNumber;i++){
				this.revisedSource.append(source[i]+"\n"); 
			}
		}
		String variableName = SingleVariableDeclaration.getName().getIdentifier();
		IVariableBinding variableBinding = (IVariableBinding)(SingleVariableDeclaration.getName().resolveBinding());
		String variableCMType = null;
		if(variableBinding.isField()){
			String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
			if(variableBinding.getJavaElement()==null){
				return true;
			}
			String classDeclPath = variableBinding.getJavaElement().getPath().toString();
			String classDeclKey = variableBinding.getDeclaringClass().getKey();
			if(currentUnitPath.equals(classDeclPath)){
				variableCMType = fileAnnotations.getCMTypeInBodyDecl(classDeclKey, variableName);
			}else{
				IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(variableBinding.getJavaElement().getPath());
				File otherSourceFileAnnotationFile = RWTSystemUtil.getAnnotationFile(ifile);
				if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
					FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
					if(otherSourcefileAnnotation == null){
						return true;
					}
					variableCMType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
				}
			}						
		}else{
			String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
			variableCMType = fileAnnotations.getCMTypeInBodyDecl(methodDeclKey, variableName);
		}
		if(variableCMType!=null){
			String wholeStatementString = source[startingLineNumber];
			for(int i=startingLineNumber+1;i<endLineNumber;i++){
				wholeStatementString = wholeStatementString + source[i];
			}
			if(!wholeStatementString.contains(SingleVariableDeclaration.toString())){
				//error here
				return true;
			}
			int index = wholeStatementString.indexOf(SingleVariableDeclaration.toString());
			String contentsBefore = wholeStatementString.substring(0, index);
			this.revisedSource.append(contentsBefore+"\n");
			List<IExtendedModifier> modifiers = SingleVariableDeclaration.modifiers();
			String thisFragmentStatement = "";
			String modifierString = "";
			for (IExtendedModifier modifier: modifiers){
				modifierString = modifierString + " " + modifier.toString();
			}
			if( SingleVariableDeclaration.getInitializer()!=null){
				thisFragmentStatement = modifierString + " "+variableCMType+" "+variableName+" = " + SingleVariableDeclaration.getInitializer().toString() + ";";
			}else{
				thisFragmentStatement = modifierString + " "+variableCMType+" "+ variableName + ";";
			}
			this.revisedSource.append(thisFragmentStatement + "\n");
			String contentsAfter = wholeStatementString.substring(index, index+SingleVariableDeclaration.toString().length());
			this.revisedSource.append(contentsAfter + "\n");
		}else{
			for(int i=startingLineNumber;i<=endLineNumber;i++){
				this.revisedSource.append(source[i]+"\n");
			}
		}
		lastUnReadLine = endLineNumber+1;
		return true;
	}

	public boolean visit( FieldDeclaration fieldDeclaration){
		int startingLineNumber = compilationUnit.getLineNumber(fieldDeclaration.getStartPosition()) -1;
		int endLineNumber = compilationUnit.getLineNumber(fieldDeclaration.getStartPosition() + fieldDeclaration.getLength()) - 1;
		Map<String, String> nameToType = new HashMap<String, String>();
		String initilizierString = 	null;
		for (Iterator iter = fieldDeclaration.fragments().iterator(); iter.hasNext();){
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
			String variableName = fragment.getName().getIdentifier();
			IVariableBinding variableBinding = fragment.resolveBinding();
			String variableCMType = null;
			if(variableBinding.isField()){
				String currentUnitPath = this.compilationUnit.getJavaElement().getPath().toString();
				if(variableBinding.getJavaElement()==null){
					continue;
				}
				String classDeclPath = variableBinding.getJavaElement().getPath().toString();
				String classDeclKey = variableBinding.getDeclaringClass().getKey();
				if(currentUnitPath.equals(classDeclPath)){
					variableCMType = fileAnnotations.getCMTypeInBodyDecl(classDeclKey, variableName);
				}else{
					IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(variableBinding.getJavaElement().getPath());
					File otherSourceFileAnnotationFile = RWTSystemUtil.getAnnotationFile(ifile);
					if(otherSourceFileAnnotationFile!= null && otherSourceFileAnnotationFile.exists()){
						FileAnnotations otherSourcefileAnnotation = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
						if(otherSourcefileAnnotation == null){
							continue;
						}
						variableCMType = otherSourcefileAnnotation.getCMTypeInBodyDecl(classDeclKey, variableBinding.getName());
					}
				}						
			}else{
				String methodDeclKey = variableBinding.getDeclaringMethod().getKey();
				variableCMType = fileAnnotations.getCMTypeInBodyDecl(methodDeclKey, variableName);
			}
			if(variableCMType!=null){
				nameToType.put(variableName, variableCMType);
			}
			if(fragment.getInitializer()!=null){
				initilizierString = fragment.getInitializer().toString();
			}

		}
		if(lastUnReadLine<=startingLineNumber){
			for(int i=lastUnReadLine;i<startingLineNumber;i++){
				this.revisedSource.append(source[i]+"\n"); 
			}
		}
		//proceed only if contains annotations
		if(nameToType.size()>0){
			String wholeStatementString = source[startingLineNumber];
			for(int i=startingLineNumber+1;i<endLineNumber;i++){
				wholeStatementString = wholeStatementString + source[i];
			}
			if(!wholeStatementString.contains(fieldDeclaration.toString())){
				//error here
				return true;
			}
			Set<String> keyset = nameToType.keySet();
			int index = wholeStatementString.indexOf(fieldDeclaration.toString());
			for(String key: keyset){
				String contentsBefore = wholeStatementString.substring(0, index);
				this.revisedSource.append(contentsBefore+"\n");
				List<IExtendedModifier> modifiers = fieldDeclaration.modifiers();
				String thisFragmentStatement = "";
				String modifierString = "";
				for (IExtendedModifier modifier: modifiers){
					modifierString = modifierString + " " + modifier.toString();
				}
				if(initilizierString!=null){
					thisFragmentStatement = modifierString + " "+nameToType.get(key)+" "+key+" = " + initilizierString + ";";
				}else{
					thisFragmentStatement = modifierString + " "+nameToType.get(key)+" "+ key + ";";
				}
				this.revisedSource.append(thisFragmentStatement + "\n");
			}
			String contentsAfter = wholeStatementString.substring(index, index+fieldDeclaration.toString().length());
			this.revisedSource.append(contentsAfter + "\n");
			
		}else{
			for(int i=startingLineNumber;i<=endLineNumber;i++){
				this.revisedSource.append(source[i]+"\n");
			}
		}
		lastUnReadLine = endLineNumber+1;
		return true;
	}
	
	public int getLastUnReadLine() {
		return lastUnReadLine;
	}

	public StringBuffer getRevisedSource() {
		return revisedSource;
	}
}
