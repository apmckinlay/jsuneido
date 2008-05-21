package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuDateTest {
	@Test
	public void test() {
		String s = "#20080514.143622123";
		SuDate d = new SuDate(s);
		assertEquals(s, d.toString());
	}
	
	@Test(expected=SuException.class)
	public void parseError() {
		new SuDate("abc");
	}
	
	@Test
	public void equals_test() {
		SuDate x = new SuDate("#20080514.143622123");
		SuDate y = new SuDate("#20080514.143622123");
		assertTrue(x.equals(x));
		assertTrue(x.equals(y));
		assertTrue(y.equals(x));
		assertFalse(x.equals(SuString.EMPTY));
	}
}
