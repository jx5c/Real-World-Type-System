package cmtypechecker.annotation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class AnnotationLearner {
	
	public ArrayList<String> itemsets_lhv_all = new ArrayList<String>();
	public ArrayList<String> itemsets_rhv_all = new ArrayList<String>();
	public ArrayList<String> statement_list_all = new ArrayList<String>();
	
	public HashMap<String, String> confirmedAnnotationMap = new HashMap<String, String>();
	
	
	private static AnnotationLearner _instance = null;
	
	public static void storeDataSet(){

	}
	
	public static AnnotationLearner getInstance(){
		if(_instance==null){
			_instance = new AnnotationLearner();
		}
		return _instance;
	}
	
	public void outputData(String dataFileName){
		File outputFile = new File(dataFileName);
		if(outputFile.exists()){
			outputFile.delete();	
		}
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(dataFileName));
			for(int i=0;i<itemsets_lhv_all.size();i++){
				System.out.println(i);
				String firstLine = itemsets_lhv_all.get(i)+"="+itemsets_rhv_all.get(i);
				String secondLine = statement_list_all.get(i);
				writer.append(firstLine);
				System.out.println(firstLine);
				writer.append("\n");
				writer.append(secondLine);
				System.out.println(secondLine);
				writer.append("\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void dataSurvey(){
		
		final HashMap <String, ArrayList<String>> map1 = new HashMap <String, ArrayList<String>>();
		HashMap <String, ArrayList<String>> map2 = new HashMap <String, ArrayList<String>>();
		for (int i=0;i<itemsets_lhv_all.size();i++){
			String lhv = itemsets_lhv_all.get(i);
			String rhv = itemsets_rhv_all.get(i);
			String statement = statement_list_all.get(i);
			if(map1.containsKey(rhv)){
				map1.get(rhv).add(lhv);
			}else{
				ArrayList<String> lhvL = new ArrayList<String>();
				lhvL.add(lhv);
				map1.put(rhv, lhvL);
			}
			if(map2.containsKey(rhv)){
				map2.get(rhv).add(statement);
			}else{
				ArrayList<String> stsL = new ArrayList<String>();
				stsL.add(statement);
				map2.put(rhv, stsL);
			}
		}
		List<String> keys = new LinkedList<String>(map1.keySet());
		Collections.sort(keys, new Comparator<String>() {
			@Override
			public int compare(String arg0, String arg1) {
				return map1.get(arg1).size() - map1.get(arg0).size();
			}
		});		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("e:\\annotation_analysis.txt"));
			for(String key: keys){
				out.append(key);
				out.append("\n");
				ArrayList<String> lhvs = map1.get(key);
				ArrayList<String> stats = map2.get(key);
				for(int i=0;i<lhvs.size();i++){
					out.append(lhvs.get(i));
					out.append("\n");
					out.append(stats.get(i));
					out.append("\n");
				}
				out.append("---------------------");
				out.append("\n");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addData(ArrayList<String> itemsets_lhv, 
			ArrayList<String> itemsets_rhv, 
			ArrayList<String> statement_list){
		itemsets_lhv_all.addAll(itemsets_lhv);
		itemsets_rhv_all.addAll(itemsets_rhv);
		statement_list_all.addAll(statement_list);
	}
	
	public void loadRawData(String dataFile){
		try {
			BufferedReader br = new BufferedReader(new FileReader(dataFile));
			
			while(true){
				String first_line = br.readLine();
				if(first_line==null){
					break;
				}else{
					if(first_line.length()==0){
						break;
					}
					String lhr = first_line.split("=")[0];
					String rhr = first_line.split("=")[1];
					itemsets_lhv_all.add(lhr);
					itemsets_rhv_all.add(rhr);
					String second_line = br.readLine();
					statement_list_all.add(second_line);	
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadConfirmedData(String dataFile){
		if(!new File(dataFile).exists()){
			return;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(dataFile));
			while(true){
				String line = br.readLine();
				if(line==null){
					break;
				}else{
					if(line.length()==0){
						break;
					}
					String lhr = line.split("=")[0];
					String rhr = line.split("=")[1];
					confirmedAnnotationMap.put(rhr, lhr);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		AnnotationLearner learner = AnnotationLearner.getInstance();
		learner.loadRawData("e:\\annotation_patterns.txt");
//		for (int i=0;i<learner.itemsets_lhv_all.size();i++){
//			System.out.println(learner.itemsets_lhv_all.get(i));
//			System.out.println(learner.itemsets_rhv_all.get(i));
//			System.out.println(learner.statement_list_all.get(i));
//		}
//		System.out.println(learner.statement_list_all.size());
		learner.dataSurvey();
		/*
		learner.loadConfirmedData("e:\\confirmed_annotations.txt");
		List<String> keys = new LinkedList<String>(learner.confirmedAnnotationMap.keySet());
		for (String i:keys){
			System.out.println(i);
			System.out.println(learner.confirmedAnnotationMap.get(i));
		}
		*/
		
	}


}

