/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static suneido.runtime.Ops.*;

import org.junit.Test;

import suneido.util.Dnum;

public class SuValueTest {
	@Test
	public void compareTo() {
		SuContainer c1 = new SuContainer();
		c1.add(0);
		SuContainer c2 = new SuContainer();
		c2.add(0);
		c2.add(1);
		SuContainer c3 = new SuContainer();
		c3.add(1);
		Object[] values = {
			false, true,
			0, 123, 456, 789,
			"", "abc", "def",
			SuDate.fromLiteral("20080514.143622123"),
			SuDate.fromLiteral("20080522.143622123"),
			SuDate.fromLiteral("20081216.152744828"),
			SuDate.fromLiteral("20081216.153244828"),
			new SuContainer(), c1, c2, c3 };
		for (int i = 0; i < values.length; ++i)
			for (int j = 0; j < values.length; ++j)
				assertEquals(display(values[i]) + " cmp " + display(values[j]),
						Integer.signum(i - j),
						Integer.signum(cmp(values[i], values[j])));
	}

	@Test
	public void math() {
		int[] ints = { 0, 1, -1, 123, -123 };
		Object[] values = new Object[ints.length * 2];
		for (int i = 0; i < ints.length; ++i) {
			values[2 * i] = ints[i];
			values[2 * i + 1] = Dnum.from(ints[i]);
		}
		for (int i : ints)
			for (int j : ints) {
				Object x = Dnum.from(i);
				Object y = Dnum.from(j);
				math1(x, y, i, j);
				math1(x, j, i, j);
				math1(i, y, i, j);
				math1(i, j, i, j);
			}
	}

	private static void math1(Object x, Object y, int i, int j) {
		Object z;
		z = add(x, y);
		assertTrue(i + " + " + j + " expected " + (i + j) + " got " + z,
				is(i + j, z));
		z = sub(x, y);
		assertTrue(i + " - " + j + " expected " + (i - j) + " got " + z,
				is(i - j, z));
		z = mul(x, y);
		assertTrue(i + " * " + j + " expected " + (i * j) + " got " + z,
				is(i * j, z));
		if (j == 0)
			return ; // skip divide by zero
		z = div(x, y);
		if (z instanceof Dnum)
			z = ((Dnum) z).round(15);
		Object expected = Dnum.from(((double) i) / ((double) j)).round(15);
		assertTrue(i + " / " + j + " expected " + expected + " got " + z,
				is(expected, z));
	}

	@Test
	public void simplifyType() {
		stest("suneido.code.Foo", "Foo");
		stest("suneido.runtime.builtin.Lucene", "Lucene");
		stest("suneido.runtime.builtin.SuThread", "Thread");
		stest("suneido.runtime.builtin.DateClass", "Date");
		stest("suneido.runtime.builtin.Adler32$1", "Adler32");
	}

	private static void stest(String s, String expected) {
		assertThat(SuValue.simplifyType(s), equalTo(expected));
	}

}
