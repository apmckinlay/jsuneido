package suneido;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UtilTest {
	@Test
	public void listToCommas() {
		assertEquals("", Util.listToCommas(null));
		List<String> list = new ArrayList<String>();
		assertEquals("", Util.listToCommas(list));
		list.add("one");
		assertEquals("one", Util.listToCommas(list));
		list.add("two");
		assertEquals("one,two", Util.listToCommas(list));
		list.add("three");
		assertEquals("one,two,three", Util.listToCommas(list));
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
	public void find() {
		List<String> list = new ArrayList<String>();
		assertEquals(-1, Util.find(list, "hello"));
		list.add("abc");
		assertEquals(-1, Util.find(list, "hello"));
		assertEquals(0, Util.find(list, "abc"));
		list.add("def");
		list.add("ghi");
		assertEquals(-1, Util.find(list, "hello"));
		assertEquals(0, Util.find(list, "abc"));
		assertEquals(1, Util.find(list, "def"));
		assertEquals(2, Util.find(list, "ghi"));
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
}
