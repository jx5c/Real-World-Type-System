package cmtypechecker.test;

/**
 * @CMTYPE_wid width
 * @CMTYPE_len Length
 */
public class sample {
	private int len;
	private int wid;
	private int perimeter;
	private int area;
	
	public int product(int x, int y){
		
		return (int) (new Element().alpha*x*y);
	}
	
	public int sum(int x, int y){
		return x+y;
	}
	
	void demo(int l, int w){
		len = l;
		wid = w;
		perimeter = len + wid + len + wid;
		
		int t = 3;
		int acc;
		area = len * wid;
		len = this.sum(len, wid);
		area = product(len, wid);
		len = sum (len, area);
		len = len + area;
		acc = len / (t * t);
		
	}
	
	public static void main(String args[]){
		System.out.println("sdfsdf*sdfsd".split("\\*")[0]);
		int x = 0;
		int y = 0;
		y = x+y;
	}
}
