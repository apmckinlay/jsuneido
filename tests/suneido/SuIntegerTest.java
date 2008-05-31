package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuIntegerTest {
	static SuInteger si = SuInteger.from(123);
	
	@Test
	public void integer() {
		assertEquals(si.integer(), 123);
	}
	
	@Test(expected=SuException.class)
	public void get() {
		si.getdata(SuInteger.ZERO);
	}
	
	@Test(expected=SuException.class)
	public void put() {
		si.putdata(SuInteger.ZERO, SuInteger.ZERO);
	}
		
	@Test
	public void equals() {
		assertEquals(si, SuInteger.from(123));
	}
}
