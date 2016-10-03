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

import rwtchecker.extractor.Extractor;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.typechecker.ExtractionMethodItemsVisitor;
import rwtchecker.typechecker.ExtractorVisitor;
import rwtchecker.typechecker.MethodItemsetVisitor;
import rwtchecker.typechecker.NewTypeCheckerVisitor;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.DiagnosticMessage;

public class ExtractTerms implements IObjectActionDelegate {
	
	private ISelection selection;
	private Shell shell;

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public ExtractTerms() {
		super();
	}
	
	public void run(IAction action) {
		IStructuredSelection selection = (IStructuredSelection)(this.selection);
		IProject iProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
		
		if (iProject == null) 
			return;

	    IJavaProject javaProject = JavaCore.create(iProject);
	    try {
	    	ArrayList<IResource> javaSourceFiles = RWTSystemUtil.getAllJavaSourceFiles(javaProject);
    		try {
    			BufferedReader in = new BufferedReader(new FileReader("e:\\confirmedList.txt"));
    			String temp = null;
    			while ((temp = in.readLine())!= null){
    				Extractor.confirmedList.add(temp.split(",")[0]);
    			}
    			in.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
            
    		ExtractorVisitor.candidateTermsMap.clear();
    		
        	for(IResource javaSource:javaSourceFiles){
        		ASTParser parser = ASTParser.newParser(AST.JLS3);
        		parser.setKind(ASTParser.K_COMPILATION_UNIT);
        		IWorkspace workspace= ResourcesPlugin.getWorkspace();    
        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
        		if(sourceFile.exists()){
        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        			parser.setSource(icompilationUnit); // set source
        			parser.setResolveBindings(true); // we need bindings later on
        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
        			ExtractorVisitor extractVisitor = new ExtractorVisitor();
        			compilationResult.accept(extractVisitor);
        			System.out.println("extracting terms from class: "+ sourceFile.getName());
        		}
        	}
        	
        	System.out.println("Start printing important terms from method names");
    		BufferedWriter method_name_BW = null;
    		try {
    			method_name_BW = new BufferedWriter(new FileWriter("e:\\candidate_terms.txt"));
    		} catch (IOException e1) {
    			e1.printStackTrace();
    		}
    		Iterator candidate_keys = ExtractorVisitor.candidateTermsMap.keySet().iterator();
    		while(candidate_keys.hasNext()){
    			String termName = candidate_keys.next().toString();
    			int freq = ExtractorVisitor.candidateTermsMap.get(termName);
    			method_name_BW.append(termName+","+String.valueOf(freq)+"\n");
    			method_name_BW.newLine();
    		}
    		method_name_BW.close();
            System.out.println("Printing Ends");
        	
            
            System.out.println("extracting Ends, Start printing");     	
    		BufferedWriter out= null;
    		try {
    			out = new BufferedWriter(new FileWriter("e:\\TermsFreqMap.txt"));
    		} catch (IOException e1) {
    			e1.printStackTrace();
    		}
    		Iterator keys = Extractor.termFreqMap.keySet().iterator();
    		while(keys.hasNext()){
    			String termName = keys.next().toString();
    			Map<String, Integer> map = Extractor.termFreqMap.get(termName);
    			Iterator mapIterator = map.keySet().iterator();
    			StringBuilder toPrint = new StringBuilder(); 
    			while(mapIterator.hasNext()){
    				String nextWord = mapIterator.next().toString();
    				String freq = String.valueOf(map.get(nextWord)); 
    				if(nextWord.equals(termName)){
    					toPrint.insert(0, nextWord+","+freq);
    				}else{
    					toPrint.append(","+nextWord+","+freq);
    				}
    			}
    			out.append(toPrint.toString()+"\n");
//    			out.newLine();
    		}
            out.close();
            System.out.println("Printing Ends");
            System.out.println("Start printing itemsets");
            
            
    		BufferedWriter termMethodItemsetInHashCodeBW= null;
    		BufferedWriter termMethodItemsetInStringBW= null;
    		
//    		BufferedWriter termsItemsetInHashCodeBW= null;
//    		BufferedWriter termsItemsetInStringBW= null;
//			termsItemsetInHashCodeBW = new BufferedWriter(new FileWriter("e:\\termsItemsetInHashCode.txt"));
//			termsItemsetInStringBW = new BufferedWriter(new FileWriter("e:\\termsItemsetInString.txt"));
//    		termsItemsetInHashCodeBW.close();
//    		termsItemsetInStringBW.close();

//    		BufferedWriter itemsetTermsResultBW= null;
//			itemsetTermsResultBW = new BufferedWriter(new FileWriter("e:\\itemsetTermsResult.txt"));
//    		itemsetTermsResultBW.close();
    		
    		BufferedWriter itemsetTermMethodsResultBW= null;
    		try {
    			termMethodItemsetInHashCodeBW = new BufferedWriter(new FileWriter("e:\\termMethodItemsetInHashCode.txt"));
    			termMethodItemsetInStringBW = new BufferedWriter(new FileWriter("e:\\termMethodItemsetInString.txt"));
    			
    			itemsetTermMethodsResultBW = new BufferedWriter(new FileWriter("e:\\itemsetTermMethodsResult.txt"));

    		} catch (IOException e1) {
    			e1.printStackTrace();
    		}
    		Map<String, String> itemStringToHashCodeMap = new HashMap<String, String>();
    		Map<String, String> hashCodeToItemMap = new HashMap<String, String>();
    		
    		this.customizedHashindFunc(itemStringToHashCodeMap, hashCodeToItemMap);
    		
    		Iterator keys2 = Extractor.methodKeyToTermMethodsMap.keySet().iterator();
    		while(keys2.hasNext()){
    			String methodBodyKey = keys2.next().toString();
    			HashSet<String> itemset = Extractor.methodKeyToTermMethodsMap.get(methodBodyKey);
    			String hashCodedItemSet =  hashCodingItemset(itemset, itemStringToHashCodeMap);
    			
    			termMethodItemsetInHashCodeBW.append(methodBodyKey);
    			termMethodItemsetInHashCodeBW.newLine();
    			termMethodItemsetInHashCodeBW.append(hashCodedItemSet);
    			termMethodItemsetInHashCodeBW.newLine();
    			termMethodItemsetInHashCodeBW.newLine();
    			
    			termMethodItemsetInStringBW.append(methodBodyKey);
    			termMethodItemsetInStringBW.newLine();
    			termMethodItemsetInStringBW.append(itemset.toString());
    			termMethodItemsetInStringBW.newLine();
    			termMethodItemsetInStringBW.newLine();
    			
    			itemsetTermMethodsResultBW.append(hashCodedItemSet);
    			itemsetTermMethodsResultBW.newLine();
    		}
    		//save for later usage
    		/*
    		Iterator keys3 = Extractor.methodKeyToTermsMap.keySet().iterator();
    		while(keys3.hasNext()){
    			String methodBodyKey = keys3.next().toString();
    			HashSet<String> itemset = Extractor.methodKeyToTermsMap.get(methodBodyKey);
    			String hashCodedItemSet =  hashCodingItemset(itemset, itemStringToHashCodeMap);
    			
    			termsItemsetInHashCodeBW.append(methodBodyKey);
    			termsItemsetInHashCodeBW.newLine();
    			termsItemsetInHashCodeBW.append(hashCodedItemSet);
    			termsItemsetInHashCodeBW.newLine();
    			
    			termsItemsetInStringBW.append(methodBodyKey);
    			termsItemsetInStringBW.newLine();
    			termsItemsetInStringBW.append(itemset.toString());
    			termsItemsetInStringBW.newLine();
    			
    			itemsetTermsResultBW.append(hashCodedItemSet);
    			itemsetTermsResultBW.newLine();
    		}
    		*/
    		    		
    		termMethodItemsetInHashCodeBW.close();
    		termMethodItemsetInStringBW.close();
    		itemsetTermMethodsResultBW.close();
    		

    		
    		System.out.println("ends printing itemsets");
    		
    		System.out.println("Printing hashcode to item mapping");
    		BufferedWriter itemsetCodeMapBW= null;
    		itemsetCodeMapBW = new BufferedWriter(new FileWriter("e:\\itemsetCodeMapping.txt"));
    		Iterator keys4 = hashCodeToItemMap.keySet().iterator();
    		while(keys4.hasNext()){
    			String hashCodedString = keys4.next().toString();
    			String itemInString = hashCodeToItemMap.get(hashCodedString);
    			itemsetCodeMapBW.append(hashCodedString+":"+itemInString);
    			itemsetCodeMapBW.newLine();
    		}
    		itemsetCodeMapBW.close();
    		System.out.println("end printing hashcode to item mapping");
    		
    		
    		
    		
    		System.out.println("Printing stats");
    		System.out.println("all identifiers count is: "+ Extractor.allIdentifiers);
    		System.out.println("identifiers with nouns count is: "+ Extractor.identifiers_with_noun);
    		System.out.println("splitted terms count is: "+ Extractor.splitted_terms);
    		System.out.println("all splitted terms count is: "+ Extractor.all_splitted_terms);
    		System.out.println("noun words count is : "+ Extractor.noun_words.size());
    		System.out.println("candidates count is: "+ Extractor.termFreqMap.size());
    		
    		System.out.println("------------------------");
    		System.out.println("------------------------");
    		System.out.println("Printing identifiers for each file");
    		BufferedWriter fileIdentifiersBW = null;
    		fileIdentifiersBW = new BufferedWriter(new FileWriter("e:\\file2identifier.txt"));
    		Iterator fileIdentifiersIterator = Extractor.file2Identifers.keySet().iterator();
    		while(fileIdentifiersIterator.hasNext()){
    			String fileName = fileIdentifiersIterator.next().toString();
    			int count = Extractor.file2Identifers.get(fileName);
    			fileIdentifiersBW.append(fileName+","+count);
    			fileIdentifiersBW.newLine();
    		}
    		fileIdentifiersBW.close();
    		System.out.println("end Printing identifiers for each file");
    		System.out.println("------------------------");
    		System.out.println("------------------------");
    		
    		System.out.println("------------------------");
    		System.out.println("------------------------");
    		System.out.println("Printing number of terms per identifier");
    		BufferedWriter termsPerIdentifierBW = null;
    		termsPerIdentifierBW = new BufferedWriter(new FileWriter("e:\\termsPerIdentifier.txt"));
    		for(int i=0;i<Extractor.terms_count.size();i++){
    			termsPerIdentifierBW.append(String.valueOf(Extractor.terms_count.get(i)));
    			termsPerIdentifierBW.newLine();
    		}
    		termsPerIdentifierBW.close();
    		System.out.println("end Printing number of terms per identifier");
    		System.out.println("------------------------");
    		System.out.println("------------------------");
    		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	private void customizedHashindFunc(Map<String, String> itemStringToHashCodeMap, Map<String, String> hashCodeToItemMap){
		Iterator<String> itemNames = Extractor.termFreqMap.keySet().iterator();
		int count = 1;
		while(itemNames.hasNext()){
			int hashedCode = count++;
			String nextItem = itemNames.next();
			itemStringToHashCodeMap.put(nextItem, String.valueOf(hashedCode));
			hashCodeToItemMap.put(String.valueOf(hashedCode), nextItem);
		}
	}
	
	private String hashCodingItemset(HashSet<String> inputs, Map<String, String> itemStringToHashCodeMap){
		if(inputs.size()==0){
			return "";
		}else{
			String[] InputArray = inputs.toArray(new String[inputs.size()]);
			String result = String.valueOf(itemStringToHashCodeMap.get(InputArray[0]));
			for(int i=1; i< InputArray.length;i++){
				String hashCodeString = String.valueOf(itemStringToHashCodeMap.get(InputArray[i]));
				result = result + " "+hashCodeString;
			}
			return result;
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
	
}
