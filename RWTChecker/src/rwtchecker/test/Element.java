package rwtchecker.test;


/**
 * two errors here: 
 * 1. the function radiationLength return the reciprocal of the radiation length
 * 2. the Math.exp(thick/X0) should be Math.exp(thick * density / X0);
 * @author Jian Xiang
 * @CMTYPE_atomicWeight latitude_degree 
 *
 */
public class Element {
	private double atomicWeight;
	private double atomicNumber;
	
	public static double alpha = 1/137.0;
	private static double re = 2.82 * Math.pow(10, -13);
	private static double NA = 6.02 * Math.pow(10, 23);    //unit cm not m
	
	public double getAtomicWeight() {
		return atomicWeight;
	}

	public void setAtomicWeight(double atomicWeight) {
		this.atomicWeight = atomicWeight;
	}

	public double getAtomicNumber() {
		return atomicNumber;
	}

	public void setAtomicNumber(double atomicNumber) {
		this.atomicNumber = atomicNumber;
	}

	public static double radiationLength(Element element){
		double A = element.getAtomicWeight();
		double Z = element.getAtomicNumber();
		double L = Math.log(184.15/Math.pow(Z, 1.0/3));
		System.out.println("L is: "+L);
		double Lp = Math.log(1194.0/Math.pow(Z, 2.0/3));
		System.out.println("Lp/z is: "+Lp/Z);
		return (4.0 * alpha*re*re) *(NA/A) * (Z*Z*L + Z*Lp);
	}
	
	public static double finalEnergy(Element element, double thick, double initEnergy){
		double X0 = radiationLength(element);
			return initEnergy / Math.exp(thick / X0);
	}
	
	public static void main(String args[]){
//		Element element = new Element();
//		element.setAtomicNumber(79);
//		element.setAtomicWeight(197);
//		System.out.println(radiationLength( element));
//		System.out.println(String.valueOf(1.0/3));
//		String d = "1/3";
//		System.out.println(Float.valueOf(d));
		/**
		 * test
		 */
		System.out.println("abcd".lastIndexOf("d"));
		System.out.println("abcd".substring(0, 3));
		int a = 1 + 2 +3 +4 ;
	}
}
