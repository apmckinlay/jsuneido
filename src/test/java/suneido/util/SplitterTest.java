/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Test;

import com.google.common.base.Splitter;

public class SplitterTest {

	@Test
	public void test_splitter() {
		Splitter splitter = Splitter.on(',');
		Iterator<String> x;

		x = splitter.split("").iterator();
		assertEquals("", x.next());
		assertFalse(x.hasNext());

		x = splitter.split("hello,world").iterator();
		assertEquals("hello", x.next());
		assertEquals("world", x.next());
		assertFalse(x.hasNext());

		assertEquals(1, "".split(",").length);
	}

}
