package rwtchecker.rwtrules;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IProject;

import rwtchecker.rwt.RWType;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.UnitsCheckingUtil;

public class RWTypeRulesManager{
	
	private String cmtypeOperationfilePath = "";
	private ArrayList<RWTypeRule> definedCMTypeRules = new ArrayList<RWTypeRule>();
	
	public static HashSet<RWTypeRule> ruleSet = new HashSet<RWTypeRule>();
	
	public static String error_type = "error_type";
	
	public static String XMLTag_root = "cmtype_rules";
	public static String XMLTag_type_rule = "cmtype_rule";
	public static String XMLTag_op = "operation";
	public static String XMLTag_attSetOne = "attsetone";
	public static String XMLTag_attSetTwo = "attsettwo";
	public static String XMLTag_resultAttSet = "resultset";
	public static String XMLTag_verfiedStatus = "verified";
	
	public int getRuleCount(){
		return this.definedCMTypeRules.size();
	}
	
	public RWTypeRulesManager(File file){
		this.cmtypeOperationfilePath = file.getAbsolutePath();
		if(!file.exists()){
			this.storeRules();
		}else{
			this.loadRules();
		}
	}
	
	public static void removeDuplicates(File file){
		RWTypeRulesManager manager = new RWTypeRulesManager(file);
		HashSet<RWTypeRule> typeRuleSet = new HashSet<RWTypeRule>();
		typeRuleSet.addAll(manager.definedCMTypeRules);
		manager.definedCMTypeRules.clear();
		manager.definedCMTypeRules.addAll(typeRuleSet);
		manager.storeRules();
	}
	
	public void detectInconsistency(){
		//reserve for future use
	}
	
	public void clear(){
		this.definedCMTypeRules.clear();
	}
	
