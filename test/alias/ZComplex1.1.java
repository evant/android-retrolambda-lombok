package example;

import java.util.*;
import static java.util.Collections.sort;
import java.io.File;

public class ZComplex1 {
	public void test() {
		java.lang.String x = null;
		int z = 0x20;
		float a = 1.234F;
		java.util.List<String>[] q;
	}
}

class ZComplex1Extra {
	{
		int x = 10 + 20 + 30;
		if (x >>> 2 < 10) {
			System.out.println("Whoa!");
		} else System.exit(0);
		if (x >>> 2 < 10) {
			System.out.println("Whoa!");
		}
		int y = (10 + 20) + 30;
		int z = ((10 + 20) + 30);
		int a = 10 + (20 + 30);
		int b = 10 + 20 * 30;
		int c = (10 + 20) * 30;
		boolean d = ("" + b) instanceof CharSequence;
		boolean g = a < 0;
		boolean e = a > 0 ? true : b < 0 ? true : false;
		boolean f = (a > 0 ? true : b < 0) ? true : false;
		
		y++;
		
		for (  int i = 0, j=1  ; i < 10; i++, j++ ) for (String name : Collections.<String>emptyList()) {
			System.out.println(i);
		}
		
		try {
			something();
		} catch (Exception ex) {
			somethingElse();
		} catch (java.lang.Throwable ex) {
			somethingElseEntirely();
		} finally {
			nothing();
		}
	}
	
	void something() {
	}
	
	void somethingElse()
	{}
	
	void somethingElseEntirely() {}
	
	void nothing() {
		
	}
	
	static {
		int y = 20;
	}
}
