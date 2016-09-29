package rwtchecker.popup.actions;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import rwtchecker.popup.actions.inference.TypePropagationVisitor;


public class TypePropagationPackageInNavigator implements IObjectActionDelegate {
	
	private ISelection selection;
	
	public TypePropagationPackageInNavigator(){
		super();
	}
	
	@Override
	public void run(IAction arg0) {
		//if(paramPropagationMap.size()>0){
			IStructuredSelection selection = (IStructuredSelection)(this.selection);
			IPackageFragment iPackage = (IPackageFragment) ((IStructuredSelection) selection).getFirstElement();
			if (iPackage == null) {
				return;
			}else{
				try {
					IJavaElement[] javaElements = iPackage.getChildren();
					for(IJavaElement javaElement : javaElements){
						IResource javaSource = javaElement.getResource();
						ASTParser parser = ASTParser.newParser(AST.JLS3);
		        		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		        		IWorkspace workspace= ResourcesPlugin.getWorkspace();    
		        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
		        		if(sourceFile.exists()){
		        			System.out.println("Type Propagation for file: "+ sourceFile.getName());
		        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
		        			parser.setSource(icompilationUnit); // set source
		        			parser.setResolveBindings(true); // we need bindings later on
		        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
		        			TypePropagationVisitor typePropagationVisitor = new TypePropagationVisitor(compilationResult);
		        			compilationResult.accept(typePropagationVisitor);
		        		}
					}
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Type Propagation ends here.");
			}
				
		//}
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		
	}
	
}

