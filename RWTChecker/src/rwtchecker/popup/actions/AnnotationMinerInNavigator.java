package rwtchecker.popup.actions;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import rwtchecker.annotation.AnnotationLearner;
import rwtchecker.typechecker.AnnotationPatternMinerVisitor;

public class AnnotationMinerInNavigator implements IObjectActionDelegate {
	
	private ISelection selection;
	private Shell shell;
	
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public AnnotationMinerInNavigator() {
		super();
	}
	
	public void run(IAction action) {
		IStructuredSelection selection = (IStructuredSelection)(this.selection);
		IProject iProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
		
		if (iProject == null) 
			return;
		
	    List<IJavaElement> ret = new ArrayList<IJavaElement>();
	    IJavaProject javaProject = JavaCore.create(iProject);
	    try {
            IPackageFragmentRoot[] packageFragmentRoot = javaProject.getAllPackageFragmentRoots();
            ArrayList<IResource> javaSourceFiles = new ArrayList<IResource>();
            for (int i = 0; i < packageFragmentRoot.length; i++){
                if (packageFragmentRoot[i].getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT && !packageFragmentRoot[i].isArchive())
                {
                	ret.add(packageFragmentRoot[i]);
                	IResource folder = packageFragmentRoot[i].getResource();
                	if(folder instanceof IContainer){
                    	findAllJavaFiles((IContainer)folder, javaSourceFiles);                		
                	}
                }
            }
        	for(IResource javaSource:javaSourceFiles){
        		IWorkspace workspace= ResourcesPlugin.getWorkspace();    
//        		IPath location= Path.fromOSString(file.getAbsolutePath()); 
        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
        		if(sourceFile.exists()){
        			ASTParser parser = ASTParser.newParser(AST.JLS3);
        			parser.setKind(ASTParser.K_COMPILATION_UNIT);
        			if(sourceFile.exists()){
        				ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        				parser.setSource(icompilationUnit); // set source
        				parser.setResolveBindings(true); // we need bindings later on
        				CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
        				AnnotationPatternMinerVisitor annotationPatternMinerVisitor = new AnnotationPatternMinerVisitor(compilationResult);
        				compilationResult.accept(annotationPatternMinerVisitor);
        			}
        		}
        	}
        	AnnotationLearner learner = AnnotationLearner.getInstance();
        	learner.outputData("e:\\annotation_patterns.txt");
        } catch (JavaModelException e) {
            e.printStackTrace();
		}
	}
	
	private void findAllJavaFiles(IContainer folder, ArrayList<IResource> allJavaSourcesFiles){
		IResource[] resources;
		try {
			resources = folder.members();
			for(IResource resource: resources){
				if(resource.getName().endsWith(".java")){
					allJavaSourcesFiles.add(resource);
				}
	        	if(resource instanceof IContainer){
	        		findAllJavaFiles((IContainer)resource, allJavaSourcesFiles);
	        	}
	        }
		} catch (CoreException e) {
			e.printStackTrace();
		}
        
	}
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
	
}
