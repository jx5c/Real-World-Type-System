package rwtchecker.util;

import rwtchecker.CMRules.CMTypeRuleCategory;

public class UnitsCheckingUtil {

	public static String unitsComputation(String arguOneType, String arguTwoType, String operationName){
		if(arguOneType.indexOf(";")!=-1 || arguTwoType.indexOf(";")!=-1){
			return "";
		}
		if(arguOneType.startsWith("unit") && arguTwoType.startsWith("unit")){
			if(operationName.equals(CMTypeRuleCategory.Plus) 
					|| operationName.equals(CMTypeRuleCategory.Subtraction)){
				if(arguOneType.equals(arguTwoType)){
					return arguOneType; 
				}
			}
			//format unit1:index1|unit2:index2
			if(operationName.equals(CMTypeRuleCategory.Multiplication)){
				//if()
			}
		}
		
		return "";
	}

	public static void main(String[] args) {
		System.out.println(UnitsCheckingUtil.unitsComputation("unit=radians","unit=radians",CMTypeRuleCategory.Plus));
	}

}
