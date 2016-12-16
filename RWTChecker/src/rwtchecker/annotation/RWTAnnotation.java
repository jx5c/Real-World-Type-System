package rwtchecker.annotation;


public class RWTAnnotation{
	
	public static String Define = "def";
	
	public static String Constant = "constant";

	public static String tagNameForAnnotation = "@CM";

	public static String cmTypeForAnnotation = "cmt";

	public static String Return = "return";
	
	public static String Invariant = "inv";
	
	public static String unitsAtt = "#units";
	
	private String annotationType = "";
	
	private String annotationContents = "";

	public String getAnnotationType() {
		return annotationType;
	}

	public void setAnnotationType(String annotationType) {
		this.annotationType = annotationType;
	}

	public String getAnnotationContents() {
		return annotationContents;
	}

	public void setAnnotationContents(String annotationContents) {
		this.annotationContents = annotationContents;
	} 	
	
	public String toString(){
		return "annotation type is:" + this.annotationType + "annotation contents are "+this.annotationContents;
	}
	
	public boolean equals(Object object){
		if(object instanceof RWTAnnotation){
			String annotationContents = ((RWTAnnotation)object).getAnnotationContents();
			if(this.annotationType.equals(((RWTAnnotation)object).getAnnotationType())){
				if(this.annotationType.equals(RWTAnnotation.Define)){
					if(this.annotationContents.split("=")[0].equals(annotationContents.split("=")[0])){
						return true;
					}
				}else if(this.annotationType.equals(RWTAnnotation.Return)){
					return true;
				}
			}
		}
		return false;
	}
}
