package suneido;

import static org.junit.Assert.*;
import static suneido.language.Pack.pack;
import static suneido.language.Pack.unpack;
import static suneido.util.Util.bufferToHex;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import org.junit.Test;

public class SuContainerTest {
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

		c.append(12);
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

		one.append(123);
		assert ! one.equals(two);
		assert ! two.equals(one);
		assert one.hashCode() != two.hashCode();

		two.append(123);
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
		c.append(1);
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
		c.append(11);
		c.append(22);
		c.append(33);
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

		c.append(1);
		assertEquals(c, unpack(pack(c)));

		c.put("", true);
		assertEquals(c, unpack(pack(c)));

		for (int i = 0; i < 5; ++i)
			c.append(i);
		assertEquals(c, unpack(pack(c)));

		for (int i = 100; i < 105; ++i)
			c.put(i, i);
		assertEquals(c, unpack(pack(c)));

		SuContainer nested = new SuContainer();
		nested.append(1);
		c.append(nested);
		c.put(999, nested);
		assertEquals(c, unpack(pack(c)));

		SuContainer list = new SuContainer();
		list.append("nextfield");
		list.append("nrows");
		list.append("table");
		list.append("tablename");
		list.append("totalsize");
		ByteBuffer buf = pack(list);
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
