package cmtypechecker.popup.actions;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import cmtypechecker.CMRules.CMTypeRulesManager;
import cmtypechecker.typechecker.CandidateRuleVisitor2;
import cmtypechecker.typechecker.CommentVisitor;
import cmtypechecker.typechecker.NewTypeCheckerVisitor;
import cmtypechecker.typechecker.TypeCheckingVisitor;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.util.DiagnosticMessage;

public class UnitsCheckingAllFilesInNavigator implements IObjectActionDelegate {
	
	private ISelection selection;
	private Shell shell;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public UnitsCheckingAllFilesInNavigator() {
		super();
	}
	
	public void run(IAction action) {
		IStructuredSelection selection = (IStructuredSelection)(this.selection);
		IProject iProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
		File outputFile = new File("unitsCheckingResults.csv");
		if(outputFile.exists()){
			System.out.println(outputFile.getAbsolutePath());
			outputFile.delete();
		}
		if (iProject == null) 
			return;
		BufferedWriter out= null;
		try {
			out = new BufferedWriter(new FileWriter(outputFile));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		long beforeTime = System.currentTimeMillis();
		TypeCheckingVisitor.cmtypeHashSet.clear();
		
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
            File ruleFolder = CMModelUtil.getRWTypeRulesFiles(iProject);
			CMTypeRulesManager manager = new CMTypeRulesManager(ruleFolder);
            CMTypeRulesManager.ruleSet.clear();
    		out.append("file name"+","+"annotation count"+","+"errors"+","+"warnings"+","+"variables involved"+","+"access rwt"+"\n");
        	for(IResource javaSource:javaSourceFiles){
        		ASTParser parser = ASTParser.newParser(AST.JLS3);
        		parser.setKind(ASTParser.K_COMPILATION_UNIT);
        		IWorkspace workspace= ResourcesPlugin.getWorkspace();    
        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
        		if(sourceFile.getName().contains("DayNightLayer")){
        			System.out.println("pause");
        		}
        		if(sourceFile.exists()){
        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        			parser.setSource(icompilationUnit); // set source
        			parser.setResolveBindings(true); // we need bindings later on
        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
        			//units checking
					TypeCheckingVisitor typeCheckingVisitor = new TypeCheckingVisitor(manager, compilationResult, true);
					System.out.println("error checking file: "+javaSource.getName());
        			compilationResult.accept(typeCheckingVisitor);
        			ArrayList<DiagnosticMessage> CMTypeCheckingResults = typeCheckingVisitor.getErrorReports();
        			int warningCount = 0;
        			int errorCount = 0;
        			for(DiagnosticMessage dm : CMTypeCheckingResults){
        				if(dm.getMessageType().equals(DiagnosticMessage.ERROR)){
        					errorCount++;
        				}else{
        					warningCount++;
        				}
        			}
        			out.append(javaSource.getName()+","
        					+String.valueOf(typeCheckingVisitor.getAnnotatedCount())+","
        					+errorCount+","
        					+warningCount+","
        					+typeCheckingVisitor.getVariableCourt()+","
        					+typeCheckingVisitor.isAccessRWT()+","
        					+"\n");
        		}
        		System.out.println("The rules have been used in the checking process: "+CMTypeRulesManager.ruleSet.size());
        		System.out.println("The total number of rules is: "+manager.getDefinedOperations().size());
        	}
            out.close();
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
