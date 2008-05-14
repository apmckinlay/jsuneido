package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuClassTest extends SuClass {
	public SuValue invoke2(int method, SuValue[] args) {
		switch (method) {
		case 1234:
			return method1(args);
		case 5678:
			return method2(args);
		default:
			return super.invoke2(method, args);
		}
	}
	public SuValue method1(SuValue[] args) {
		return SuString.EMPTY;
	}
	public SuValue method2(SuValue[] args) {
		return SuInteger.ZERO;
	}
	
	@Test
	public void test() {
		SuValue c = new SuClassTest();
		assertEquals(SuString.EMPTY, c.invoke(1234));
		assertEquals(SuInteger.ZERO, c.invoke(5678));		
	}
	
	@Test(expected=SuException.class)
	public void unknown() {
		new SuClassTest().invoke(9999);
	}
}
