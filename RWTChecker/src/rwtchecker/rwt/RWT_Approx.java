package rwtchecker.rwt;

import java.io.Serializable;
import java.util.ArrayList;

public class RWT_Approx implements Serializable{
	
	private static final long serialVersionUID = -7826296647144579348L;
	private ArrayList<RWT_ApproximationProperty> approximateProperties = new ArrayList<RWT_ApproximationProperty>();
	
	public RWT_Approx(){
	}
	
	public RWT_Approx(RWT_Approx existingType){
		if(existingType!= null){
			this.approximateProperties.addAll(existingType.getApproximateProperties());	
		}
	}
	
	public void addProperty (RWT_ApproximationProperty approximateProperty){
		this.approximateProperties.add(approximateProperty);
	}
	
	public ArrayList<RWT_ApproximationProperty> getApproximateProperties() {
		return approximateProperties;
	}

	public void setApproximateProperties(
			ArrayList<RWT_ApproximationProperty> approximateProperties) {
		this.approximateProperties = approximateProperties;
	}
	
	
	
}
