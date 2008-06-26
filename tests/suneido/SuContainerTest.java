package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuContainerTest {
	@Test
	public void add_put() {
		SuContainer c = new SuContainer();
		SuValue[] i = { SuInteger.valueOf(12), SuInteger.valueOf(34),
			SuInteger.valueOf(56), SuInteger.valueOf(78) };
		SuValue[] s = { new SuString("ab"), new SuString("cd"),
			new SuString("ef"), new SuString("gh") };
		
		assertEquals(0, c.size());
		assertEquals("[]", c.toString());
		
		c.append(i[0]);
		assertEquals(1, c.size());
		assertEquals(i[0], c.getdata(SuInteger.ZERO));
		assertEquals("[12]", c.toString());
		
		c.putdata(s[0], i[1]);
		assertEquals(2, c.size());
		assertEquals(i[0], c.getdata(SuInteger.ZERO));
		assertEquals(i[1], c.getdata(s[0]));
		assertEquals("[12, ab: 34]", c.toString());
		
		c.putdata(SuInteger.valueOf(2), s[1]);
		assertEquals(3, c.size());
		assertEquals(i[0], c.getdata(SuInteger.ZERO));
		assertEquals(i[1], c.getdata(s[0]));
		assertEquals(s[1], c.getdata(SuInteger.valueOf(2)));
		assertEquals("[12, 2: 'cd', ab: 34]", c.toString());
		
		c.putdata(SuInteger.ONE, s[2]);
		assertEquals(4, c.size());
		assertEquals(i[0], c.getdata(SuInteger.ZERO));
		assertEquals(i[1], c.getdata(s[0]));
		assertEquals(s[2], c.getdata(SuInteger.ONE));
		assertEquals("[12, 'ef', 'cd', ab: 34]", c.toString());
	}
	
	@Test
	public void equals_hash() {
		SuContainer one = new SuContainer();
		SuContainer two = new SuContainer();
		assertEquals(one, two);
		assertEquals(two, one);
		assertEquals(one.hashCode(), two.hashCode());
		
		one.append(SuInteger.valueOf(123));
		assert ! one.equals(two);
		assert ! two.equals(one);
		assert one.hashCode() != two.hashCode();
		
		two.append(SuInteger.valueOf(123));
		assertEquals(one, two);
		assertEquals(two, one);
		assertEquals(one.hashCode(), two.hashCode());
		
		one.putdata(new SuString("abc"), SuInteger.valueOf(456));
		assert ! one.equals(two);
		assert ! two.equals(one);
		assert one.hashCode() != two.hashCode();
		
		two.putdata(new SuString("abc"), SuInteger.valueOf(456));
		assert one.equals(two);
		assert two.equals(one);
		assertEquals(one.hashCode(), two.hashCode());
	}
	
	@Test(expected=SuException.class)
	public void integer() {
		new SuContainer().integer();
	}
	
	@Test
	public void erase() {
		SuContainer c = new SuContainer();
		assertFalse(c.erase(SuInteger.ZERO));
		assertFalse(c.erase(SuString.EMPTY));
		assert c.size() == 0;
		c.append(SuInteger.ONE);
		c.putdata(new SuString("a"), SuInteger.ONE);
		assert c.size() == 2;
		assertTrue(c.erase(SuInteger.ZERO));
		assert c.size() == 1;
		assertTrue(c.erase(new SuString("a")));
		assert c.size() == 0;
	}
}
