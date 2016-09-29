package rwtchecker.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import rwtchecker.extractor.Extractor;
import rwtchecker.extractor.WordNetTool;

public class ExtractorVisitor extends ASTVisitor{
	
	public static HashMap<String, HashSet<String>> methodItems = new HashMap<String, HashSet<String>>();
	public static HashMap<String, HashSet<String>> methodCallee = new HashMap<String, HashSet<String>>();
	
	private int identifier_count = 0;
	
	WordNetTool wordNetTool = new WordNetTool();
//	private String currentAccessingMethod = "";
	private String currentAccessingStatement = "";
	
	private HashSet<String> allTermsHashSet = new HashSet<String>();
	private HashSet<String> termsHashSetResults = new HashSet<String>();
	
	public static HashMap<String, Integer> candidateTermsMap = new HashMap<String, Integer>();
	
	boolean accessingPrimitiveAndMethods = false;
	
	public ExtractorVisitor(){
		identifier_count = 0;
	}
	
	@Override
	public void preVisit(ASTNode astNode){
		if (astNode instanceof VariableDeclarationStatement || astNode instanceof Assignment){
			currentAccessingStatement = astNode.toString(); 
			termsHashSetResults.clear();
			allTermsHashSet.clear();
		}
		
		if(astNode instanceof ExpressionStatement){
			if(currentAccessingStatement.length()>0){
				ExpressionStatement expressionST = (ExpressionStatement)astNode;
				if (expressionST.getExpression() instanceof MethodInvocation){
					currentAccessingStatement = astNode.toString(); 
					termsHashSetResults.clear();
					allTermsHashSet.clear();
				}
			}
		}
	}
	
	@Override
	public boolean visit(MethodDeclaration md){
		String methodName = md.getName().getIdentifier(); 
		if(methodName.startsWith("get")|| methodName.startsWith("set")){
			String[] termsInMethod = Extractor.getSplitArray2(methodName);
			for (String term: termsInMethod){
				String consistentTerm = WordNetTool.getConsistentLemma(term);
				if(consistentTerm.length()>0){
					if(candidateTermsMap.containsKey(consistentTerm)){
						candidateTermsMap.put(consistentTerm, candidateTermsMap.get(consistentTerm)+1);
					}else{
						candidateTermsMap.put(consistentTerm, 1);
					}
				}
			}
		}
		return true;
	}
	
	/*
	@Override
	public boolean visit(FieldDeclaration fd){
		List<VariableDeclarationFragment> fragments = fd.fragments();
		for (VariableDeclarationFragment fragment: fragments){
			String typeKey = fragment.resolveBinding().getType().getKey();
			if(typeKey.equals("I") || typeKey.equals("F") || typeKey.equals("D") || typeKey.equals("L")){
				String[] termsInMethod = Extractor.getSplitArray2(fragment.getName().getIdentifier());
				for (String term: termsInMethod){
					String consistentTerm = WordNetTool.getConsistentLemma(term);
					if(consistentTerm.length()>0){
						if(candidateTermsMap.containsKey(consistentTerm)){
							candidateTermsMap.put(consistentTerm, candidateTermsMap.get(consistentTerm)+1);
						}else{
							candidateTermsMap.put(consistentTerm, 1);
						}
					}
				}
			}	
		}
		return true;
	}
	*/
	
