package rwtchecker.popup.actions.inference;

import java.util.ArrayList;

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

import rwtchecker.annotation.visitor.TypeInferenceAssignmentsVisitor;
import rwtchecker.util.RWTSystemUtil;


public class TypeInferenceAssignments implements IObjectActionDelegate {
	
	private ISelection selection;

	public TypeInferenceAssignments(){
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
        			System.out.println("Type Propagation for Assignment in file: "+ sourceFile.getName());
        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        			parser.setSource(icompilationUnit); // set source
        			parser.setResolveBindings(true); // we need bindings later on
        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
        			TypeInferenceAssignmentsVisitor typePropagationVisitor = new TypeInferenceAssignmentsVisitor(compilationResult);
        			compilationResult.accept(typePropagationVisitor);
        		}
			}
			System.out.println("Type Propagation for Assignments here.");
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

