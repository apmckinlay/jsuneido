package suneido.util;

import static org.junit.Assert.*;
import static suneido.util.PersistentList.nil;

import org.junit.Test;

public class PersistentListTest {

	@Test
	public void test() {
		PersistentList<String> list = nil();

		assertEquals(0, list.size());
		assertEquals(null, list.head());
		assertEquals(list, list.tail());

		list = list.with("hello");
		assertEquals(1, list.size());
		assertEquals("hello", list.head());
		assertSame(nil(), list.tail());

		list = new PersistentList.Builder<String>()
				.add("hello")
				.add("world")
				.build();
		assertEquals(2, list.size());
		assertEquals("hello", list.head());
		assertEquals("world", list.tail().head());
		assertSame(nil(), list.tail().tail());

		list = PersistentList.of("hello", "world");
		assertEquals(2, list.size());
		assertEquals("hello", list.head());
		assertEquals("hello", list.get(0));
		assertEquals("world", list.tail().head());
		assertEquals("world", list.get(1));
		assertSame(nil(), list.tail().tail());

		list = list.reversed();
		assertEquals(2, list.size());
		assertEquals("world", list.head());
		assertEquals("hello", list.tail().head());
		assertSame(nil(), list.tail().tail());
	}

	@Test
	public void test_without() {
		PersistentList<Integer> list;

		list = PersistentList.nil();
		assertTrue(list.isEmpty());
		assertSame(list, list.without(5));

		list = PersistentList.of(1, 2, 3, 4);
		assertSame(list, list.without(5));
		assertEquals("(1,3,4)", list.without(2).toString());

		list = PersistentList.of(2, 2, 2);
		assertTrue(list.without(2).isEmpty());
	}

}
