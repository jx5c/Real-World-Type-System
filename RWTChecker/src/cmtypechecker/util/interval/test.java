package cmtypechecker.util.interval;

public class test {

	/**
	 * @param args
	 */
	public static void main(String arg[]){
		RealInterval real1 = new RealInterval(-1,1);
		RealInterval real2 = new RealInterval(-1,1);
		RealInterval lat = new RealInterval(-90,90);
		int num = 1;
		System.out.println(IAMath.sin2pi(lat));
		System.out.println(IAMath.div(new RealInterval(1), IAMath.sin2pi(lat)));
		System.out.println(IAMath.acos(real1));
		
		
		
		RealInterval lat1 = new RealInterval(-Math.PI/2,Math.PI/2);
		RealInterval lat2 = new RealInterval(-Math.PI/2,Math.PI/2);
		
		RealInterval lon1 = new RealInterval(-Math.PI,Math.PI);
		RealInterval lon2 = new RealInterval(-Math.PI,Math.PI);
		
		RealInterval dlat = IAMath.sub(lat1, lat2);
		dlat = IAMath.div(dlat, new RealInterval(2,2));
		
		RealInterval dlon = IAMath.sub(lon1, lon2);
		dlon = IAMath.div(dlon, new RealInterval(2,2));
		
		RealInterval a =IAMath.add(IAMath.evenPower(IAMath.sin(dlat),2), IAMath.mul(IAMath.mul(IAMath.cos(lat1),IAMath.cos(lat2)),IAMath.evenPower(IAMath.sin(dlat),2)));  
     
		RealInterval thenIntval = new RealInterval(1.0E-8);
		RealInterval elseIntval = new RealInterval(0);
		
		System.out.println(IAMath.union(thenIntval,elseIntval));
		

	}

}

