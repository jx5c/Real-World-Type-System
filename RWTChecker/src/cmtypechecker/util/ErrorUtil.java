package cmtypechecker.util;

import org.eclipse.jdt.core.dom.Expression;

import cmtypechecker.util.interval.RealInterval;

public class ErrorUtil {
	
	//error in phase zero
	public static String getInfixExpDerivationTypeError(String leftHandType, String rightHandType){
		if(leftHandType.equals("")){
			leftHandType = "Empty Type";
		}
		if(rightHandType.equals("")){
			rightHandType = "Empty Type";
		}
		return "The CM type of "+leftHandType + " is not the same with the CMType of "+ rightHandType;
	}
	
	public static String getDerivationTypeInconsistencyInConditionExpression(String thenDerivation, String elseDerivation){
		if(thenDerivation.equals("")){
			thenDerivation = "Empty Type";
		}
		if(elseDerivation.equals("")){
			elseDerivation = "Empty Type";
		}
		return "inconsistent types between the two sides: then expression derivation is typed: "+thenDerivation+" ; while else expression derivation is typed " + elseDerivation;
	}
	
	public static String getDerivationTypeInconsistency(String leftHandDerivation, String rightHandDerivation){
		if(leftHandDerivation.equals("")){
			leftHandDerivation = "Empty Type";
		}
		if(rightHandDerivation.equals("")){
			rightHandDerivation = "Empty Type";
		}
		return "inconsistent types between the two sides: left hand derivation is typed: "+leftHandDerivation+" ; while right hand derivation is typed " + rightHandDerivation;
	}
	
	public static String getMathArctan2DimensionError(){
		return "The cmtype derivation for the first argument of arctan2 function should be the same as the second argument";
	}
	
	public static String getMathDimensionlessError(){
		return "The argument of the function should be dimensionless";
	}
	
	public static String getRemanderDimensionError(){
		return "The CM types for the remander operation are not applicable";
	}
	public static String getRemanderUniError(){
		return "The units for the remander operation are not applicable";
	}
	
	//error in phase one
	public static String getUnitErrorType(String leftHandUnit, String rightHandUnit){
		return "inconsistent unit types between the two sides: left hand has unit of "+ leftHandUnit +" ; while right hand has unit of "+rightHandUnit;
	}
	
	public static String getUnitInconsistencyInConditionExpression(String thenUnit, String elseUnit){
		if(thenUnit.equals("")){
			thenUnit = "Empty Type";
		}
		if(elseUnit.equals("")){
			elseUnit = "Empty Type";
		}
		return "inconsistent types between the two sides: then expression has unit: "+thenUnit+" ; while else expression has unit " + elseUnit;
	}
	
	public static String getAssignmentUnitError(String leftHandType, String rightHandType){
		return "The unit of left hand "+leftHandType + " is not the same with the unit of right hand "+ rightHandType;
	}
	
	public static String getVariableDeclUnitError(String leftHandType, String rightHandType){
		return "The unit of "+leftHandType + " is not the same with the unit of "+ rightHandType;
	}
	
	public static String getInfixExpUnitError(String leftHandType, String rightHandType){
		return "The unit of "+leftHandType + " is not the same with the unit of "+ rightHandType;
	}
	
	public static String getMethodParameterUnitError(String actualParameterUnit, String annotatedUnits ){
		return "The unit of actual parameter: "+actualParameterUnit + " is not the same with the unit of the annotated type "+annotatedUnits;
	}
	
	public static String getMethodParameterDimensionError(String actualParameterType, String annotatedType){
		return "The derivation of actual parameter: "+actualParameterType + " is not the same with the dimension of the annotated type "+annotatedType;
	}
	
	public static String getMethodReturnDimensionError(String actualReturnTypeDerivation, String annotatedReturnTypeDerivation){
		return "The derivation for CM type of return statement is: "+actualReturnTypeDerivation + " that is not the same with the derivation for CM Type of the annotated return type "+annotatedReturnTypeDerivation;
	}
	
	public static String getMethodReturnUnitError(String actualReturnTypeUnit, String annotatedReturnTypeUnit){
		return "The unit of return statement is: "+actualReturnTypeUnit + " that is not the same with the unit of the annotated return type "+annotatedReturnTypeUnit;
	}
	
