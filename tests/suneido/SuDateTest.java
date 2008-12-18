package suneido;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

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

	@Test
	public void pack() {
		SuDate d = SuDate.literal("20010203");
		ByteBuffer buf = d.pack();
		SuDate e = (SuDate) SuValue.unpack(buf);
		assertEquals(d, e);
	}

	@Test
	public void compare() {
		SuDate d1 = SuDate.literal("#20081215");
		SuDate d2 = SuDate.literal("#20081216.153244828");
		assert (d1.compareTo(d2) < 0);
		assert (d2.compareTo(d1) > 0);
		ByteBuffer b1 = d1.pack();
		ByteBuffer b2 = d2.pack();
		assert (b1.compareTo(b2) < 0);
		assert (b2.compareTo(b1) > 0);
	}

}
