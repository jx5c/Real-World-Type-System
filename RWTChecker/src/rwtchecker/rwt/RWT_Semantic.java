package rwtchecker.rwt;

import java.util.ArrayList;

public class RWT_Semantic{

	private String explicationLink = "";
	
	private ArrayList<RWT_Attribute> semanticTypeAttributes = new ArrayList<RWT_Attribute>();

	public RWT_Semantic(){
	}
	
	public RWT_Semantic(RWT_Semantic existingType){
		if(existingType!=null){
			explicationLink = existingType.explicationLink;
			semanticTypeAttributes.addAll(existingType.getSemanticTypeAttributes());
		}
	}
	
	public void addSemanticTypeAtt(RWT_Attribute newAtt){
		if(!semanticTypeAttributes.contains(newAtt)){
			semanticTypeAttributes.add(newAtt);
		}
	}
	
	public void addSemanticTypeAtts(ArrayList<RWT_Attribute> newAtts){
		for(RWT_Attribute att: newAtts){
			if(!semanticTypeAttributes.contains(att)){
				semanticTypeAttributes.add(att);
			}
		}
	}

	public ArrayList<RWT_Attribute> getSemanticTypeAttributes() {
		return semanticTypeAttributes;
	}

	public void setSemanticTypeAttributes(
			ArrayList<RWT_Attribute> semanticTypeAttributes) {
		this.semanticTypeAttributes = semanticTypeAttributes;
	}
	
	public String findAttValue(String attName){
		for(RWT_Attribute att: this.semanticTypeAttributes){
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
