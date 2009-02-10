package suneido;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SuDecimalTest {
	@Test
	public void test() {
		int n = 1234567890;
		SuDecimal big = new SuDecimal(n);
		assertEquals(big, big);
		assertEquals(big, new SuDecimal(n));
		assertEquals(big, SuInteger.valueOf(n));
		assertEquals(SuInteger.valueOf(n), big);
		String huge = "12345678901234567890.123456789";
		assertEquals(new SuDecimal(huge).toString(), huge);
		assertFalse(new SuDecimal("1.5").equals(SuInteger.valueOf(1)));
		assertEquals("1000", new SuDecimal("1e3").toString());
		assertEquals(".5", new SuDecimal(".5").toString());
	}
}
