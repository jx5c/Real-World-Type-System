package rwtchecker.CM;

import java.io.Serializable;
import java.util.ArrayList;

public class CM_ApproxType implements Serializable{
	
	private static final long serialVersionUID = -7826296647144579348L;
	private ArrayList<CorrespondenceApproTypeProperty> approximateProperties = new ArrayList<CorrespondenceApproTypeProperty>();
	
	public CM_ApproxType(){
	}
	
	public CM_ApproxType(CM_ApproxType existingType){
		if(existingType!= null){
			this.approximateProperties.addAll(existingType.getApproximateProperties());	
		}
	}
	
	public void addProperty (CorrespondenceApproTypeProperty approximateProperty){
		this.approximateProperties.add(approximateProperty);
	}
	
	public ArrayList<CorrespondenceApproTypeProperty> getApproximateProperties() {
		return approximateProperties;
	}

	public void setApproximateProperties(
			ArrayList<CorrespondenceApproTypeProperty> approximateProperties) {
		this.approximateProperties = approximateProperties;
	}
	
	
	
}