	public static String getNoUnitError(String cmType){
		return "The unit of the "+cmType + " is not specified in the CM type definition";
	}
	
	public static String getNoCMTypeExistedError(String cmtypeName){
		return "The CM type with name of "+ cmtypeName +" is not existed;";
	}
	public static String getPhaseZeroInconsistentTypeError(String cmtypeOne, String cmtypeTwo){
		return "The CM type of " + cmtypeOne + " and the CM type of " + cmtypeTwo + " are not coming from the same base type";
	}
	
	public static String getMathExpUnitError(String argumentUnit, String unit){
		return "The argument of EXP function should be unitless, while the " +argumentUnit + " has unit of "+ unit;
	}
	
	public static String getMathMaxMinDimensionError(){
		return "The derivation for the first argument of Max(Min) function should be the same as the second argument";
	}
	
	public static String getMathExpDimensionError(String argumentType, String unit){
		return "The argument of EXP function should be dimensionless, while the " +argumentType + " has unit of "+ unit;
	}
	
	public static String getMathAngleDimensionError(String argumentType){
		if(argumentType.equals("")){
			argumentType = "Empty type";
		}
		return "The dimension of the argument of function should be angle, while it is in fact " +argumentType;
	}
	
	public static String getMathAngleUnitError(String argumentType, String goodUnit, String factUnit){
		return "The unit of the argument of function should be "+ goodUnit + ", while it is in fact " +argumentType + " has unit of "+ factUnit;
	}
	
	public static String getMathLogDimensionError(String argumentType, String unit){
		return "The argument of LOG function should be dimensionless, while the " +argumentType + " has unit of "+ unit;
	}
	public static String getMathLogUnitError(String argumentUnit, String unit){
		return "The argument of LOG function should be unitless, while the " +argumentUnit + " has unit of "+ unit;
	}
	
	public static String getMathPowError(String argumentTwo, String unit){
		return "The index argument of POWER function should be dimensionless and unitless, while the " +argumentTwo + " has unit of "+ unit;
	}

	public static String getMathArctan2UnitError(){
		return "The unit for the first argument of arctan2 function should be the same as the second argument";
	}
	
	public static String getMathMaxMinUnitError(){
		return "The unit for the first argument of Max(Min) function should be the same as the second argument";
	}
	
	public static String getMathUnitlessError(){
		return "The argument of the function should be unitless";
	}
	
	/** errors in phase two*/
	public static String getInconsistAttributeError(String cmtype){
		return "In phase two checking, the value of attribute of this cm type "+cmtype +" is not consistent with the values from other cm types";
	}
	
	public static String getInconsistAttributeErrorInReturn(String actualReturnType,  String annotatedReturnType, String attribute){
		return "The value of attribute "+attribute+" of the actual return cm type "+actualReturnType+" is not consistent with the annotated return cm type "+annotatedReturnType;
	}
	
	public static String getInconsistAttributeErrorForBothSides(String leftType,  String rightType, String attribute){
		return "The value of attribute "+attribute+" of the left cm type "+leftType+" is not consistent with the right hand cm type "+rightType;
	}
	
	/** errors in phase three*/
	/*************************/
	/*************************/
	/*************************/
	/*************************/
	/** errors in phase three*/
	public static String getUndeclaredCalculation(String calculationString){
		return "Unclear type calculation: there is no type rule that permits this calculation "+calculationString;
	}
	
	public static String getMethodReturnCMTypeError(String actualReturnType, String annotatedReturnType){
		return "The CM type of return statement is: "+actualReturnType + " that is not the same with the CM Type of the annotated return type "+annotatedReturnType;
	}
	
	public static String getMathArctan2CMTypeError(){
		return "The CM type for the first argument of arctan2 function should be the same as the second argument";
	}
	
	public static String getMathMaxMinCMTypeError(){
		return "The CMType for the first argument of Max(Min) function should be the same as the second argument";
	}
	
	public static String getMathExpCMTypeError(String argumentCMType){
		return "The argument of EXP function should be CM TypeLess, while the argument has CM type of " +argumentCMType;
	}
	
