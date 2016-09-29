package rwtchecker.extractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Shell;

import rwtchecker.typechecker.ExtractorVisitor;

public class Extractor {
	
	public static HashMap<String, Map<String, Integer>> termFreqMap = new HashMap<String, Map<String, Integer>>();
	
	public static int allIdentifiers = 0;
	public static int identifiers_with_noun = 0;
	public static int splitted_terms = 0;
	public static int all_splitted_terms = 0;
	public static HashSet<String> noun_words = new HashSet<String>();
	
	public static ArrayList<Integer> terms_count = new ArrayList<Integer>(); 
	
	public static Map<String, Integer> file2Identifers = new HashMap<String, Integer>();
	
	public static Map<String, HashSet<String>> methodKeyToTermMethodsMap = new HashMap<String, HashSet<String>>();
//	public static Map<String, HashSet<String>> methodKeyToTermsMap= new HashMap<String, HashSet<String>>();

	public static ArrayList<String> confirmedList = new ArrayList<String>();
	
	public static String[] getSplitArray(String input){
		StringBuffer changedInput = new StringBuffer("");
		String [] inputsInArray = input.split("_");
		//deal with underscore
		for(int i=0;i<inputsInArray.length;i++){
			if(inputsInArray[i].length()>0){
				String firstChar = String.valueOf(inputsInArray[i].charAt(0));
				String thisInputPart = inputsInArray[i].replaceFirst(firstChar, firstChar.toUpperCase());
				changedInput.append(thisInputPart);
			}
		}
		
		StringBuffer results = new StringBuffer("");
		input = changedInput.toString();
		int lastend = 0;
		for(int j=0;j<input.length();j++){
			if(Character.isUpperCase(input.charAt(j))){
				if(j!=0 && lastend!=j){
					results.append(input.substring(lastend,j)+",");
				}
				lastend = j;
			}
			if(String.valueOf(input.charAt(j)).equals("_")){
				if(j!=0 && lastend!=j){
					results.append(input.substring(lastend,j)+",");
				}
				lastend = j+1;
			}
		}
		results.append(input.substring(lastend));
		return results.toString().split(",");
	}
	
	/**
	 * Tokenization 
	 * @param input
	 * @return
	 */
	public static String[] getSplitArray2(String input){
		StringBuffer results = new StringBuffer("");
		int lastend = 0;
		for(int j=0;j<input.length();j++){
			if(Character.isUpperCase(input.charAt(j))){
				if(j!=0 && lastend!=j){
					if(!Character.isUpperCase(input.charAt(j-1))){
						results.append(input.substring(lastend,j)+",");
						lastend = j;
					}else{
						if(j == input.length() -1){
							break;
						}
						else if(Character.isUpperCase(input.charAt(j+1))){
							continue;
						}
					}
				}
			}
			if(String.valueOf(input.charAt(j)).equals("_")){
				if(j!=0 && lastend!=j){
					results.append(input.substring(lastend,j)+",");
				}
				lastend = j+1;
			}
			if(String.valueOf(input.charAt(j)).equals("$")){
				if(j!=0 && lastend!=j){
					results.append(input.substring(lastend,j)+",");
				}
				lastend = j+1;
			}
				
		}
		results.append(input.substring(lastend));
		return results.toString().toLowerCase().split(",");
	}
	
	public static ArrayList<String> getFilterList(){
		ArrayList<String> filterList = new ArrayList<String>();
		if(new File("e:\\filterlist.txt").exists()){
		    try {
		    	BufferedReader in = new BufferedReader(new FileReader(new File("e:\\filterlist.txt")));
		    	String temp="";
	    		while((temp = in.readLine())!=null){
	    			filterList.add(temp.split(",")[0]);
	    		}
	    		in.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return filterList;
	}
	
	public static ArrayList<String> getAllTerms(String input, ArrayList<String> filterList, WordNetTool wordnetool){
		ArrayList<String> allPhrases = new ArrayList<String>();
		String[] termArray = Extractor.getSplitArray2(input);
		
		terms_count.add(termArray.length);
		
		Extractor.all_splitted_terms = Extractor.all_splitted_terms + termArray.length;
		
		HashMap<String, String> equalTerms = new HashMap<String, String>();
		
		if(new File("e:\\equalTerms.txt").exists()){
			try {
				BufferedReader equalTermsInput = new BufferedReader(new FileReader("e:\\equalTerms.txt"));
				String temp = null;
				while ((temp = equalTermsInput.readLine())!= null){
					equalTerms.put(temp.split("=")[0], temp.split("=")[1]);
				}
				equalTermsInput.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		boolean flag = false;
		for(int i=0;i<termArray.length;i++){
			if(wordnetool.checkIfHaveUsefulSense(termArray[i])){
				flag = true;
			}
			if(Extractor.confirmedList.contains(termArray[i])){
				String tempStr = termArray[i];
				if(equalTerms.containsKey(tempStr)){
					tempStr = equalTerms.get(tempStr);
				}
				allPhrases.add(tempStr);
			}
			if((!filterList.contains(termArray[i].toLowerCase()))
					&&termArray[i].length()>1){
//				String lemma = wordnetool.getLemma(termArray[i]);
				String lemma = termArray[i];
				if(lemma.length()<=2){
					continue;
				}
				if(equalTerms.containsKey(lemma)){
					lemma = equalTerms.get(lemma);
				}
				if(!filterList.contains(lemma)){
					allPhrases.add(lemma);	
				}
			}
		}
		if(!flag){
			return new ArrayList<String>();
		}
		return allPhrases;
	}
	

	
	public static void main(String[] args){
//		String[] results = splitString.getSplitArray("textContextMenuItem", 1);
//		for(int i=0;i<results.length;i++){
//			System.out.println(results[i]);
//		}
//		WordNetTool wordnetool = new WordNetTool();
//		ArrayList<String> filterList = getFilterList();
//		ArrayList<String> allPhases = getAllTerms("setsdf", filterList, wordnetool);
//		System.out.println(wordnetool.checkIfHaveNounSense("set_max"));
//		for(int i=0;i<allPhases.size();i++){
//			System.out.println(allPhases.get(i));
//		}
		
		String [] results = getSplitArray2("getSize");
		for (String result : results ){
			System.out.println(result);
		}
		
		WordNetTool wordnetool = new WordNetTool();
//		ArrayList<String> filterList = getFilterList();
//		ArrayList<String> allPhases = getAllTerms("FEET_PER_DEGREE", filterList, wordnetool);
//
//		for(String phrase: allPhases){
//			System.out.println(phrase);	
//		}
	}
}
