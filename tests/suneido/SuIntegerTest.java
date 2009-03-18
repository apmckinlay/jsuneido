package suneido;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SuIntegerTest {
	static SuInteger si = SuInteger.valueOf(123);

	@Test
	public void integer() {
		assertEquals(si.integer(), 123);
	}

	@Test(expected=SuException.class)
	public void get() {
		si.get(SuInteger.ZERO);
	}

	@Test(expected=SuException.class)
	public void put() {
		si.put(SuInteger.ZERO, SuInteger.ZERO);
	}

	@Test
	public void equals() {
		assertEquals(si, SuInteger.valueOf(123));
	}
}
