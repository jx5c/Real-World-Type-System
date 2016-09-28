package cmtypechecker.test;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cmtypechecker.CMRules.CMTypeRuleCategory;

public class testStringRegex {
	public static void main(String args[]){
		String input = "basic=arc distance;dimension=angle*length;distance_plane=earth_surface;earth_model=spherical;unit=nautical_mile*radians";
		String input2 = "subtraction(multiplication(multiplication(orthographic_projection_inverse_rho_unit_sphere@basic=cosine;dimension=;projection type=spherical orthographic projection;target=center point latitude;unit=)@cosine_orthographic_projection_inverse_c_unit_sphere)@multiplication(multiplication(basic=orthographic projection of earth;convertion from=spherical;direction=south to north through viewpoint;origin=viewpoint with lat and lon;radius of source sphere=1@basic=sine;dimension=;projection type=spherical orthographic projection;target=center point latitude;unit=)@sine_orthographic_projection_inverse_c_unit_sphere))";
		Pattern p = Pattern.compile("([^\\(|\\)|\\@]+)");
		Matcher m = p.matcher(input2);
		
		ArrayList<String> ops = CMTypeRuleCategory.getOpNames();
		if(!input.contains("(")){
			System.out.println(true);
		}
		if(input2.contains("(")){
			System.out.println(m.groupCount());
			while (m.find()) {
				String temp = m.group();
				if(!ops.contains(temp)){
					System.out.println(temp);	
				}
			}
		}
			
		
	}
}
