package cmtypechecker.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cmtypechecker.CMRules.CMTypeRule;
import cmtypechecker.CMRules.CMTypeRulesManager;

public class TypeRuleGeneratorTest {
	
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
		
		File folder = new File("E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\CMTYPE_Rules");
		File[] files = folder.listFiles();
		ArrayList<CMTypeRule> ruleList = new ArrayList<CMTypeRule>();
		for (int i=0;i<files.length;i++ ){
			File file = files[i];
			CMTypeRulesManager cmtm = new CMTypeRulesManager(file);
			ruleList.addAll(cmtm.getDefinedOperations());
		}
		Map<String, Integer > typeRuleMap = new HashMap<String, Integer>();
		for (int i=0;i<ruleList.size();i++){
			CMTypeRule cmTypeRule = ruleList.get(i);
			if(typeRuleMap.containsKey(cmTypeRule.toString())){
				int court = typeRuleMap.get(cmTypeRule.toString());
				court = court + 1;
				typeRuleMap.put(cmTypeRule.toString(), court);
			}else{
				typeRuleMap.put(cmTypeRule.toString(), 1);
			}
		}
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter("e://jianjian.csv"));
			for (int i=0;i<typeRuleMap.keySet().size();i++){
				int court = typeRuleMap.get(typeRuleMap.keySet().toArray()[i].toString());
				CMTypeRule cmtypeRule = CMTypeRule.constructOpFromString(typeRuleMap.keySet().toArray()[i].toString());
				bw.write(court +"," + cmtypeRule.getOperationName() +"," + cmtypeRule.getCMTypeOneName()+"," + cmtypeRule.getCMTypeTwoName() +","+ cmtypeRule.getReturnCMTypeName()+"\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}
}
