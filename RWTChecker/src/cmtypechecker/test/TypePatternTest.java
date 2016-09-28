package cmtypechecker.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cmtypechecker.CMRules.CMTypeRule;
import cmtypechecker.CMRules.CMTypeRulesManager;

public class TypePatternTest {
	
	public static void main(String[] args){
//		CMTypeRulesManager cmtm = new CMTypeRulesManager(new File("E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\CMTypeRules.txt"));
//		CMTypeRule cmTypeOperation = new CMTypeRule(CMTypeRuleCategory.Plus, "latitude_radians", "longitude_radians", "error", CMTypeRule.Verified);
//		cmtm.addCMTypeOperation(cmTypeOperation);
//		System.out.println(cmtm.getDefinedOperations().get(1).getCMTypeOneName());
//		System.out.println(cmtm.getDefinedOperations().get(1).getCMTypeTwoName());
//		System.out.println(cmtm.getDefinedOperations().get(1).getOperationName());
//		System.out.println(cmtm.getDefinedOperations().get(1).getReturnCMTypeName());
//		System.out.println(cmtm.getDefinedOperations().get(1).getTypeRuleCategory());
//		cmtm.storeRules();
		
		File folder = new File("E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\CMTYPE_Patterns");
		File[] files = folder.listFiles();
		Map<String, Integer > typePatternMap = new HashMap<String, Integer>();
		for (int i=0;i<files.length;i++ ){
			File file = files[i];
			try {
				BufferedReader in = new BufferedReader(new FileReader(file));
				String patternStr = null;
				while(true){
					patternStr = in.readLine();
					if(patternStr == null){
						break;
					}
					if(typePatternMap.containsKey(patternStr)){
						int court = typePatternMap.get(patternStr);
						court = court + 1;
						typePatternMap.put(patternStr, court);
					}else{
						typePatternMap.put(patternStr, 1);
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter("e://patternCMType.csv"));
			for (int i=0;i<typePatternMap.keySet().size();i++){
				int court = typePatternMap.get(typePatternMap.keySet().toArray()[i].toString());
				bw.write(court +"," + typePatternMap.keySet().toArray()[i].toString() + "\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}
}
