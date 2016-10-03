package rwtchecker.concept;

import java.io.Serializable;
import java.util.ArrayList;

public class ConceptAttribute  implements Serializable{
		private static final long serialVersionUID = -965723210545660875L;
		private String attributeName = "";
		private String attributeExplanation = "";
		private ArrayList<String> candidateValues = new ArrayList<String>();
		
		public ConceptAttribute(){
		}
		public ConceptAttribute(String attributeName, String attributeExplanation) {
			super();
			this.attributeName = attributeName;
			this.attributeExplanation = attributeExplanation;
		}
		public String getAttributeName() {
			return attributeName;
		}
		public void setAttributeName(String attributeName) {
			this.attributeName = attributeName;
		}
		public String getAttributeExplanation() {
			return attributeExplanation;
		}
		public void setAttributeExplanation(String attributeExplanation) {
			this.attributeExplanation = attributeExplanation;
		}		
		public String toString(){
			return attributeName + attributeExplanation;
		}
		public void addCandidateValues(String candidateValue){
			this.candidateValues.add(candidateValue);
		}
		public void removeCandidateValues(String candidateValue){
			this.candidateValues.remove(candidateValue);
		}
		public ArrayList<String> getCandidateValues() {
			return candidateValues;
		}
		public void setCandidateValues(ArrayList<String> candidateValues) {
			this.candidateValues = candidateValues;
		}
		
		public boolean equals(Object conceptAtt){
			if(conceptAtt instanceof ConceptAttribute){
				if (this.attributeName.equals(((ConceptAttribute)conceptAtt).getAttributeName())){
					return true;
				}
			}
				return false;
		}
	}
