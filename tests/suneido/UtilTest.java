package suneido;

import static org.junit.Assert.*;
import static suneido.Util.list;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UtilTest {
	@Test
	public void listToParens() {
		assertEquals("()", Util.listToParens(null));
		List<String> list = new ArrayList<String>();
		assertEquals("()", Util.listToParens(list));
		list.add("one");
		assertEquals("(one)", Util.listToParens(list));
		list.add("two");
		assertEquals("(one,two)", Util.listToParens(list));
		list.add("three");
		assertEquals("(one,two,three)", Util.listToParens(list));
	}

	@Test
	public void commasToList() {
		assertTrue(Util.commasToList("").isEmpty());
		List<String> list = new ArrayList<String>();
		list.add("abc");
		assertEquals(list, Util.commasToList("abc"));
		list.add("def");
		assertEquals(list, Util.commasToList("abc,def"));
		list.add("ghi");
		assertEquals(list, Util.commasToList("abc,def,ghi"));
	}

	@Test
	public void bufferToString() {
		String s = "hello world";
		ByteBuffer buf = ByteBuffer.wrap(s.getBytes());
		assertEquals(s, Util.bufferToString(buf));
	}

	@Test
	public void set_union() {
		List<String> x = new ArrayList<String>();
		List<String> y = new ArrayList<String>();
		List<String> list = new ArrayList<String>();
		assertEquals(list, Util.union(x, y));
		x.add("abc");
		list.add("abc");
		assertEquals(list, Util.union(x, y));
		assertEquals(list, Util.union(y, x));
		y.add("def");
		list.add("def");
		assertSetEquals(list, Util.union(x, y));
		assertSetEquals(list, Util.union(y, x));
		x.add("def");
		assertSetEquals(list, Util.union(x, y));
		assertSetEquals(list, Util.union(y, x));
	}
	private void assertSetEquals(List<String> x, List<String> y) {
		assertTrue(Util.set_eq(x, y));
	}

	@Test
	public void set_eq() {
		List<String> x = new ArrayList<String>();
		List<String> y = new ArrayList<String>();
		assertTrue(Util.set_eq(x, y));
		x.add("a");
		assertFalse(Util.set_eq(x, y));
		assertFalse(Util.set_eq(y, x));
		y.add("b");
		assertFalse(Util.set_eq(x, y));
		assertFalse(Util.set_eq(y, x));
		x.add("b");
		assertFalse(Util.set_eq(x, y));
		assertFalse(Util.set_eq(y, x));
		y.add("a");
		assertTrue(Util.set_eq(x, y));
		assertTrue(Util.set_eq(y, x));
	}

	@Test
	public void prefix_set() {
		assertTrue(Util.prefix_set(list("a", "b", "c"), new ArrayList<String>()));
		assertTrue(Util.prefix_set(list("a", "b", "c"), list("a")));
		assertFalse(Util.prefix_set(list("a", "b", "c"), list("b")));
		assertFalse(Util.prefix_set(list("a", "b", "c"), list("c")));
		assertTrue(Util.prefix_set(list("a", "b", "c"), list("a", "b")));
		assertTrue(Util.prefix_set(list("a", "b", "c"), list("b", "a")));
		assertFalse(Util.prefix_set(list("a", "b", "c"), list("c", "a")));
		assertTrue(Util.prefix_set(list("a", "b", "c"), list("a", "b", "c")));
		assertTrue(Util.prefix_set(list("a", "b", "c"), list("c", "a", "b")));
		assertFalse(Util.prefix_set(list("a", "b", "c"), list("c", "a", "d")));
		assertFalse(Util.prefix_set(list("a"), list("b")));
		assertFalse(Util.prefix_set(list("a"), list("b", "a")));
	}
}
