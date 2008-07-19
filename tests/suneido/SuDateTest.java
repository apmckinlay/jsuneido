package suneido;

import static org.junit.Assert.*;

import org.junit.Test;

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

	@Test
	public void literal() {
		assertNull(SuDate.literal(""));
		assertNull(SuDate.literal("hello"));
		assertNull(SuDate.literal("20010203.123456789x"));
		String[] cases = new String[] { "#20010203", "#20010203.1234",
				"#20010203.123456", "#20010203.123456789" };
		for (String s : cases)
			assertEquals(s, SuDate.literal(s).toString());
	}
}
