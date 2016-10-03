package rwtchecker.popup.actions.inference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import rwtchecker.annotation.visitor.TypeInferenceReturnValueToMethodVisitor;
import rwtchecker.annotation.visitor.TypeInferenceVisitor;
import rwtchecker.typechecker.ExtractorVisitor;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;


public class TypeInferenceReturnValueToMethod implements IObjectActionDelegate {
	
	private ISelection selection;

	public TypeInferenceReturnValueToMethod(){
		super();
	}


	@Override
	public void run(IAction arg0) {
		IStructuredSelection selection = (IStructuredSelection)(this.selection);
		IProject iProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
		if (iProject == null) {
			return;
		}else{
			IJavaProject javaProject = JavaCore.create(iProject);
			ArrayList<IResource> javaSourceFiles = RWTSystemUtil.getAllJavaSourceFiles(javaProject);
			for(IResource javaSource:javaSourceFiles){
        		ASTParser parser = ASTParser.newParser(AST.JLS3);
        		parser.setKind(ASTParser.K_COMPILATION_UNIT);
        		IWorkspace workspace= ResourcesPlugin.getWorkspace();    
        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
        		if(sourceFile.exists()){
        			System.out.println("Type Propagation from return values to method signitures  for file: "+ sourceFile.getName());
        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        			parser.setSource(icompilationUnit); // set source
        			parser.setResolveBindings(true); // we need bindings later on
        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
        			TypeInferenceReturnValueToMethodVisitor typePropagationVisitor = new TypeInferenceReturnValueToMethodVisitor(compilationResult);
        			compilationResult.accept(typePropagationVisitor);
        		}
			}
			System.out.println("Type Propagation from return values to method signitures ends here.");
		}
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		
	}
	
}

