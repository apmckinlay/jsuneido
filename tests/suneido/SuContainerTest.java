package suneido;

import static org.junit.Assert.*;
import static suneido.Util.bufferToHex;

import java.nio.ByteBuffer;

import org.junit.Test;

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
		assertEquals(i[0], c.get(SuInteger.ZERO));
		assertEquals("[12]", c.toString());

		c.put(s[0], i[1]);
		assertEquals(2, c.size());
		assertEquals(i[0], c.get(SuInteger.ZERO));
		assertEquals(i[1], c.get(s[0]));
		assertEquals("[12, ab: 34]", c.toString());

		c.put(SuInteger.valueOf(2), s[1]);
		assertEquals(3, c.size());
		assertEquals(i[0], c.get(SuInteger.ZERO));
		assertEquals(i[1], c.get(s[0]));
		assertEquals(s[1], c.get(SuInteger.valueOf(2)));
		assertEquals("[12, 2: 'cd', ab: 34]", c.toString());

		c.put(SuInteger.ONE, s[2]);
		assertEquals(4, c.size());
		assertEquals(i[0], c.get(SuInteger.ZERO));
		assertEquals(i[1], c.get(s[0]));
		assertEquals(s[2], c.get(SuInteger.ONE));
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

		one.put(new SuString("abc"), SuInteger.valueOf(456));
		assert ! one.equals(two);
		assert ! two.equals(one);
		assert one.hashCode() != two.hashCode();

		two.put(new SuString("abc"), SuInteger.valueOf(456));
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
		c.put(new SuString("a"), SuInteger.ONE);
		assert c.size() == 2;
		assertTrue(c.erase(SuInteger.ZERO));
		assert c.size() == 1;
		assertTrue(c.erase(new SuString("a")));
		assert c.size() == 0;
	}

	@Test
	public void pack() {
		SuContainer c = new SuContainer();
		assertEquals(c, SuValue.unpack(c.pack()));

		c.append(SuDecimal.ONE);
		assertEquals(c, SuValue.unpack(c.pack()));

		c.put(SuString.EMPTY, SuBoolean.TRUE);
		assertEquals(c, SuValue.unpack(c.pack()));

		for (int i = 0; i < 5; ++i)
			c.append(SuInteger.valueOf(i));
		assertEquals(c, SuValue.unpack(c.pack()));

		for (int i = 100; i < 105; ++i)
			c.put(SuInteger.valueOf(i), SuInteger.valueOf(i));
		assertEquals(c, SuValue.unpack(c.pack()));

		SuContainer nested = new SuContainer();
		nested.append(SuDecimal.ONE);
		c.append(nested);
		c.put(SuInteger.valueOf(999), nested);
		assertEquals(c, SuValue.unpack(c.pack()));

		SuContainer list = new SuContainer();
		list.append(SuString.valueOf("nextfield"));
		list.append(SuString.valueOf("nrows"));
		list.append(SuString.valueOf("table"));
		list.append(SuString.valueOf("tablename"));
		list.append(SuString.valueOf("totalsize"));
		ByteBuffer buf = list.pack();
		assertEquals("06800000058000000a046e6578746669656c6480000006046e726f777380000006047461626c658000000a047461626c656e616d658000000a04746f74616c73697a6580000000", bufferToHex(buf).replace(" ", ""));
	}

	@Test(expected = SuException.class)
	public void packNest() {
		SuContainer c = new SuContainer();
		c.append(c);
		c.packSize();
	}

	@Test(expected = SuException.class)
	public void hashCodeNest() {
		SuContainer c = new SuContainer();
		c.append(c);
		c.hashCode();
	}

}