	public static String getMathLogCMTypeError(String argumentCMType){
		return "The argument of LOG function should be CM TypeLess, while the argument has CM type of " +argumentCMType;
	}
	
	public static String typeConversionError(String leftHandType){
		return "Possible type conversion here, left hand is typed "+leftHandType+"; while right hand type is empty typed;";
	}
	
	public static String unknownLeftType(String rightHandType){
		return "Unknown Type: Left hand type is unknown, right hand is typed "+ rightHandType;
	}
	
	public static String typeInconsistency(String leftHandType, String rightHandType){
		return "Type Inconsistency; left hand is typed "+leftHandType+"; while right hand is typed " +rightHandType;
	}

	public static String unknownCalculation(){
		return "Unclear Calculation: No rule has been defined for this infix expression, the result type is unknown";
	}
	
	public static String getErrorTypeFive(String returnType, String annotatedType){
		return "Inconsistent return types! The real world type for this return value is "+returnType +" while the annotated CM type is "+annotatedType;
	}
	
	public static String getErrorTypeSix(String argument, String argumentType, String annotatedType){
		return "Inconsistent argument type: the argument "+ argument + "is typed "+ argumentType +"; and it is inconsistent with the annotated type "+ annotatedType + " in method declaration";
	}
	
	public static String unknownMathCalculation(String calculationString){
		return "unknown Math calculation: there is no operation rule defined that permits this calculation "+calculationString;
	}
	
	public static String getErrorTypeEight(String leftHandType, String rightHandType){
		return "Possible inappropriate type casting: left hand type: "+leftHandType+" is a subtype of right hand type: " + rightHandType;
	}
	
	public static String getErrorTypeNine(String leftType, String rightType){
		return "Unclear Math calculation: "+"left operand is typed "+ leftType+"; while right operand is typed "+rightType;
	}
	
	public static String getErrorTypeTen(String leftHandType, String rightHandType){
		return "inconsistent types between the two sides: left hand is typed: "+leftHandType+" ; while right hand is typed " + rightHandType;
	}
	
	public static String methodInvocationError(){
		return "the method invocation will cause errors, the method body has errors when invoked with these arguments ";
	}
	public static String methodInvocationInconsistentReturn(){
		return "the method invocation will lead to different return types ";
	}
	public static String methodArgumentError(){
		return "the cmtypes of arguments are conflict with the type used in the method body";
	}
	
	public static String methodArgumentMissingAnnotation(){
		return "The arguments require proper cmtype annotation";
	}
	public static String methodInproperArgumentAnnotation(){
		return "The arguments require more accurate annotation";
	}
	
	public static String methodInvocationReturnInconsistentError(){
		return "The return types in the method invocation with these arguments will be inconsistent";
	}
	
	public static String getInconsistentAttributeError(){
		return "Union of the two sets yield inconsistent attributes";
	}
	
	// for interval arithmetic errors
	public static String inconsistAssignment(RealInterval left, RealInterval right){
		return "The bound for left side: "+left+ "is not fully compatible with the right side:"+right;
	}
	
	public static String possibleOutRangeAssignment(RealInterval left, RealInterval right){
		return "The bound for left side: "+left+ "may not fully compatible with the right side:"+right;
	}
	
	public static String methodIntervalError(){
		return "the range of arguments are not fully compatiable with the range of the parameters";
	}
	
	public static String intvalInconsistency(RealInterval left, RealInterval right){
		return "The bound for left:"+left+ " is not fully compatible with the right:"+right;
	}
	
	public static String NaNWarning(RealInterval intval, Expression exp){
		return "The expression "+exp +" has NaN in its bound: "+intval;
	}
	
	public static String InfinityWarning(RealInterval intval, Expression exp){
		return "The expression "+exp +" has infinity in its bound: "+intval;
	}
	
	public static String divisionOfZero(RealInterval intOne, RealInterval intTwo){
		return "Possible division of zero between "+intOne + " and "+intTwo;
	}
	
	//invariants violation
	public static String invariantViolation(String invariant){
		return "The invariant "+invariant + " is violated.";
	}
	
}
