package cmtypechecker.CM;

import java.util.ArrayList;

public class CM_SemanticType{

	private String explicationLink = "";
	
	private ArrayList<CMAttribute> semanticTypeAttributes = new ArrayList<CMAttribute>();

	public CM_SemanticType(){
	}
	
	public CM_SemanticType(CM_SemanticType existingType){
		if(existingType!=null){
			explicationLink = existingType.explicationLink;
			semanticTypeAttributes.addAll(existingType.getSemanticTypeAttributes());
		}
	}
	
	public void addSemanticTypeAtt(CMAttribute newAtt){
		if(!semanticTypeAttributes.contains(newAtt)){
			semanticTypeAttributes.add(newAtt);
		}
	}
	
	public void addSemanticTypeAtts(ArrayList<CMAttribute> newAtts){
		for(CMAttribute att: newAtts){
			if(!semanticTypeAttributes.contains(att)){
				semanticTypeAttributes.add(att);
			}
		}
	}

	public ArrayList<CMAttribute> getSemanticTypeAttributes() {
		return semanticTypeAttributes;
	}

	public void setSemanticTypeAttributes(
			ArrayList<CMAttribute> semanticTypeAttributes) {
		this.semanticTypeAttributes = semanticTypeAttributes;
	}
	
	public String findAttValue(String attName){
		for(CMAttribute att: this.semanticTypeAttributes){
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
