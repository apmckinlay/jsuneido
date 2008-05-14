package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuStringTest {
	static SuString ss = new SuString("hello world");
	
	@Test
	public void is_number() {
		assertFalse(ss.is_numeric());
	}
	
	@Test
	public void integer() {
		String[] cases = {"0", "1", "123", "0x0", "010" };
		int results[] = { 0, 1, 123, 0, 8 };
		for (int i = 0; i < cases.length; ++i)
			assertEquals(new SuString(cases[i]).integer(), results[i]);
	}
	
	@Test
	public void get_valid() {
		assertEquals(ss.getdata(SuInteger.ZERO), new SuString("h"));
	}
	
	@Test(expected=SuException.class)
	public void get_invalid() {
		ss.getdata(ss);
	}
	
	@Test(expected=SuException.class)
	public void put() {
		ss.putdata(SuInteger.ZERO, SuInteger.ZERO);
	}
	
	@Test
	public void equals() {
		assertEquals(ss, new SuString("hello world"));
	}
	
	@Test(expected=SuException.class)
	public void numberbad() {
		new SuString("1a").number();
	}
	
	@Test
	public void numbergood() {
		String[] cases = {"0", "1", "123", "-456", "123.456" };
		for (String c : cases) {
			assertEquals(new SuString(c).number(), new SuNumber(c));
		}
	}
}

