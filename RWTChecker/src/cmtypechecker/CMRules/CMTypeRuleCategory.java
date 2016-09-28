package cmtypechecker.CMRules;

import java.util.ArrayList;

public class CMTypeRuleCategory {
	
	private String name = "";
	private String description = "";
	private int parameterCourt = 2;	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public int getParameterCourt() {
		return parameterCourt;
	}
	public void setParameterCourt(int parameterCourt) {
		this.parameterCourt = parameterCourt;
	}

//	public static final String java.lang.Math.
	
	
	public static final String Plus = "plus";
	public static final String Subtraction = "subtraction";
	public static final String Multiplication = "multiplication";
	public static final String Division = "division";
	public static final String REMAINDER = "reminder";
	
	public static final String Comparable = "comparable";
	public static final String Assignable = "assignable";
	
	public static final String Unary_minus = "unary_minus";
	public static final String Unary_plus = "unary_plus"; 
	
	
	//java.lang.Math
	public static final String Power = "java.lang.Math.pow";
	public static final String Natural_Logarithm = "java.lang.Math.log";
//	public static final String Euler_Exponential = "Euler_Exponential";  
	public static final String Abosolute_Value = "java.lang.Math.abs";
	public static final String Arc_Cosine  = "java.lang.Math.acos";
	public static final String Round = "java.lang.Math.round";
	public static final String Arc_Sine  = "java.lang.Math.asin";
	public static final String Arc_Tangent= "java.lang.Math.atan";
	public static final String Arc_Tangent2= "java.lang.Math.atan2";
	public static final String Ceil = "java.lang.Math.ceil";
	public static final String Cosine = "java.lang.Math.cos";
	public static final String Exp = "java.lang.Math.exp";
	public static final String Floor = "java.lang.Math.floor";
	public static final String Max = "java.lang.Math.max";
	public static final String Min = "java.lang.Math.min";
	public static final String Sine = "java.lang.Math.sin";
	public static final String Sqrt = "java.lang.Math.sqrt";
	public static final String Tangent = "java.lang.Math.tan";
	public static final String RadiansToDegree = "java.lang.Math.toDegrees";
	public static final String DegreeToRadians = "java.lang.Math.toRadians";
	
	public static final String TypeContext = "typeContext";
	public static final String subType = "subType";
	
	public static final String setUnion = "union";
	
	public static final String Multiplicative_Inverse = "multiplicative_inverse";
	
//	public static final String AllowLiteralOperations = "Allow_Literal_Operations";
	
	public static ArrayList<String> getOpNames(){
		ArrayList<String> ops = new ArrayList<String>();
		ops.add(Plus);
		ops.add(Subtraction);
		ops.add(Multiplication);
		ops.add(Division);
		ops.add(REMAINDER);
		ops.add(Comparable);
		ops.add(Assignable);
		ops.add(Unary_minus);
		ops.add(Unary_plus);
		ops.add(Power);
		ops.add(Natural_Logarithm);
		ops.add(Abosolute_Value);
		ops.add(Arc_Cosine);
		ops.add(Arc_Tangent);
		ops.add(Arc_Tangent2);
		ops.add(Ceil);
		ops.add(Cosine);
		ops.add(Exp);
		ops.add(Tangent);
		ops.add(Sine);
		ops.add(Max);
		ops.add(Min);
		ops.add(Exp);
		ops.add(Sqrt);
		ops.add(RadiansToDegree);
		ops.add(DegreeToRadians);
		ops.add(Multiplicative_Inverse);
		return ops;
	}
	
	public static ArrayList<CMTypeRuleCategory> getDefaultOperationList(){
		ArrayList<CMTypeRuleCategory> operationTypeList = new ArrayList<CMTypeRuleCategory>();
		
		CMTypeRuleCategory op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Plus);
		op.setDescription("operand one and two are the two operands of the operation");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Subtraction);
		op.setDescription("operand one is the minuend and operand two are the subtrahend");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Multiplication);
		op.setDescription("both operands are factors");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Division);
		op.setDescription("operand one is the dividend, operand two is the divisor");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Power);
		op.setDescription("operand one is the base, operand two is exponent");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Natural_Logarithm);
		op.setDescription("operand is the number to be calculated in Natural Logarithm");
		op.setParameterCourt(1);
		operationTypeList.add(op);
		
		//math types in Java.util.math
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Abosolute_Value);
		op.setParameterCourt(1);
		op.setDescription("Returns the absolute value of a double value.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Arc_Cosine);
		op.setParameterCourt(1);
		op.setDescription("Returns the arc cosine of an angle, in the range of 0.0 through pi.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Arc_Sine);
		op.setParameterCourt(1);
		op.setDescription("Returns the arc sine of an angle, in the range of -pi/2 through pi/2.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Arc_Tangent);
		op.setParameterCourt(1);
		op.setDescription("Returns the arc tangent of an angle, in the range of -pi/2 through pi/2.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Arc_Tangent2);
		op.setParameterCourt(2);
		op.setDescription("Converts rectangular coordinates (x, y) to polar (r, theta).");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Exp);
		op.setParameterCourt(1);
		op.setDescription("Returns Euler's number e raised to the power of the argument.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Cosine);
		op.setParameterCourt(1);
		op.setDescription("Returns the trigonometric cosine of an angle.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Sine);
		op.setParameterCourt(1);
		op.setDescription("Returns the trigonometric sine of an angle.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Tangent);
		op.setParameterCourt(1);
		op.setDescription("Returns the trigonometric tangent of an angle.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Sqrt);
		op.setParameterCourt(1);
		op.setDescription("Returns the correctly rounded positive square root of an angle.");
		operationTypeList.add(op);

		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.RadiansToDegree);
		op.setParameterCourt(1);
		op.setDescription("Converts an angle measured in radians to an approximately equivalent angle measured in degrees.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.DegreeToRadians);
		op.setParameterCourt(1);
		op.setDescription("Converts an angle measured in degrees to an approximately equivalent angle measured in radians.");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.Multiplicative_Inverse);
		op.setParameterCourt(1);
		op.setDescription("Converts an angle measured in degrees to an approximately equivalent angle measured in radians.");
		operationTypeList.add(op);
		
//		op = new OperationTypeForCMTypes();
//		op.setName(OperationTypeForCMTypes.AllowLiteralOperations);
//		op.setParameterCourt(1);
//		op.setDescription("Allow the type in Operand one to be calculated with integer and float point literals");
//		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.TypeContext);
		op.setParameterCourt(2);
		op.setDescription("define the relatonship between types");
		operationTypeList.add(op);
		
		op = new CMTypeRuleCategory();
		op.setName(CMTypeRuleCategory.subType);
		op.setParameterCourt(2);
		op.setDescription("define the relatonship between types");
		operationTypeList.add(op);
		
		return operationTypeList;
	}
	
}
	

