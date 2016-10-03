package rwtchecker.popup.actions;


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

import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.typechecker.CandidateRuleVisitor2;
import rwtchecker.typechecker.CommentVisitor;
import rwtchecker.typechecker.NewTypeCheckerVisitor;
import rwtchecker.typechecker.TypeCheckingVisitor;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.DiagnosticMessage;

public class TypeCheckingMultiplyFilesInNavigator implements IObjectActionDelegate {
	
	private ISelection selection;
	private Shell shell;

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public TypeCheckingMultiplyFilesInNavigator() {
		super();
	}
	
	public void run(IAction action) {
		IStructuredSelection selection = (IStructuredSelection)(this.selection);
		IProject iProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
		File outputFile = new File("checkingResults.csv");
		if(outputFile.exists()){
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
		NewTypeCheckerVisitor.cmtypeHashSet.clear();
		
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
            File ruleFolder = RWTSystemUtil.getRWTypeRulesFiles(iProject);
			RWTypeRulesManager manager = new RWTypeRulesManager(ruleFolder);
            RWTypeRulesManager.ruleSet.clear();
        	for(IResource javaSource:javaSourceFiles){
        		int annotationCourt = 0;
        		ASTParser parser = ASTParser.newParser(AST.JLS3);
        		parser.setKind(ASTParser.K_COMPILATION_UNIT);
        		IWorkspace workspace= ResourcesPlugin.getWorkspace();    
//        		IPath location= Path.fromOSString(file.getAbsolutePath()); 
        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
        		if(sourceFile.exists()){
        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        			parser.setSource(icompilationUnit); // set source
        			parser.setResolveBindings(true); // we need bindings later on
        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);

        			//type checking choice
        			int checkingVisitorChoice = 1; // 0 for comment based visitor or 1 for javaDoc based visitor
        			switch (checkingVisitorChoice){
        				case 0:
        					BufferedReader infile = null;
                			ArrayList<String> contents = new ArrayList<String>();
                			String line = "";
                			try {
                				infile = new BufferedReader(new FileReader(sourceFile.getRawLocation().toFile()));
                				while((line = infile.readLine())!= null){
                					if(line.contains("*cm")){
                						annotationCourt ++;
                					}
                					contents.add(line);
                				}
                			} catch (IOException e) {
                				e.printStackTrace();
                			}
                			String[] sourceList = contents.toArray(new String[contents.size()]);
                			
                			Map<Integer, ArrayList<String>> varsCommentsMap = new HashMap<Integer, ArrayList<String>>();
                			Map<Integer, ArrayList<String>> funcCommentsMap = new HashMap<Integer, ArrayList<String>>();
                			
                			for (Comment comment : (List<Comment>) compilationResult.getCommentList()) {
                				CommentVisitor thisCommentVisitor = new CommentVisitor(compilationResult, sourceList);
                	            comment.accept(thisCommentVisitor);
                	            if(thisCommentVisitor.isDefComment()){
                	            	varsCommentsMap.put(thisCommentVisitor.getLineCourt(), thisCommentVisitor.getCommentConents());
                	            }
                	            if(thisCommentVisitor.isFuncComment()){
                	            	funcCommentsMap.put(thisCommentVisitor.getLineCourt(), thisCommentVisitor.getCommentConents());
                	            }
                	        }
                			
                			NewTypeCheckerVisitor newTypeCheckingVisitor = new NewTypeCheckerVisitor(manager, compilationResult, varsCommentsMap, funcCommentsMap);
                			System.out.println("error checking file: "+javaSource.getName());
                			compilationResult.accept(newTypeCheckingVisitor);
                			ArrayList<DiagnosticMessage> newCMTypeCheckingResults = newTypeCheckingVisitor.getErrorReports();
                			
                			CandidateRuleVisitor2 candidateRuleVisitor = new CandidateRuleVisitor2(manager, compilationResult, varsCommentsMap, funcCommentsMap);
                			compilationResult.accept(candidateRuleVisitor);
                			out.append(javaSource.getName()+","+String.valueOf(annotationCourt)+","+newCMTypeCheckingResults.size()+","+newTypeCheckingVisitor.getVariableCourt()+"\n");
        					break;
        				case 1:
        					TypeCheckingVisitor typeCheckingVisitor = new TypeCheckingVisitor(manager, compilationResult, false);
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
                			out.append(javaSource.getName()+","+String.valueOf(typeCheckingVisitor.getAnnotatedCount())+","+errorCount+","+warningCount+","+typeCheckingVisitor.getVariableCourt()+"\n");
        					break;
        			}
        		}
        		System.out.println("The rules have been used in the checking process: "+RWTypeRulesManager.ruleSet.size());
        		System.out.println("The total number of rules is: "+manager.getDefinedOperations().size());
        	}
            out.close();
        } catch (JavaModelException e) {
            e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    /*
		BufferedWriter out2;
		try {
			out2 = new BufferedWriter(new FileWriter("e://ruleCount.txt"));
			for(int i:CandidateRuleVisitor2.ruleCounts){
				out2.append(String.valueOf(i));
				out2.newLine();
			}
			out2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
       
		long afterTime = System.currentTimeMillis();
		System.out.println("The total time needed is about " + (afterTime -beforeTime));
		System.out.println("The total cm type used is about "+TypeCheckingVisitor.cmtypeHashSet.size());
		
		try {
			BufferedWriter cmtypesFile = new BufferedWriter(new FileWriter("used_cmtypes.txt"));
			for(String i : TypeCheckingVisitor.cmtypeHashSet){
				cmtypesFile.append(i);
				cmtypesFile.newLine();
			}
			cmtypesFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
			Iterator iterator = selection.iterator();
			long totalTime = 0;
			while(iterator.hasNext()){
			Object firstElement = iterator.next();
			if (firstElement instanceof IFile) {
				IFile thisFile = (IFile)firstElement;
				
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(thisFile);
				parser.setSource(icompilationUnit); // set source
				parser.setResolveBindings(true); // we need bindings later on
				CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
				
				File file = RWTSystemUtil.getCMTypeOperationRuleFile(thisFile.getProject());
				CMTypeRulesManager manager = new CMTypeRulesManager(file);
				long startTime= System.currentTimeMillis();
					typeCheckingVisitor = new TypeCheckingVisitor(manager, compilationResult);
					compilationResult.accept(typeCheckingVisitor);
					CMTypeCheckingResults = typeCheckingVisitor.getErrorReports();
				long endTime= System.currentTimeMillis();
			    long difference = (endTime - startTime)/10; //check different
			    System.out.println("Type checking eclipsed time: " + difference+ " for file "+thisFile.getName());
//			    totalTime += difference;
			}
		}
		*/
//		System.out.println("total time used for the files are: " +totalTime);
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
