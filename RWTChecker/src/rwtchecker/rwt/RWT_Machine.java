package rwtchecker.rwt;

import java.util.ArrayList;

public class RWT_Machine{

	private String explicationLink = "";
	
	private ArrayList<RWT_Attribute> machineTypeAttributes = new ArrayList<RWT_Attribute>();

	public RWT_Machine(){
	}
	
	public RWT_Machine(RWT_Machine existingType){
		if(existingType!=null){
			explicationLink = existingType.explicationLink;
			machineTypeAttributes.addAll(existingType.getSemanticTypeAttributes());
		}
	}
	
	public void addSemanticTypeAtt(RWT_Attribute newAtt){
		if(!machineTypeAttributes.contains(newAtt)){
			machineTypeAttributes.add(newAtt);
		}
	}
	
	public void addSemanticTypeAtts(ArrayList<RWT_Attribute> newAtts){
		for(RWT_Attribute att: newAtts){
			if(!machineTypeAttributes.contains(att)){
				machineTypeAttributes.add(att);
			}
		}
	}

	public ArrayList<RWT_Attribute> getSemanticTypeAttributes() {
		return machineTypeAttributes;
	}

	public void setSemanticTypeAttributes(
			ArrayList<RWT_Attribute> semanticTypeAttributes) {
		this.machineTypeAttributes = semanticTypeAttributes;
	}
	
	public String findAttValue(String attName){
		for(RWT_Attribute att: this.machineTypeAttributes){
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
