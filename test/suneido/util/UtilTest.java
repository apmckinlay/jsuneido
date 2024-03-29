/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static suneido.util.Util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import suneido.runtime.Ops;

public class UtilTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@After
	public void restoreQuoting() {
		Ops.default_single_quotes = false;
	}

	@Test
	public void listToParens() {
		assertEquals("()", Util.listToParens(null));
		List<String> list = new ArrayList<>();
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
		List<String> list = new ArrayList<>();
		list.add("abc");
		assertEquals(list, Util.commasToList("abc"));
		list.add("def");
		assertEquals(list, Util.commasToList("abc,def"));
		list.add("ghi");
		assertEquals(list, Util.commasToList("abc,def,ghi"));
	}

	@Test
	public void set_union() {
		List<String> x = new ArrayList<>();
		List<String> y = new ArrayList<>();
		List<String> list = new ArrayList<>();
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
	private static void assertSetEquals(List<String> x, List<String> y) {
		assertTrue(Util.setEquals(x, y));
	}

	@Test
	public void set_eq() {
		List<String> x = new ArrayList<>();
		List<String> y = new ArrayList<>();
		assertTrue(Util.setEquals(x, y));
		x.add("a");
		assertFalse(Util.setEquals(x, y));
		assertFalse(Util.setEquals(y, x));
		y.add("b");
		assertFalse(Util.setEquals(x, y));
		assertFalse(Util.setEquals(y, x));
		x.add("b");
		assertFalse(Util.setEquals(x, y));
		assertFalse(Util.setEquals(y, x));
		y.add("a");
		assertTrue(Util.setEquals(x, y));
		assertTrue(Util.setEquals(y, x));
	}

	@Test
	public void prefix_set() {
		assertTrue(Util.startsWithSet(asList("a", "b", "c"), new ArrayList<String>()));
		assertTrue(Util.startsWithSet(asList("a", "b", "c"), asList("a")));
		assertFalse(Util.startsWithSet(asList("a", "b", "c"), asList("b")));
		assertFalse(Util.startsWithSet(asList("a", "b", "c"), asList("c")));
		assertTrue(Util.startsWithSet(asList("a", "b", "c"), asList("a", "b")));
		assertTrue(Util.startsWithSet(asList("a", "b", "c"), asList("b", "a")));
		assertFalse(Util.startsWithSet(asList("a", "b", "c"), asList("c", "a")));
		assertTrue(Util.startsWithSet(asList("a", "b", "c"), asList("a", "b", "c")));
		assertTrue(Util.startsWithSet(asList("a", "b", "c"), asList("c", "a", "b")));
		assertFalse(Util.startsWithSet(asList("a", "b", "c"), asList("c", "a", "d")));
		assertFalse(Util.startsWithSet(asList("a"), asList("b")));
		assertFalse(Util.startsWithSet(asList("a"), asList("b", "a")));
	}

	@Test
	public void test_lowerBound() {
		assertEquals(0, lowerBound(Collections.emptyList(), 123));
		assertEquals(0, lowerBound(asList(456), 123));
		assertEquals(0, lowerBound(asList(123), 123));
		assertEquals(0, lowerBound(asList(123, 123, 456), 123));
		assertEquals(1, lowerBound(asList(123), 456));
		assertEquals(1, lowerBound(asList(0, 123, 123, 456), 123));
		assertEquals(3, lowerBound(asList(0, 123, 123, 456), 456));
	}

	@Test
	public void test_upperBound() {
		assertEquals(0, upperBound(Collections.emptyList(), 123));
		assertEquals(0, upperBound(asList(456), 123));
		assertEquals(1, upperBound(asList(123), 123));
		assertEquals(2, upperBound(asList(123, 123, 456), 123));
		assertEquals(1, upperBound(asList(123), 456));
		assertEquals(3, upperBound(asList(0, 123, 123, 456), 123));
		assertEquals(4, upperBound(asList(0, 123, 123, 456), 456));
	}

	@Test
	public void test_bytes_strings() {
		byte[] bytes = new byte[256];
		for (int i = 0; i < 256; ++i)
			bytes[i] = (byte) (i & 0xff);
		String s = bytesToString(bytes);
		bytes = stringToBytes(s);
		for (int i = 0; i < 256; ++i)
			assertEquals(i, (bytes[i] & 0xff));
	}

}
