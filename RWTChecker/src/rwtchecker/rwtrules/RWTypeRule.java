package rwtchecker.rwtrules;

public class RWTypeRule{
	
	private static final long serialVersionUID = -1036311609436632588L;
	private String attSetOne = "";
	private String attSetTwo = "";
	private String resultAttSet = "";
	private String operationName = "";
	//type category: verified(v) (which are generated by users or verified by users) and unverified (uv) 
	public static String Verified = "v"; //created by users
	public static String notVerified = "nv"; //created  by automatic generation
	private String typeRuleCategory = notVerified;
	
	/*
	public CMTypeRule(String operationName, String cMTypeOneName, String cMTypeTwoName,
			String returnCMTypeName) {
		super();
		CMTypeOneName = cMTypeOneName;
		CMTypeTwoName = cMTypeTwoName;
		this.returnCMTypeName = returnCMTypeName;
		this.operationName = operationName;
	}
	*/
	
	public RWTypeRule(String operationName, String attSetOne, String attSetTwo, 
			String returnAttSet, String typeRuleCategory) {
		super();
		this.attSetOne = attSetOne;
		this.attSetTwo = attSetTwo;
		this.resultAttSet = returnAttSet;
		this.operationName = operationName;
		this.typeRuleCategory = typeRuleCategory;
	}
	
	public RWTypeRule() {
	}
	
	public static RWTypeRule constructOpFromString(String ruleString){
		String[] contents = ruleString.split(" ; ");
		RWTypeRule op = new RWTypeRule(contents[1], contents[2], contents[3], contents[4], contents[0]);
		return op;
	}

	public String getCMTypeOneName() {
		return attSetOne;
	}
	public void setCMTypeOneName(String cMTypeOneName) {
		attSetOne = cMTypeOneName;
	}
	public String getCMTypeTwoName() {
		return attSetTwo;
	}
	public void setCMTypeTwoName(String cMTypeTwoName) {
		attSetTwo = cMTypeTwoName;
	}
	public String getReturnCMTypeName() {
		return resultAttSet;
	}
	public void setReturnCMTypeName(String returnCMTypeName) {
		this.resultAttSet = returnCMTypeName;
	}
	public String getOperationName() {
		return operationName;
	}
	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}
	public String toString(){
		return this.typeRuleCategory + ";" + 
		       this.operationName + ";" + 
		       this.attSetOne + ";" + 
		       this.attSetTwo + ";" + 
		       this.resultAttSet;
	}
	
	public boolean equals(Object obj){
		if(obj instanceof RWTypeRule){
			if(((RWTypeRule)obj).toString().equals(this.toString())){
				return true;
			}
		}
		return false;
	}

	public String getVarifiedStatus() {
		return typeRuleCategory;
	}

	public void setTypeRuleCategory(String varifiedStatus) {
		this.typeRuleCategory = varifiedStatus;
	}		
	
	public static void main(String args[]){
		System.out.println("jianjian  asdfasdf asdfasdf \n sadf ");
	}
}
