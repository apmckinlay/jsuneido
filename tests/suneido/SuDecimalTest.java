package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

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
	}
}
