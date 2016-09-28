package cmtypechecker.CM;

import java.util.ArrayList;

public class CM_MachineType{

	private String explicationLink = "";
	
	private ArrayList<CMAttribute> machineTypeAttributes = new ArrayList<CMAttribute>();

	public CM_MachineType(){
	}
	
	public CM_MachineType(CM_MachineType existingType){
		if(existingType!=null){
			explicationLink = existingType.explicationLink;
			machineTypeAttributes.addAll(existingType.getSemanticTypeAttributes());
		}
	}
	
	public void addSemanticTypeAtt(CMAttribute newAtt){
		if(!machineTypeAttributes.contains(newAtt)){
			machineTypeAttributes.add(newAtt);
		}
	}
	
	public void addSemanticTypeAtts(ArrayList<CMAttribute> newAtts){
		for(CMAttribute att: newAtts){
			if(!machineTypeAttributes.contains(att)){
				machineTypeAttributes.add(att);
			}
		}
	}

	public ArrayList<CMAttribute> getSemanticTypeAttributes() {
		return machineTypeAttributes;
	}

	public void setSemanticTypeAttributes(
			ArrayList<CMAttribute> semanticTypeAttributes) {
		this.machineTypeAttributes = semanticTypeAttributes;
	}
	
	public String findAttValue(String attName){
		for(CMAttribute att: this.machineTypeAttributes){
			if(att.getAttributeName().equalsIgnoreCase(attName)){
				return att.getAttributeValue();
			}
		}
		return "";
	}

	public String getExplicationLink() {
		return explicationLink;
	}

	public void setExplicationLink(String explicationLink) {
		this.explicationLink = explicationLink;
	}
	
	
}
