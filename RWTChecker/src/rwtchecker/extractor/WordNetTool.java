package rwtchecker.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;

public class WordNetTool {
	private static Dictionary dict = null;
	
    String wordNetDir = "E:\\Develop\\jwnl14-rc2\\";   
	
	public WordNetTool() {
		try {
			JWNL.initialize(new FileInputStream(new File(wordNetDir+"config\\file_properties.xml")));
			dict = Dictionary.getInstance();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JWNLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public WordNetTool(String javaSourceFileName){
		try {
			JWNL.initialize(new FileInputStream(new File(javaSourceFileName)));
			dict = Dictionary.getInstance();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JWNLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String search(String lemma){
		String senseString = "";
		try {
			IndexWord iw = dict.lookupIndexWord(POS.NOUN, lemma);
			if(iw!=null){
				Synset[] result = iw.getSenses();
				for(int i=0;i<result.length;i++){
					senseString = senseString + "Explication #"+ (i+1) +": "+result[i].getGloss()+"\n"+"/";
				}
			}
		} catch (JWNLException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return senseString;
	}
	
	public boolean checkIfHaveUsefulSense(String lemma){
		boolean result = false;
		try {
			IndexWord iw = dict.lookupIndexWord(POS.NOUN, lemma);
			if(iw!=null){
				Extractor.noun_words.add(lemma);
				if(iw.getSenses().length>0){
					result = true;
				}
			}
			/*
			else{
				iw = dict.lookupIndexWord(POS.ADJECTIVE, lemma);
				if(iw!=null){
					if(iw.getSenses().length>0){
						result = true;
					}
				}
			}
			*/
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public String getLemma(String input){
		IndexWord iw;
		try {
			iw = dict.lookupIndexWord(POS.NOUN, input);
			if(iw!=null){
				return iw.getLemma();
			}
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String getConsistentLemma(String input){
		IndexWord iw;
		try {
			iw = dict.lookupIndexWord(POS.NOUN, input);
			if(iw!=null){
				return iw.getLemma();
			}else{
				iw = dict.lookupIndexWord(POS.ADJECTIVE, input);
				if(iw!=null){
					return iw.getLemma();
				}else{
					return "";
				}
			}
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public boolean checkIfHaveVerbSense(String lemma){
		boolean result = false;
		try {
			IndexWord iw = dict.lookupIndexWord(POS.VERB, lemma);
			if(iw!=null){
				if(iw.getSenses().length>0){
					//Synset[] results = iw.getSenses();
					result = true;
				}
			}
			
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public void close(){
		dict.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(new WordNetTool().checkIfHaveUsefulSense("g175"));
	}

}
