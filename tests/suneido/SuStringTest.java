package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuStringTest {
	static SuString ss = new SuString("hello world");
	
	@Test
	public void integer() {
		String[] cases = {"0", "1", "123", "0x0", "0xf", "010" };
		int results[] = { 0, 1, 123, 0, 15, 8 };
		for (int i = 0; i < cases.length; ++i)
			assertEquals(results[i], new SuString(cases[i]).integer());
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
			assertEquals(new SuString(c).number(), new SuDecimal(c));
		}
	}
	
	@Test
	public void getdata() {
		SuString s = new SuString("hello world");
		int[] offsets = { -1, 0, 1, 10, 11, 999 };
		String[] results = { "", "h", "e", "d", "", "" };
		for (int i = 0; i < offsets.length; ++i)
			assertEquals(results[i], s.getdata(SuInteger.valueOf(offsets[i])).string());
	}
	
	@Test
	public void substr() {
		SuValue s = new SuString("hello world");
		assertEquals(new SuString("hello"), 
				s.invoke("Substr", SuInteger.ZERO, SuInteger.valueOf(5)));
		assertEquals(new SuString("world"),
				s.invoke("Substr", SuInteger.valueOf(6)));
	}
}