	public void storeRules(){
		Document document = DocumentHelper.createDocument();
        Element root = document.addElement( RWTypeRulesManager.XMLTag_root );
        
        for (RWTypeRule definedCMTypeRule :definedCMTypeRules){
        	Element typeRuleElement = root.addElement( RWTypeRulesManager.XMLTag_type_rule );
        	typeRuleElement.addElement(RWTypeRulesManager.XMLTag_op).addText(definedCMTypeRule.getOperationName());
        	typeRuleElement.addElement(RWTypeRulesManager.XMLTag_attSetOne).addText(definedCMTypeRule.getCMTypeOneName());
        	typeRuleElement.addElement(RWTypeRulesManager.XMLTag_attSetTwo).addText(definedCMTypeRule.getCMTypeTwoName());
        	typeRuleElement.addElement(RWTypeRulesManager.XMLTag_resultAttSet).addText(definedCMTypeRule.getReturnCMTypeName());
        	typeRuleElement.addElement(RWTypeRulesManager.XMLTag_verfiedStatus).addText(definedCMTypeRule.getVarifiedStatus());
        }
        XMLWriter writer;
		try {
			File file = new File(this.cmtypeOperationfilePath);
			file.delete();
			file.createNewFile();
			writer = new XMLWriter(
			        new FileWriter(file));
            writer.write( document );
            writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadRules(){
		File file = new File(this.cmtypeOperationfilePath);
		ArrayList<File> allFiles = new ArrayList<File>();
		if(file.isFile()){
			allFiles.add(file);
		}
		if(file.isDirectory()){
			for(File temp : file.listFiles()){
				allFiles.add(temp);
			}
		}
		for(File ruleFile : allFiles){
			if(ruleFile.exists()){
		        SAXReader reader = new SAXReader();
		        try {
					Document document = reader.read(ruleFile);
					Element root = document.getRootElement();
			        for ( Iterator i = root.elementIterator(RWTypeRulesManager.XMLTag_type_rule); i.hasNext(); ) {
			            Element typeRuleElement = (Element) i.next();
			            String attSetOne = typeRuleElement.element(RWTypeRulesManager.XMLTag_attSetOne).getText();
			            String attSetTwo = typeRuleElement.element(RWTypeRulesManager.XMLTag_attSetTwo).getText();
			            String operation = typeRuleElement.element(RWTypeRulesManager.XMLTag_op).getText();
			            String resultSet = typeRuleElement.element(RWTypeRulesManager.XMLTag_resultAttSet).getText();
			            String verifiedStatus = typeRuleElement.element(RWTypeRulesManager.XMLTag_verfiedStatus).getText();
			            String[] ruleContents = reorderRuleContents(new String[]{operation, attSetOne, attSetTwo});
			            RWTypeRule newRule =  new RWTypeRule();
			            newRule.setCMTypeOneName(ruleContents[1]);
			            newRule.setCMTypeTwoName(ruleContents[2]);
			            newRule.setOperationName(operation);
			            newRule.setReturnCMTypeName(resultSet);
			            newRule.setTypeRuleCategory(verifiedStatus);
			            this.definedCMTypeRules.add(newRule);
			        }
				} catch (DocumentException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * reorder the sequence of operand one and two; so the amount of rules shall be reduced.  
	 * @param oldRule
	 * @return
	 */
	public static String[] reorderRuleContents(String[] ruleInputs){
		String operatorName = ruleInputs[0];
		String attSetOne = ruleInputs[1];
		String attSetTwo = ruleInputs[2];
		if(operatorName.equalsIgnoreCase(RWTypeRuleCategory.Comparable)||
				operatorName.equalsIgnoreCase(RWTypeRuleCategory.Max)||
				operatorName.equalsIgnoreCase(RWTypeRuleCategory.Min)||
				operatorName.equalsIgnoreCase(RWTypeRuleCategory.Multiplication)||
				operatorName.equalsIgnoreCase(RWTypeRuleCategory.Plus)){
			if(attSetOne.length()>0 && attSetTwo.length()>0){
				if(attSetOne.compareTo(attSetTwo)<0){
					ruleInputs[1] = attSetTwo;
					ruleInputs[2] = attSetOne;
				}
			}	
		}
        return ruleInputs;
	}

	public static RWTypeRulesManager getManagerForCurrentProject(){
		if(ActivePart.getFileOfActiveEditror() != null){
			File file = RWTSystemUtil.getRWTypeRulesFiles(ActivePart.getFileOfActiveEditror().getProject());
			return new RWTypeRulesManager(file);
		}
		return null;
	}
	
	public static RWTypeRulesManager getCandidateRuleManager(String fileName){
		if(ActivePart.getFileOfActiveEditror() != null){
			String ruleFileName = String.valueOf(fileName.hashCode());
			File file = RWTSystemUtil.getCandidateCMTypeRuleFile(ActivePart.getFileOfActiveEditror().getProject(), ruleFileName);
			return new RWTypeRulesManager(file);
		}
		return null;
	}
	
	public void addCMTypeOperation(RWTypeRule cmTypeOperation){
		if(!definedCMTypeRules.contains(cmTypeOperation)){
		// no check on existence, because we care how many times the rules appear
			this.definedCMTypeRules.add(cmTypeOperation);	
		}
	}
	
	public void addCMTypeOperations( ArrayList<RWTypeRule> CMTypeOperations){
		for(RWTypeRule cmTypeOperation: CMTypeOperations){
			addCMTypeOperation(cmTypeOperation);
		}
	}
	
	public void delCMTypeOperation(RWTypeRule cmTypeOperation){
		this.definedCMTypeRules.remove(cmTypeOperation);
	}
	
	public ArrayList<RWTypeRule> getDefinedOperations() {
		return definedCMTypeRules;
	}
	
	public static void ruleUsed(RWTypeRule rule){
		ruleSet.add(rule);
	}
	
	

	
	public String getReturnType(IProject currentProject, String argumentOneCMType, String operationName, String argumentTwoCMType){
		//do units computation, if the result is valid, return the result;
		String unitsCheckingRT = UnitsCheckingUtil.unitsComputation(argumentOneCMType,argumentTwoCMType, operationName);
		if(unitsCheckingRT.length()!=0){
			return unitsCheckingRT;
		}
		if((argumentOneCMType.equals(RWType.TypeLess)&& argumentTwoCMType.startsWith("literal"))
			||
			(argumentTwoCMType.equals(RWType.TypeLess)&& argumentOneCMType.startsWith("literal"))){
			return RWType.TypeLess;
		}
		for(RWTypeRule cmTypeRule: definedCMTypeRules){
			if(cmTypeRule.getVarifiedStatus().equals("v")){
				if(operationName.equals(cmTypeRule.getOperationName())){
					String parameterOneType = cmTypeRule.getCMTypeOneName().trim();
					String parameterTwoType = cmTypeRule.getCMTypeTwoName().trim();
					String returnType = cmTypeRule.getReturnCMTypeName().trim();
					
					String[] ruleContents = reorderRuleContents(new String[]{operationName, argumentOneCMType, argumentTwoCMType});
					argumentOneCMType = ruleContents[1];
					argumentTwoCMType = ruleContents[2];
					//check if follow generic type rule 
					if(parameterOneType.contains(RWType.genericTypeInRules)){
						String tempParaOneType = parameterOneType.replace(RWType.genericTypeInRules, "");
						if(argumentOneCMType.contains(tempParaOneType)){
							if(parameterTwoType.equals(argumentTwoCMType)
									||
									parameterTwoType.length()==0){
								if(returnType.contains(RWType.genericTypeInRules)){
									String tempReturnType = returnType.replace(RWType.genericTypeInRules, "");
									ruleUsed(cmTypeRule);
									return  argumentOneCMType.replace(tempParaOneType, tempReturnType);
								}
							}
							if(parameterTwoType.contains(RWType.genericTypeInRules)){
								//consider using generic approach. to be continued....
							}
						}
					}
					if(parameterOneType.equals(argumentOneCMType.trim())) {
						if(parameterTwoType.equals(argumentTwoCMType.trim())){
							ruleUsed(cmTypeRule);
							return returnType;
						}
						/*
						if(parameterTwoType.contains(CMType.genericType)){
							String tempParaTwoType = parameterTwoType.replace(CMType.genericType, "");
							if(argumentTwoCMType.contains(tempParaTwoType)){
								return cmTypeRule.getReturnCMTypeName();
							}
						}
						*/
					}
//					if(RWTSystemUtil.isSubTypeOf(currentProject, argumentOneCMType, parameterOneType)
//						&& RWTSystemUtil.isSubTypeOf(currentProject, argumentTwoCMType, parameterTwoType)){
//						String thisReturnType = cmTypeOperation.getReturnCMTypeName();
//						if((returnType == null) || (RWTSystemUtil.isSubTypeOf(currentProject,thisReturnType, returnType))){
//							returnType = thisReturnType;
//						}
//					}
				}	
			}
		}
		
		if(argumentOneCMType.equals(argumentTwoCMType)){
			if(!argumentOneCMType.contains(";") && 
					!argumentOneCMType.contains("unit=") && 
					!argumentOneCMType.contains("dimension=")){
				return argumentOneCMType;
			}
		}
		return null;
	}
	
//	public String getReturnType(IProject currentProject, String argumentCMType, String operationName){
//		for(CMTypeRule cmTypeOperation: definedCMTypeRules){
//			if(operationName.equals(cmTypeOperation.getOperationName())){
//				String parameterType = cmTypeOperation.getCMTypeOneName();
//				if(argumentCMType.equals(parameterType)){
//					String thisReturnType = cmTypeOperation.getReturnCMTypeName();
//					return thisReturnType; 
//				}
//			}
//		}
//		return null;
//	}
	
	public void assignmentHandling(){
		for(RWTypeRule cmtypeRule :this.definedCMTypeRules){
				String operandOne = cmtypeRule.getCMTypeOneName();
				String operandTwo = cmtypeRule.getCMTypeTwoName();
				String operatorName = cmtypeRule.getOperationName();
				if((operandTwo.indexOf("@")!=-1) 
						&& (operandOne.indexOf("@")==-1)
						&& (operatorName.equalsIgnoreCase(RWTypeRuleCategory.Assignable)) ){
					for(RWTypeRule cmtypeRuleIn :this.definedCMTypeRules){
						String operandOneIn = cmtypeRuleIn.getCMTypeOneName();
						String operandTwoIn = cmtypeRuleIn.getCMTypeTwoName();
						String resultSet = cmtypeRuleIn.getReturnCMTypeName();
						operandOneIn = operandOneIn.replace(operandTwo, operandOne);
						operandTwoIn = operandTwoIn.replace(operandTwo, operandOne);
						resultSet = resultSet.replace(operandTwo, operandOne);
						cmtypeRuleIn.setCMTypeOneName(operandOneIn);
						cmtypeRuleIn.setCMTypeTwoName(operandTwoIn);
						cmtypeRuleIn.setReturnCMTypeName(resultSet);
						
					}
				}
		}
	}
	
	public void duplicateHanding(){
		HashSet<RWTypeRule> set = new HashSet<RWTypeRule>();
		set.addAll(definedCMTypeRules);
		this.definedCMTypeRules.clear();
		this.definedCMTypeRules.addAll(set);
	}
	
	public static void main(String args[]){
//		CMTypeRule rule1 = new CMTypeRule();
//		rule1.setCMTypeOneName("asdf");
//		rule1.setCMTypeTwoName("gfd");
//		rule1.setOperationName("plus");
//		rule1.setReturnCMTypeName("result");
//		
//		CMTypeRule rule2 = new CMTypeRule();
//		rule2.setCMTypeOneName("asdf2");
//		rule2.setCMTypeTwoName("gfd2");
//		rule2.setOperationName("plus");
//		rule2.setReturnCMTypeName("result2");
//		
//		CMTypeRulesManager manager = new CMTypeRulesManager(new File("E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\CMTypeOperationRuleFile.xml"));
//		manager.addCMTypeOperation(rule1);
//		manager.addCMTypeOperation(rule2);
//		manager.storeRules();
		
//		CMTypeRulesManager manager = new CMTypeRulesManager(new File("E:/Develop/EvaluationCMs/KelpieFlightPlanner/CMTypeRuleFile.xml"));
//		CMTypeRulesManager manager = new CMTypeRulesManager(new File("E:/Develop/EvaluationCMs/KelpieFlightPlanner/CMTypeRuleFile.xml"));
		
		RWTypeRulesManager manager = new RWTypeRulesManager(new File("E:/Develop/EvaluationCMs/KelpieFlightPlanner/"));
		System.out.println(manager.getDefinedOperations().size());
//		CMTypeRulesManager.removeDuplicates(new File("E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\RWType_rules\\CMTypeRuleFile.xml"));
	}
	
}
