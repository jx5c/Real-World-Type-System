package rwtchecker.typechecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import rwtchecker.extractor.Extractor;
import rwtchecker.extractor.WordNetTool;


public class MethodItemsetVisitor extends ASTVisitor{
	public static int callerDepth = 0;
	String targetMethodKey = "";
	HashSet<String> results = new HashSet<String>();
	boolean insideTargetMethod = false;
	
	public HashSet<String> getResults() {
		return results;
	}

	public void setResults(HashSet<String> results) {
		this.results = results;
	}

	WordNetTool wordNetTool = new WordNetTool();
	
	public MethodItemsetVisitor(String target){
		MethodItemsetVisitor.this.targetMethodKey = target;
	}
	
	@Override
	public void preVisit(ASTNode astNode){
		if(astNode instanceof MethodDeclaration){
			MethodDeclaration md = (MethodDeclaration)astNode;
			if(targetMethodKey.equals(md.resolveBinding().getKey())){
				insideTargetMethod = true;
			}
		}
	}
	
	@Override
	public void postVisit(ASTNode astNode){
		if(astNode instanceof MethodDeclaration){
			MethodDeclaration md = (MethodDeclaration)astNode;
			if(targetMethodKey.equals(md.resolveBinding().getKey())){
				insideTargetMethod = false;
			}
		}
	}
	
	@Override
	public boolean visit(SimpleName simpleName){
		if(insideTargetMethod){
			String termComposite = simpleName.getIdentifier();
			
			ITypeBinding typeBinding = simpleName.resolveTypeBinding();
			if(typeBinding==null){
				return false;
			}
			IBinding fbinding = simpleName.resolveBinding();
			ArrayList<String> filterList = Extractor.getFilterList();
			if(fbinding instanceof IVariableBinding){
				ArrayList<String> splitedVariableResults = Extractor.getAllTerms(termComposite,filterList, wordNetTool);
				
				for(int j=0;j<splitedVariableResults.size();j++){
					String thisString = splitedVariableResults.get(j);
					thisString = thisString.toLowerCase();
					if(!Extractor.termFreqMap.containsKey(thisString)){
						Map<String, Integer> multiWordMap = new HashMap<String, Integer>();
						multiWordMap.put(thisString, 1);
						Extractor.termFreqMap.put(thisString, multiWordMap);
					}
					else{
						int currentFreq = Extractor.termFreqMap.get(thisString).get(thisString);
						Extractor.termFreqMap.get(thisString).put(thisString, currentFreq+1);
					}
				}
				results.addAll(splitedVariableResults);	
//				if(currentAccessingStatement.length()>0){
//					tempHashSet.addAll(splitedVariableResults);	
//				}
			}
			if(fbinding instanceof IMethodBinding){
				//interProcural analysis
				IMethodBinding iMethodBinding = (IMethodBinding) fbinding;
//				if(currentAccessingStatement.length()>0){
//					tempHashSet.add(termComposite);	
//				}
				MethodItemsetVisitor.callerDepth = callerDepth + 1;
				if(callerDepth<=2){
					results.addAll(getItemsByMethodKey(iMethodBinding));	
				}else{
					ArrayList<String> splitedVariableResults = Extractor.getAllTerms(termComposite,filterList, wordNetTool);
					for(int j=0;j<splitedVariableResults.size();j++){
						String thisString = splitedVariableResults.get(j);
						thisString = thisString.toLowerCase();
						if(!Extractor.termFreqMap.containsKey(thisString)){
							Map<String, Integer> multiWordMap = new HashMap<String, Integer>();
							multiWordMap.put(thisString, 1);
							Extractor.termFreqMap.put(thisString, multiWordMap);
						}
						else{
							int currentFreq = Extractor.termFreqMap.get(thisString).get(thisString);
							Extractor.termFreqMap.get(thisString).put(thisString, currentFreq+1);
						}
					}
					results.addAll(splitedVariableResults);	
				}
			}
		}
			
			//only consider variable with type INTEGER, FLOAT, DOUBLE
			
//			String key = typeBinding.getKey();
//			if(key.equals("I") || key.equals("F") || key.equals("D")){
//				accessingDigits = true;
//			}

			return true; 
	}
	
	
	
	public static HashSet<String> getItemsByMethodKey(final IMethodBinding iMethodBinding){
		if(ExtractionMethodItemsVisitor.methodKeyItemsets.containsKey(iMethodBinding.getKey())){
			return ExtractionMethodItemsVisitor.methodKeyItemsets.get(iMethodBinding.getKey());
		}
		HashSet<String> results = new HashSet<String>();
		//method declared in other sources
		if(iMethodBinding!=null && iMethodBinding.getJavaElement()!=null){
			IFile methodDeclFile = ResourcesPlugin.getWorkspace().getRoot().getFile(iMethodBinding.getJavaElement().getPath());
			if((methodDeclFile != null) && (methodDeclFile.getFileExtension().toLowerCase().endsWith("java"))){
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(methodDeclFile);
				parser.setSource(icompilationUnit); // set source
				parser.setResolveBindings(true); // we need bindings later on
				final CompilationUnit otherCompilationResult = (CompilationUnit) parser.createAST(null);
				BufferedReader infile = null;
				ArrayList<String> contents = new ArrayList<String>();
				String line = "";
				try {
					infile = new BufferedReader(new FileReader(methodDeclFile.getRawLocation().toFile()));
					while((line = infile.readLine())!= null){
						contents.add(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				MethodItemsetVisitor visitor = new MethodItemsetVisitor(iMethodBinding.getKey());
				otherCompilationResult.accept(visitor);
				results = visitor.getResults();
			}
		}
		return results;
	}
}