	@Override
	public void postVisit(ASTNode astNode){
		//mining terms association in method-unit, ignore for now
/*		if(astNode instanceof MethodDeclaration){
			if(currentAccessingMethod.length()>0){
				HashSet<String> termItemset = new HashSet<String>();
				termItemset.addAll(termsMethodsHashSet);
				if(accessingPrimitives){
					Extractor.methodBodyTermMap.put(currentAccessingMethod, termItemset);	
				}
			}	
			accessingPrimitives = false;
			termsMethodsHashSet.clear();
			currentAccessingMethod = "";
		}*/
		
//		if(astNode instanceof InfixExpression){
//			accessingCalculating = false;
//		}
		if (astNode instanceof VariableDeclarationStatement || astNode instanceof Assignment){
			if(currentAccessingStatement.length()>0){
				HashSet<String> termsMethodsHashSetClone = new HashSet<String>();
				termsMethodsHashSetClone.addAll(termsHashSetResults);
				if(accessingPrimitiveAndMethods){
					if(hasConfirmedItems(allTermsHashSet)){
						//ACCESSING statements with useful terms
						Extractor.methodKeyToTermMethodsMap.put(currentAccessingStatement.trim(), termsMethodsHashSetClone);	
					}
				}
			}
			accessingPrimitiveAndMethods = false;
			termsHashSetResults.clear();
			allTermsHashSet.clear();
			currentAccessingStatement = "";
		}
		
		
		//coding right now
		if(astNode instanceof ExpressionStatement){
			if(currentAccessingStatement.length()>0){
				ExpressionStatement expressionST = (ExpressionStatement)astNode;
				if (expressionST.getExpression() instanceof MethodInvocation){
					HashSet<String> termsMethodsHashSetClone = new HashSet<String>();
					termsMethodsHashSetClone.addAll(termsHashSetResults);
					if(accessingPrimitiveAndMethods){
						if(hasConfirmedItems(allTermsHashSet)){
							//ACCESSING statements with useful terms
							Extractor.methodKeyToTermMethodsMap.put(currentAccessingStatement.trim(), termsMethodsHashSetClone);	
						}
					}
				}
			}
			accessingPrimitiveAndMethods = false;
			termsHashSetResults.clear();
			allTermsHashSet.clear();
			currentAccessingStatement = "";
		}
		
		if (astNode instanceof TypeDeclaration){
			String className = ((TypeDeclaration)astNode).getName().getIdentifier();
			Extractor.file2Identifers.put(className, this.identifier_count);
			this.identifier_count = 0;
		}

	}
	
	@Override
	public boolean visit(SimpleName simpleName){
			
		    String termComposite = simpleName.getIdentifier();
			ITypeBinding typeBinding = simpleName.resolveTypeBinding();
			if(typeBinding==null){
				return false;
			}
			
			//only consider variable with type INTEGER, FLOAT, DOUBLE
			String typeKey = typeBinding.getKey();
			IBinding ib = simpleName.resolveBinding();
			if(ib instanceof IVariableBinding){
				if(typeKey.equals("I") || typeKey.equals("F") || typeKey.equals("D")){
					accessingPrimitiveAndMethods = true;
				}
			}
			if(ib instanceof IMethodBinding){
				accessingPrimitiveAndMethods = true;
			}

			ArrayList<String> filterList = Extractor.getFilterList();
			Extractor.allIdentifiers++;
			this.identifier_count++;
			ArrayList<String> splitedResults = Extractor.getAllTerms(termComposite,filterList, wordNetTool);
			if(splitedResults.size()>0){
				Extractor.identifiers_with_noun++;
				Extractor.splitted_terms = Extractor.splitted_terms + splitedResults.size();
				addTermsAndFreq(splitedResults, filterList, wordNetTool);	
			}
			
			

			//mining terms
			
			//preprocessing, and mining terms
			if(currentAccessingStatement.length()>0){
				allTermsHashSet.addAll(splitedResults);	
			}
			
			
			//mining terms and methods
			if(currentAccessingStatement.length()>0){
				/*
				if(fbinding instanceof IMethodBinding){
					if(simpleName.getParent()!=null && simpleName.getParent() instanceof MethodInvocation){
						addTermsAndFreq("m_"+simpleName.getIdentifier());
						termsHashSetResults.add("m_"+simpleName.getIdentifier());
						//tokenize method names; adding those tokens into the itemset
						HashSet<String> tempSet = new HashSet<String>();
						tempSet.addAll(splitedResults);
						if(hasConfirmedItems(tempSet)){
							for(String term : splitedResults){
								termsHashSetResults.add(term);
							}	
						}
						//we can disable this part; we can choose to use method name only or use all terms in method name; shall we include method name or not?
					}
				}
				*/
				HashSet<String> tempSet = new HashSet<String>();
				tempSet.addAll(splitedResults);
				if(hasConfirmedItems(tempSet)){
					for(String term : splitedResults){
						termsHashSetResults.add(term);
					}	
				}
			}
			
			/*
			switch (mining_choice){
				case 0: 
					//preprocessing
					if(currentAccessingStatement.length()>0){
						tempHashSet.addAll(splitedResults);	
					}
					break;
				case 1:
					//error detection option one
					if(currentAccessingStatement.length()>0){
						if(fbinding instanceof IMethodBinding){
							if(simpleName.getParent()!=null && simpleName.getParent() instanceof MethodInvocation){
								addTermsAndFreq("m_"+simpleName.getIdentifier());
								tempHashSet.add("m_"+simpleName.getIdentifier());
							}
							for(String term : splitedResults){
								if(Extractor.confirmedList.contains(term)){
									tempHashSet.add(term);
								}
							}
						}else{
							for(String term : splitedResults){
								if(Extractor.confirmedList.contains(term)){
									tempHashSet.add(term);
								}
							}
						}
					}
					break;
				case 2: 
					//option one: mining all terms after splitting
					if(currentAccessingStatement.length()>0){
						tempHashSet.addAll(splitedResults);	
					}
					//option three: mining all keys for variables and function
					if(fbinding instanceof IMethodBinding){
						IMethodBinding iMethodBinding = (IMethodBinding) fbinding;
						String key = iMethodBinding.getKey();
						if(currentAccessingStatement.length()>0){
							MethodItemsetVisitor.callerDepth = 0;
							if(ExtractionMethodItemsVisitor.methodKeyItemsets.containsKey(key)){
								HashSet<String> results = ExtractionMethodItemsVisitor.methodKeyItemsets.get(key);
								tempHashSet.addAll(results);	
							}
						}
					}
					break;
				case 3: 
					// to be done?
			}
			*/
			
			
			
			//method-unit 
			/*
			if(currentAccessingMethod.length()>0){
				HashSet<String> termItemset = null;
				if(!Extractor.methodBodyTermMap.containsKey(currentAccessingMethod)){
					termItemset = new HashSet<String>();
				}else{
					termItemset = Extractor.methodBodyTermMap.get(currentAccessingMethod);
				}
				termItemset.addAll(splitedResults);
				Extractor.methodBodyTermMap.put(currentAccessingMethod, termItemset);
			}
			*/
			

			
			return true; 
		}		
	
