package rwtchecker.CM;

import java.io.Serializable;

public class CorrespondenceApproTypeProperty  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6273676156972251949L;
	private String property_name;
	private String description;
	private String possibleValue;
	
	public CorrespondenceApproTypeProperty(){
	}
	
	public CorrespondenceApproTypeProperty(String name, String description){
		this.property_name = name;
		this.description = description;
	}

	public String getProperty_name() {
		return property_name;
	}

	public void setProperty_name(String propertyName) {
		property_name = propertyName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPossibleValue() {
		return possibleValue;
	}

	public void setPossibleValue(String possibleValue) {
		this.possibleValue = possibleValue;
	}
	
	public boolean equals(Object attribute){
		if(attribute instanceof CorrespondenceApproTypeProperty ){
			if(((CorrespondenceApproTypeProperty)attribute).getProperty_name().equals(property_name)){
				return true;
			}
		}
		return false;
	}
}
