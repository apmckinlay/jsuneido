package suneido.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class PersistentMapTest {

	@Test
	public void test_empty() {
		PersistentMap<String, Integer> map = PersistentMap.empty();
		assertEquals(0, map.size());
		assertEquals(null, map.get("Joe"));
	}

	@Test
	public void test_with() {
		PersistentMap<String, Integer> map = PersistentMap.empty();
		assertEquals(0, map.size());
		map = map.with("Sue", 19);
		assertEquals(1, map.size());
		assertEquals(null, map.get("Joe"));
		assertEquals((Integer) 19, map.get("Sue"));

		map = map.with("Sue", 21);
		assertEquals(1, map.size());
		assertEquals(null, map.get("Joe"));
		assertEquals((Integer) 21, map.get("Sue"));

		map = map.with("Ann", 31);
		assertEquals(2, map.size());
		assertEquals(null, map.get("Joe"));
		assertEquals((Integer) 21, map.get("Sue"));
		assertEquals((Integer) 31, map.get("Ann"));

		assertSame(map, map.with("Ann", 31));
	}

	@Test
	public void test_without() {
		PersistentMap<String, Integer> map = PersistentMap.empty();
		assertSame(map, map.without("Joe"));

		map = map.with("Sue", 21);
		assertSame(map, map.without("Joe"));

		map = map.with("Ann", 31);
		map = map.without("Sue");
		assertEquals(1, map.size());
		assertEquals(null, map.get("Sue"));
		assertEquals((Integer) 31, map.get("Ann"));
	}

	@Test
	public void test_collision() {
		// same low 5 bits but differ in next 5
		final int k1 = 0x100;
		final int k2 = 0x200;
		final int k3 = 0x300;

		PersistentMap<Integer, String> map = PersistentMap.empty();
		map = map.with(k1, "Sue");
		assertSame(map, map.without(k2)); // same slot, different key

		map = PersistentMap.empty();
		map = map.with(k1, "Sue").with(k2, "Ann");
		assertEquals(2, map.size());
		assertEquals("Sue", map.get(k1));
		assertEquals("Ann", map.get(k2));
		assertEquals(null, map.get(k3));

		map = PersistentMap.empty();
		map = map.with(k2, "Ann").with(k1, "Sue").with(k1, "Sue");
		assertEquals(2, map.size());
		assertEquals("Sue", map.get(k1));
		assertEquals("Ann", map.get(k2));

		map = map.with(k3, "Joe");
		assertEquals(3, map.size());
		assertEquals("Sue", map.get(k1));
		assertEquals("Ann", map.get(k2));
		assertEquals("Joe", map.get(k3));

		map = map.without(k2);
		assertEquals(2, map.size());
		assertEquals("Sue", map.get(k1));
		assertEquals("Joe", map.get(k3));
		map = map.without(k1);
		assertEquals(1, map.size());
		assertEquals("Joe", map.get(k3));
		map = map.without(k3);
		assertSame(map, PersistentMap.empty());

		map = PersistentMap.empty();
		map = map.with(0x2000, "Ann").with(0x1000, "Sue").with(0x1000, "Sue");
		map = map.without(0x3000);
		assertEquals(2, map.size());
		assertEquals("Sue", map.get(0x1000));
		assertEquals("Ann", map.get(0x2000));

		map = map.without(0x2000).without(0x1000);
		assertSame(map, PersistentMap.empty());
	}

	@Test
	public void test_sameHash() {
		Long k1 = Long.valueOf(1);
		Long k2 = Long.valueOf(1L << 32);
		Long k3 = Long.valueOf((2L << 32) + 3L);
		assert k1 != k2 && k2 != k3;
		assert k1.hashCode() == k2.hashCode();
		assert k2.hashCode() == k3.hashCode();

		PersistentMap<Long, String> map = PersistentMap.empty();
		map = map.with(k1, "Sue");
		map = map.with(k2, "Ann").with(k2, "Ann").without(k3);
		assertEquals(2, map.size());
		assertEquals("Sue", map.get(k1));
		assertEquals("Ann", map.get(k2));
		assertEquals(null, map.get(k3));
		map = map.with(k3, "Jim").with(k3, "Joe");
		assertEquals(3, map.size());
		assertEquals("Sue", map.get(k1));
		assertEquals("Ann", map.get(k2));
		assertEquals("Joe", map.get(k3));

		map = map.without(k3).without(k1).without(k2);
		assertSame(map, PersistentMap.empty());
	}

}
