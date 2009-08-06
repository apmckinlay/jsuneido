package suneido.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static suneido.util.Util.*;

import java.nio.ByteBuffer;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

import suneido.language.Ops;
import suneido.util.Util.Range;

public class UtilTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@Test
	public void listToParens() {
		assertEquals("()", Util.listToParens(null));
		List<String> list = new ArrayList<String>();
		assertEquals("()", Util.listToParens(list));
		list.add("one");
		assertEquals("('one')", Util.displayListToParens(list));
		list.add("two");
		assertEquals("('one','two')", Util.displayListToParens(list));
		list.add("three");
		assertEquals("('one','two','three')", Util.displayListToParens(list));
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
	public void bufferUcompare_test() {
		byte[][] values = {
				new byte[] {}, new byte[] { 12 },
				new byte[] { 12, 34 }, new byte[] { (byte) 0xee },
				new byte[] { (byte) 0xee, 12 } };
		for (int i = 0; i < values.length; ++i) {
			ByteBuffer buf1 = ByteBuffer.wrap(values[i]);
			assertEquals(0, bufferUcompare(buf1, buf1));
			for (int j = i + 1; j < values.length; ++j) {
				ByteBuffer buf2 = ByteBuffer.wrap(values[j]);
				assert (bufferUcompare(buf1, buf2) < 0);
				assert (bufferUcompare(buf2, buf1) > 0);
			}

		}
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
		assertTrue(Util.prefix_set(asList("a", "b", "c"), new ArrayList<String>()));
		assertTrue(Util.prefix_set(asList("a", "b", "c"), asList("a")));
		assertFalse(Util.prefix_set(asList("a", "b", "c"), asList("b")));
		assertFalse(Util.prefix_set(asList("a", "b", "c"), asList("c")));
		assertTrue(Util.prefix_set(asList("a", "b", "c"), asList("a", "b")));
		assertTrue(Util.prefix_set(asList("a", "b", "c"), asList("b", "a")));
		assertFalse(Util.prefix_set(asList("a", "b", "c"), asList("c", "a")));
		assertTrue(Util.prefix_set(asList("a", "b", "c"), asList("a", "b", "c")));
		assertTrue(Util.prefix_set(asList("a", "b", "c"), asList("c", "a", "b")));
		assertFalse(Util.prefix_set(asList("a", "b", "c"), asList("c", "a", "d")));
		assertFalse(Util.prefix_set(asList("a"), asList("b")));
		assertFalse(Util.prefix_set(asList("a"), asList("b", "a")));
	}

	@Test
	public void test_lowerBound() {
		assertEquals(0, lowerBound(Collections.<Integer> emptyList(), 123));
		assertEquals(0, lowerBound(asList(456), 123));
		assertEquals(0, lowerBound(asList(123), 123));
		assertEquals(0, lowerBound(asList(123, 123, 456), 123));
		assertEquals(1, lowerBound(asList(123), 456));
		assertEquals(1, lowerBound(asList(0, 123, 123, 456), 123));
		assertEquals(3, lowerBound(asList(0, 123, 123, 456), 456));
	}

	@Test
	public void test_upperBound() {
		assertEquals(0, upperBound(Collections.<Integer> emptyList(), 123));
		assertEquals(0, upperBound(asList(456), 123));
		assertEquals(1, upperBound(asList(123), 123));
		assertEquals(2, upperBound(asList(123, 123, 456), 123));
		assertEquals(1, upperBound(asList(123), 456));
		assertEquals(3, upperBound(asList(0, 123, 123, 456), 123));
		assertEquals(4, upperBound(asList(0, 123, 123, 456), 456));
	}

	@Test
	public void test_equalRange() {
		assertEquals(new Range(0,0), equalRange(Collections.<Integer> emptyList(), 123));
		assertEquals(new Range(0,0), equalRange(asList(456), 123));
		assertEquals(new Range(0, 1), equalRange(asList(123), 123));
		assertEquals(new Range(0, 2), equalRange(asList(123, 123, 456), 123));
		assertEquals(new Range(1,1), equalRange(asList(123), 456));
		assertEquals(new Range(1, 3), equalRange(asList(0, 123, 123, 456), 123));
		assertEquals(new Range(3, 4), equalRange(asList(0, 123, 123, 456), 456));
	}

}
