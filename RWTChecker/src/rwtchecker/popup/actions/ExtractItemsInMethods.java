package rwtchecker.popup.actions;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.actions.FoldingMessages;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import rwtchecker.CMRules.CMTypeRulesManager;
import rwtchecker.extractor.Extractor;
import rwtchecker.typechecker.ExtractionMethodItemsVisitor;

public class ExtractItemsInMethods implements IObjectActionDelegate {
	
	private ISelection selection;
	private Shell shell;

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public ExtractItemsInMethods() {
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
//                	System.out.println(javaSourceFiles.size());	 
                }
            }
            
            System.out.println("we have in total "+javaSourceFiles.size()+" files");
            
        	for(IResource javaSource:javaSourceFiles){
        		ASTParser parser = ASTParser.newParser(AST.JLS3);
        		parser.setKind(ASTParser.K_COMPILATION_UNIT);
        		IWorkspace workspace= ResourcesPlugin.getWorkspace();    
        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
        		if(sourceFile.exists()){
        			System.out.println("extracting terms in methods from class: "+ sourceFile.getName());
        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        			parser.setSource(icompilationUnit); // set source
        			parser.setResolveBindings(true); // we need bindings later on
        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
        			ExtractionMethodItemsVisitor.callerDepth = 0;
        			ExtractionMethodItemsVisitor extractionMethodItemsVisitor = new ExtractionMethodItemsVisitor();
        			compilationResult.accept(extractionMethodItemsVisitor);
        		}
        	}
        	
        	System.out.println("extracting method itemsets ends, Start printing");
    		BufferedWriter methodItemsetOut= null;
    		try {
    			methodItemsetOut = new BufferedWriter(new FileWriter("e:\\methodItemsets.txt"));
    		} catch (IOException e1) {
    			e1.printStackTrace();
    		}
    		Map<String, HashSet<String>> methodKeyItemsets= ExtractionMethodItemsVisitor.methodKeyItemsets;
    		Iterator methodKeyItemsetsKeys = methodKeyItemsets.keySet().iterator();
    		while(methodKeyItemsetsKeys.hasNext()){
    			String methodKey = methodKeyItemsetsKeys.next().toString();
    			methodItemsetOut.append(methodKey+"\n");
    			methodItemsetOut.newLine();	
    			HashSet<String> itemsets = ExtractionMethodItemsVisitor.methodKeyItemsets.get(methodKey);
    			for(String itemset : itemsets){
    				methodItemsetOut.append(itemset+",");
        			
    			}
    			methodItemsetOut.newLine();	
    		}
    		methodItemsetOut.close();
            System.out.println("Printing Ends");
            
            System.out.println("Start printing terms in method names");
            BufferedWriter out= null;
    		try {
    			out = new BufferedWriter(new FileWriter("e:\\allExtractedTerms.txt"));
    		} catch (IOException e1) {
    			e1.printStackTrace();
    		}
    		Iterator keys = Extractor.termFreqMap.keySet().iterator();
    		while(keys.hasNext()){
    			String termName = keys.next().toString();
    			int freq = Extractor.termFreqMap.get(termName).get(termName);
    			out.append(termName+","+String.valueOf(freq)+"\n");
    			out.newLine();
    		}
            out.close();
            System.out.println("Printing Ends");
            
        	
        } catch (JavaModelException e) {
            e.printStackTrace();
		} catch (IOException e) {
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
