package suneido;

import static org.junit.Assert.*;
import static suneido.Util.bufferUcompare;

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
		assertNull(SuDate.valueOf(""));
		assertNull(SuDate.valueOf("hello"));
		assertNull(SuDate.valueOf("20010203.123456789x"));
		String[] cases = new String[] { "#20010203", "#20010203.1234",
				"#20010203.123456", "#20010203.123456789" };
		for (String s : cases)
			assertEquals(s, SuDate.valueOf(s).toString());
	}

	@Test
	public void pack() {
		String[] cases = { "20010203", "#20090122.091407890", 
				"#20090122.091423854" };
		for (String s : cases) {
			SuDate d = SuDate.valueOf(s);
			ByteBuffer buf = d.pack();
			SuDate e = (SuDate) SuValue.unpack(buf);
			assertEquals(d, e);
		}
	}

	@Test
	public void compare() {
		SuDate d1 = SuDate.valueOf("#20081215");
		SuDate d2 = SuDate.valueOf("#20081215.133244828");
		assert (d1.compareTo(d2) < 0);
		assert (d2.compareTo(d1) > 0);
		ByteBuffer b1 = d1.pack();
		ByteBuffer b2 = d2.pack();
		assert (bufferUcompare(b1, b2) < 0);
		assert (bufferUcompare(b2, b1) > 0);
	}

}
