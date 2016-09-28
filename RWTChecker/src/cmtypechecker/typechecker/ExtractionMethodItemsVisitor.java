package cmtypechecker.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import cmtypechecker.extractor.Extractor;
import cmtypechecker.extractor.WordNetTool;

/**
 * This class runs the extractions process with 
 * @author Admin
 *
 */
public class ExtractionMethodItemsVisitor extends ASTVisitor{
	public static int callerDepth = 0;
	
	public static Map<String, HashSet<String>> methodKeyItemsets= new HashMap<String, HashSet<String>>();
	WordNetTool wordNetTool = new WordNetTool();
	private String currentAccessingMethod = ""; 
	HashSet<String> methodItemset = new HashSet<String>();
			
	public ExtractionMethodItemsVisitor(){
	}
	
	@Override
	public void preVisit(ASTNode astNode){
		if(astNode instanceof MethodDeclaration){
			MethodDeclaration md = (MethodDeclaration)astNode;
			currentAccessingMethod = md.resolveBinding().getKey();
			methodItemset.clear();
		}
	}
	
	@Override
	public void postVisit(ASTNode astNode){
		if(astNode instanceof MethodDeclaration){
			MethodDeclaration md = (MethodDeclaration)astNode;
			HashSet<String> thisMethodItemset = methodItemset;
			if(currentAccessingMethod.length()>0){
				methodKeyItemsets.put(currentAccessingMethod, (HashSet<String>)(thisMethodItemset.clone()));
				currentAccessingMethod = "";	
			}
		}
	}
	
	@Override
	public boolean visit(SimpleName simpleName){
		if(currentAccessingMethod.length()>0){
			String termComposite = simpleName.getIdentifier();
			
			ITypeBinding typeBinding = simpleName.resolveTypeBinding();
			if(typeBinding==null){
				return false;
			}
			
			IBinding fbinding = simpleName.resolveBinding();
			ArrayList<String> filterList = Extractor.getFilterList();			
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
			if((fbinding instanceof IMethodBinding) || (fbinding instanceof IVariableBinding)){
				methodItemset.addAll(splitedVariableResults);
			}
			
			if(fbinding instanceof IMethodBinding){
				//interProcural analysis
				IMethodBinding iMethodBinding = (IMethodBinding) fbinding;
//				if(currentAccessingStatement.length()>0){
//					tempHashSet.add(termComposite);	
//				}
				ExtractionMethodItemsVisitor.callerDepth = 0;
				methodItemset.addAll(MethodItemsetVisitor.getItemsByMethodKey(iMethodBinding));	
			}
		}
			
			//only consider variable with type INTEGER, FLOAT, DOUBLE
			
//			String key = typeBinding.getKey();
//			if(key.equals("I") || key.equals("F") || key.equals("D")){
//				accessingDigits = true;
//			}

			return true; 
	}
	

	
}
