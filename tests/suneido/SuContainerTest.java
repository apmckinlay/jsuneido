package suneido;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static suneido.language.Pack.pack;
import static suneido.language.Pack.unpack;
import static suneido.util.Util.bufferToHex;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import suneido.language.Ops;

public class SuContainerTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@Test
	public void canonical() {
		Object[] a = { 100, BigDecimal.valueOf(100), new BigDecimal("1e2") };
		for (Object x : a) {
			SuContainer c = new SuContainer();
			c.put(x, true);
			for (Object y : a)
				assertEquals(true, c.get(y));
			assertTrue(c.delete(x));
		}
	}

	@Test
	public void add_put() {
		SuContainer c = new SuContainer();

		assertEquals(0, c.size());
		assertEquals("#()", c.toString());

		c.add(12);
		assertEquals(1, c.size());
		assertEquals(12, c.get(0));
		assertEquals("#(12)", c.toString());

		c.put("ab", 34);
		assertEquals(2, c.size());
		assertEquals(12, c.get(0));
		assertEquals(34, c.get("ab"));
		assertEquals("#(12, ab: 34)", c.toString());

		c.put(2, "cd");
		assertEquals(3, c.size());
		assertEquals(12, c.get(0));
		assertEquals(34, c.get("ab"));
		assertEquals("cd", c.get(2));
		assertEquals("#(12, 2: 'cd', ab: 34)", c.toString());

		c.put(1, "ef");
		assertEquals(4, c.size());
		assertEquals(12, c.get(0));
		assertEquals(34, c.get("ab"));
		assertEquals("ef", c.get(1));
		assertEquals("#(12, 'ef', 'cd', ab: 34)", c.toString());
	}

	@Test
	public void equals_hash() {
		SuContainer one = new SuContainer();
		SuContainer two = new SuContainer();
		assertEquals(one, two);
		assertEquals(two, one);
		assertEquals(one.hashCode(), two.hashCode());

		one.add(123);
		assert ! one.equals(two);
		assert ! two.equals(one);
		assert one.hashCode() != two.hashCode();

		two.add(123);
		assertEquals(one, two);
		assertEquals(two, one);
		assertEquals(one.hashCode(), two.hashCode());

		one.put("abc", 456);
		assert ! one.equals(two);
		assert ! two.equals(one);
		assert one.hashCode() != two.hashCode();

		two.put("abc", 456);
		assert one.equals(two);
		assert two.equals(one);
		assertEquals(one.hashCode(), two.hashCode());
	}

	@Test
	public void delete() {
		SuContainer c = new SuContainer();
		assertFalse(c.delete(0));
		assertFalse(c.delete(""));
		assert c.size() == 0;
		c.add(1);
		c.put("a", 1);
		assert c.size() == 2;
		assertTrue(c.delete(0));
		assert c.size() == 1;
		assertTrue(c.delete("a"));
		assert c.size() == 0;
	}

	@Test
	public void erase() {
		SuContainer c = new SuContainer();
		assertFalse(c.erase(0));
		assertFalse(c.erase(""));
		assert c.size() == 0;
		c.add(11);
		c.add(22);
		c.add(33);
		c.put("a", 1);
		assert c.size() == 4;
		assertTrue(c.erase(1));
		assert c.size() == 3;
		assertEquals(2, c.mapSize());
		assertEquals(1, c.vecSize());
		assertTrue(c.erase("a"));
		assert c.size() == 2;
	}

	@Test
	public void test_pack() {
		SuContainer c = new SuContainer();
		assertEquals(c, unpack(pack(c)));

		c.add(1);
		assertEquals(c, unpack(pack(c)));

		c.put("", true);
		assertEquals(c, unpack(pack(c)));

		for (int i = 0; i < 5; ++i)
			c.add(i);
		assertEquals(c, unpack(pack(c)));

		for (int i = 100; i < 105; ++i)
			c.put(i, i);
		assertEquals(c, unpack(pack(c)));

		SuContainer nested = new SuContainer();
		nested.add(1);
		c.add(nested);
		c.put(999, nested);
		assertEquals(c, unpack(pack(c)));

		SuContainer list = new SuContainer();
		list.add("nextfield");
		list.add("nrows");
		list.add("table");
		list.add("tablename");
		list.add("totalsize");
		ByteBuffer buf = pack(list);
		assertEquals("06800000058000000a046e6578746669656c6480000006046e726f777380000006047461626c658000000a047461626c656e616d658000000a04746f74616c73697a6580000000", bufferToHex(buf).replace(" ", ""));
	}

	@Test(expected = RuntimeException.class)
	public void packNest() {
		SuContainer c = new SuContainer();
		c.add(c);
		c.packSize();
	}

	@Test(expected = RuntimeException.class)
	public void hashCodeNest() {
		SuContainer c = new SuContainer();
		c.add(c);
		c.hashCode();
	}

	@Test
	public void escaping() {
		SuContainer c = new SuContainer();
		String[] strings = new String[] { "plain", "single's", "double\"s", "back\\slash" };
		for (String s : strings)
			c.add(s);
		for (String s : strings)
			c.put(s, true);
		assertEquals("#('plain', \"single's\", 'double\"s', `back\\slash`, " +
				"`back\\slash`: true, 'double\"s': true, \"single's\": true, plain: true)",
				c.toString());
	}

}
