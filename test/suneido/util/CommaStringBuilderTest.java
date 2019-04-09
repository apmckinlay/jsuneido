/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CommaStringBuilderTest {

	@Test
	public void test() {
		CommaStringBuilder csb = new CommaStringBuilder();
		assertEquals("", csb.toString());
		csb.add("one");
		assertEquals("one", csb.toString());
		csb.add(123);
		assertEquals("one,123", csb.toString());
	}

	@Test
	public void test2() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		CommaStringBuilder csb = new CommaStringBuilder(sb);
		csb.add("one");
		csb.add("two");
		sb.append(")");
		assertEquals("(one,two)", sb.toString());
	}

}