	public boolean hasConfirmedItems(HashSet<String> termItemset){
		if (Extractor.confirmedList.size()==0){
			return true;
		}
		for(String item :termItemset){
			if(Extractor.confirmedList.contains(item)){
				return true;
			}
		}
		return false;
	}
	
	public static void addTermsAndFreq(ArrayList<String> terms, ArrayList<String> filterList, WordNetTool wordnetool){
		for(int j=0;j<terms.size();j++){
			if(terms.size()<1){
				return;
			}
			String thisString = terms.get(j);
			if(!wordnetool.checkIfHaveUsefulSense(thisString) || filterList.contains(thisString)){
				continue;
			}
			thisString = wordnetool.getLemma(thisString); 
			String firstLetter = thisString.substring(0,1);
			thisString = thisString.replaceFirst(firstLetter, firstLetter.toLowerCase());
			
			if(!Extractor.termFreqMap.containsKey(thisString)){
				Map<String, Integer> multiWordMap = new HashMap<String, Integer>();
				multiWordMap.put(thisString, 1);
				Extractor.termFreqMap.put(thisString, multiWordMap);
			}
			else{
				int currentFreq = Extractor.termFreqMap.get(thisString).get(thisString);
				Extractor.termFreqMap.get(thisString).put(thisString, currentFreq+1);
			}
			
			Map<String, Integer> map = Extractor.termFreqMap.get(thisString);
			for(int p=0;p<terms.size();p++){
				String relatedWord = terms.get(p);
				String firstChar = relatedWord.substring(0,1);
				relatedWord = relatedWord.replaceFirst(firstChar, firstChar.toLowerCase());
				if(!relatedWord.equals(thisString)){
					if(!map.containsKey(relatedWord)){
						map.put(relatedWord, 1);
					}else{
						map.put(relatedWord,map.get(relatedWord)+1);
					}
				}
			}
		}
	}
	
}


