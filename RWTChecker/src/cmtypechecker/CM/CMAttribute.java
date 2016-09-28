package cmtypechecker.CM;


public class CMAttribute{
	
	private String attributeName = "";
	private String attributeValue = "";
	private String enableStatus = enableMark;
	
	public static String enableMark = "y";
	public static String disEnableMark = "n";
	
	public CMAttribute(String attributeName,
			String attributeValue) {
		super();
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
	}
	
	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	public boolean equals(Object attribute){
		if(attribute instanceof CMAttribute ){
			if(((CMAttribute)attribute).getAttributeName().equals(attributeName)){
				return true;
			}
		}
		return false;
	}
	public String getAttributeValue() {
		return attributeValue;
	}
	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}

	public String getEnableStatus() {
		return enableStatus;
	}

	public void setEnableStatus(String enableStatus) {
		this.enableStatus = enableStatus;
	}
}

