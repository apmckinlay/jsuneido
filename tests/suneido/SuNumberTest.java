package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuNumberTest {
	@Test
	public void test() {
		int n = 1234567890;
		SuNumber big = new SuNumber(n);
		assertEquals(big, big);
		assertEquals(big, new SuNumber(n));
		assertEquals(big, new SuInteger(n));
		assertEquals(new SuInteger(n), big);
		String huge = "12345678901234567890.123456789";
		assertEquals(new SuNumber(huge).toString(), huge);
	}
}
