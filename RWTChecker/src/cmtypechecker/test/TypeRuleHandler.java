package cmtypechecker.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cmtypechecker.CM.CMType;
import cmtypechecker.CMRules.CMTypeRule;
import cmtypechecker.CMRules.CMTypeRuleCategory;
import cmtypechecker.CMRules.CMTypeRulesManager;
import cmtypechecker.util.CMModelUtil;

public class TypeRuleHandler {
	public static void main(String args[]){
		File typeRuleFile = new File("E:/Develop/EvaluationCMs/KelpieFlightPlanner/CMTypeRuleFile.xml");
		String allContents = "";
		String temp = null;
		try {
			BufferedReader input = new BufferedReader(new FileReader(typeRuleFile));
			while((temp = input.readLine())!=null){
				allContents = allContents + temp;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ArrayList<CMType> results = new ArrayList<CMType>();
		String location = "E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner";
		if(location !=null){
			File dir = new File(location.toString()+CMModelUtil.PathSeparator+CMModelUtil.CMTypesFolder);
			if((dir.exists())&& (dir.isDirectory())){
				File[] cmtypeFiles = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return dir.isDirectory();
					}
				});
				for(int i=0;i<cmtypeFiles.length;i++){
					results.add(CMType.readInCorrespondenceType(cmtypeFiles[i]));
				}
			}	
		}
		HashMap<String, String> setToAttSet = new HashMap<String,String>();
		for(CMType cmtype : results){
			String attSet = cmtype.getEnabledAttributeSet();
			if(attSet.length()>0){
				setToAttSet.put(attSet, cmtype.getTypeName());	
			}
//			allContents.replaceAll(attSet, attSet);
		}
		
		CMTypeRulesManager manager = new CMTypeRulesManager(new File("E:/Develop/EvaluationCMs/KelpieFlightPlanner/CMTypeRuleFile.xml"));
		ArrayList<CMTypeRule> rules = manager.getDefinedOperations();
		ArrayList<CMTypeRule> newRules = new ArrayList<CMTypeRule>();
		for(CMTypeRule cmtypeRule: rules){
			String operandOne = cmtypeRule.getCMTypeOneName();
			String operandTwo = cmtypeRule.getCMTypeTwoName();
			String resultSet = cmtypeRule.getReturnCMTypeName();
			String newOpOne = transferSetNames(operandOne,setToAttSet);
			String newOpTwo = transferSetNames(operandTwo,setToAttSet);
			String newResultSet = transferSetNames(resultSet,setToAttSet);
			cmtypeRule.setCMTypeOneName(newOpOne);
			cmtypeRule.setCMTypeTwoName(newOpTwo);
			cmtypeRule.setReturnCMTypeName(newResultSet);
			newRules.add(cmtypeRule);
		}
		CMTypeRulesManager newmanager = new CMTypeRulesManager(new File("E:/newCMTypeRuleFile.xml"));
		newmanager.addCMTypeOperations(newRules);
		newmanager.storeRules();
		
		
	}
	public static String transferSetNames(String input, HashMap<String, String> setToAttSet){
		if(!input.contains("(")){
			if(setToAttSet.containsKey(input)){
				return setToAttSet.get(input);
			}else{
				return input;
			}
		}else{
			ArrayList<String> ops = CMTypeRuleCategory.getOpNames();
			Pattern p = Pattern.compile("([^\\(|\\)|\\@]+)");
			Matcher m = p.matcher(input);
			String result = input;
			while (m.find()) {
				String temp = m.group();
				if(!ops.contains(temp)){
					if(setToAttSet.containsKey(temp)){
						result = result.replace(temp, setToAttSet.get(temp));
					}
				}
			}
			return result;
		}
		

	}
}
