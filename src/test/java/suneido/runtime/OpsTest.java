/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static suneido.compiler.Compiler.compile;
import static suneido.runtime.Ops.*;
import static suneido.util.Dnum.Inf;
import static suneido.util.Dnum.MinusInf;
import static suneido.util.Dnum.Zero;
import static suneido.util.Dnum.div;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Strings;

import suneido.PortTests;
import suneido.SuException;
import suneido.compiler.ExecuteTest;
import suneido.util.Dnum;

public class OpsTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@After
	public void restoreQuoting() {
		Ops.default_single_quotes = false;
	}

	/** @see ExecuteTest test_display */
	@Test
	public void test_display() {
		assertThat(Ops.display("hello"), equalTo("'hello'"));
		assertThat(Ops.display("hello\000"), equalTo("'hello\\x00'"));
		assertThat(Ops.display(new Concats("hello", "world")), equalTo("'helloworld'"));
	}

	@Test
	public void test_is() {
		is(123, 123);
		is(123, Dnum.from(123));
		is("hello", "hello");
		is("hello", new Concats("hel", "lo"));
		is("hello", new Except("hello", null));
	}
	private static void is(Object x, Object y) {
		assertTrue(Ops.is(x, y));
		assertTrue(Ops.is(y, x));
		assertTrue(Ops.cmp(x, y) == 0);
		assertTrue(Ops.cmp(y, x) == 0);
	}

	@Test
	public void test_cmp() {
		lt(false, true);
		lt(true, 123);
		lt(123, 456);
		lt(123, "def");
		lt("abc", "def");
		lt(1, Dnum.from(1.1));
		lt(Dnum.from(.9), 1);
		lt(Dnum.from(.9), 1);
		lt(1, Dnum.from(1.1));
		lt(123, this);
		lt("hello", this);
		lt(compile("x", "function(){}"), compile("x", "class{}")); // non-portable
	}
	private static void lt(Object x, Object y) {
		assertTrue(Ops.cmp(x, y) < 0);
		assertTrue(Ops.cmp(y, x) > 0);
		assertFalse(Ops.is(x, y));
		assertFalse(Ops.is(y, x));
	}

	@Test
	public void test_cat() {
		assertEquals("onetwo", cat("one", "two"));
		assertEquals("one.2", cat("one", Dnum.from(.2)));
		assertEquals("1two", cat(1, "two"));
	}

	@Test
	public void test_cat2() {
		Object x = Ops.cat("hello", "world");
		assertTrue(x instanceof String);
		assertEquals("helloworld", x);

		String s = Strings.repeat("helloworld", 30);
		x = Ops.cat(s, ".");
		assertTrue(x instanceof String2);
		assertEquals(s + ".", x.toString());

		x = Ops.cat(x, ">");
		assertTrue(x instanceof String2);
		assertEquals(s + "." + ">", x.toString());
		}

	private static final Object x = 123;
	private static final Object y = 456;
	private static final Object z = Dnum.from(.9);

	@Test
	public void test_add() {
		assertEquals(579, add(x, y));
		assertEquals(Dnum.parse("456.9"), add(z, y));
		assertEquals(Dnum.parse("123.9"), add(x, z));
		assertEquals(Dnum.parse("1.8"), add(z, z));

		assertEquals(Inf, add(Inf, x));
		assertEquals(Inf, add(x, Inf));
		assertEquals(Inf, add(Inf, z));
		assertEquals(Inf, add(z, Inf));
		assertEquals(Inf, add(Inf, Inf));
		assertEquals(Zero, add(Inf, MinusInf));

		assertEquals(MinusInf, add(MinusInf, x));
		assertEquals(MinusInf, add(x, MinusInf));
		assertEquals(MinusInf, add(MinusInf, z));
		assertEquals(MinusInf, add(z, MinusInf));
		assertEquals(MinusInf, add(MinusInf, MinusInf));
		assertEquals(Zero, add(MinusInf, Inf));
	}

	@Test
	public void test_sub() {
		assertEquals(-333, sub(x, y));
		assertEquals(333, sub(y, x));
		assertEquals(Dnum.parse("-455.1"), sub(z, y));
		assertEquals(Dnum.parse("122.1"), sub(x, z));
		is(Zero, sub(z, z));

		assertEquals(Inf, sub(Inf, x));
		assertEquals(MinusInf, sub(x, Inf));
		assertEquals(Inf, sub(Inf, z));
		assertEquals(MinusInf, sub(z, Inf));
		assertEquals(Zero, sub(Inf, Inf));
		assertEquals(Inf, sub(Inf, MinusInf));

		assertEquals(MinusInf, sub(MinusInf, x));
		assertEquals(Inf, sub(x, MinusInf));
		assertEquals(MinusInf, sub(MinusInf, z));
		assertEquals(Inf, sub(z, MinusInf));
		assertEquals(Zero, sub(MinusInf, MinusInf));
		assertEquals(MinusInf, sub(MinusInf, Inf));
	}

	private static final Object p9 = Dnum.from(9);
	private static final Object m9 = Dnum.from(-9);
	private static final Object p81 = Dnum.from(81);
	private static final Object m81 = Dnum.from(-81);

	@Test
	public void test_mul() {
		assertEquals(Inf,		mul(MinusInf, MinusInf));
		assertEquals(Inf,		mul(MinusInf, -9));
		assertEquals(Inf,		mul(MinusInf, m9));
		assertEquals(Zero,		mul(MinusInf, Zero));
		assertEquals(MinusInf,	mul(MinusInf, 9));
		assertEquals(MinusInf,	mul(MinusInf, p9));
		assertEquals(MinusInf,	mul(MinusInf, Inf));

		assertEquals(Inf,		mul(m9, MinusInf));
		assertEquals(p81,		mul(m9, -9));
		assertEquals(p81,		mul(m9, m9));
		assertEquals(Zero,		mul(m9, 0));
		assertEquals(Zero,		mul(m9, Zero));
		assertEquals(m81,		mul(m9, 9));
		assertEquals(m81,		mul(m9, p9));
		assertEquals(MinusInf,	mul(m9, Inf));

		assertEquals(Inf,		mul(-9, MinusInf));
		assertEquals(p81,		mul(-9, m9));
		assertEquals(81,		mul(-9, -9));
		assertEquals(0,			mul(-9, 0));
		assertEquals(Zero,		mul(-9, Zero));
		assertEquals(-81,		mul(-9, 9));
		assertEquals(m81,		mul(-9, p9));
		assertEquals(MinusInf,	mul(-9, Inf));

		assertEquals(Zero,		mul(0, MinusInf));
		assertEquals(Zero,		mul(0, m9));
		assertEquals(0,			mul(0, 0));
		assertEquals(0,			mul(0, 0));
		assertEquals(Zero,		mul(0, Zero));
		assertEquals(0,			mul(0, 9));
		assertEquals(Zero,		mul(0, p9));
		assertEquals(Zero, 		mul(0, Inf));

		assertEquals(Zero,		mul(Zero, MinusInf));
		assertEquals(Zero,		mul(Zero, m9));
		assertEquals(Zero,		mul(Zero, 0));
		assertEquals(Zero,		mul(Zero, 0));
		assertEquals(Zero,		mul(Zero, Zero));
		assertEquals(Zero,		mul(Zero, 9));
		assertEquals(Zero,		mul(Zero, p9));
		assertEquals(Zero, 		mul(Zero, Inf));

		assertEquals(MinusInf,	mul(9, MinusInf));
		assertEquals(m81,		mul(9, m9));
		assertEquals(-81,		mul(9, -9));
		assertEquals(0,			mul(9, 0));
		assertEquals(Zero,		mul(9, Zero));
		assertEquals(81,		mul(9, 9));
		assertEquals(p81,		mul(9, p9));
		assertEquals(Inf,		mul(9, Inf));

		assertEquals(MinusInf,	mul(p9, MinusInf));
		assertEquals(m81,		mul(p9, m9));
		assertEquals(m81,		mul(p9, -9));
		assertEquals(Zero,		mul(p9, 0));
		assertEquals(Zero,		mul(p9, Zero));
		assertEquals(p81,		mul(p9, 9));
		assertEquals(p81,		mul(p9, p9));
		assertEquals(Inf,		mul(p9, Inf));

		assertEquals(MinusInf,	mul(Inf, MinusInf));
		assertEquals(MinusInf,	mul(Inf, -9));
		assertEquals(MinusInf,	mul(Inf, m9));
		assertEquals(Zero,		mul(Inf, Zero));
		assertEquals(Inf, 		mul(Inf, 9));
		assertEquals(Inf,		mul(Inf, p9));
		assertEquals(Inf,		mul(Inf, Inf));
	}

	@Test
	public void test_overflow() {
		assertEquals(Dnum.from(Integer.MAX_VALUE + 1L), add(Integer.MAX_VALUE, 1));
		assertEquals(Dnum.from(Integer.MAX_VALUE + 1L), sub(Integer.MAX_VALUE, -1));
		assertEquals(Dnum.from(Integer.MAX_VALUE * 10L), mul(Integer.MAX_VALUE, 10));
	}

	private static final Object p1 = Dnum.from(1);
	private static final Object m1 = Dnum.from(-1);

	@Test
	public void test_div() {
		assertEquals(Dnum.One,	div(MinusInf, MinusInf));
		assertEquals(Inf,		div(MinusInf, -9));
		assertEquals(Inf,		div(MinusInf, m9));
		assertEquals(MinusInf,	div(MinusInf, Zero));
		assertEquals(MinusInf,	div(MinusInf, 9));
		assertEquals(MinusInf,	div(MinusInf, p9));
		assertEquals(Dnum.from(-1), div(MinusInf, Inf));

		assertEquals(Zero,		div(m9, MinusInf));
		assertEquals(p1,		div(m9, -9));
		assertEquals(p1,		div(m9, m9));
		assertEquals(MinusInf,	div(m9, 0));
		assertEquals(MinusInf,	div(m9, Zero));
		assertEquals(m1,		div(m9, 9));
		assertEquals(m1,		div(m9, p9));
		assertEquals(Zero,		div(m9, Inf));

		assertEquals(Zero,		div(-9, MinusInf));
		assertEquals(p1,		div(-9, m9));
		assertEquals(p1,		div(-9, -9));
		assertEquals(MinusInf,	div(-9, 0));
		assertEquals(MinusInf,	div(-9, Zero));
		assertEquals(m1,		div(-9, 9));
		assertEquals(m1,		div(-9, p9));
		assertEquals(Zero, 		div(-9, Inf));

		assertEquals(Zero,		div(0, MinusInf));
		assertEquals(Zero,		div(0, m9));
		assertEquals(Zero,		div(0, 0));
		assertEquals(Zero,		div(0, 0));
		assertEquals(Zero,		div(0, Zero));
		assertEquals(Zero,		div(0, 9));
		assertEquals(Zero,		div(0, p9));
		assertEquals(Zero, 		div(0, Inf));

		assertEquals(Zero,		div(Zero, MinusInf));
		assertEquals(Zero,		div(Zero, m9));
		assertEquals(Zero,		div(Zero, 0));
		assertEquals(Zero,		div(Zero, 0));
		assertEquals(Zero,		div(Zero, Zero));
		assertEquals(Zero,		div(Zero, 9));
		assertEquals(Zero,		div(Zero, p9));
		assertEquals(Zero, 		div(Zero, Inf));

		assertEquals(Zero,		div(9, MinusInf));
		assertEquals(m1,		div(9, m9));
		assertEquals(m1,		div(9, -9));
		assertEquals(Inf,		div(9, 0));
		assertEquals(Inf,		div(9, Zero));
		assertEquals(p1,		div(9, 9));
		assertEquals(p1,		div(9, p9));
		assertEquals(Zero,		div(9, Inf));

		assertEquals(Zero,		div(p9, MinusInf));
		assertEquals(m1,		div(p9, m9));
		assertEquals(m1,		div(p9, -9));
		assertEquals(Inf,		div(p9, 0));
		assertEquals(Inf,		div(p9, Zero));
		assertEquals(p1,		div(p9, 9));
		assertEquals(p1,		div(p9, p9));
		assertEquals(Zero,		div(p9, Inf));

		assertEquals(Dnum.from(-1),	div(Inf, MinusInf));
		assertEquals(MinusInf,	div(Inf, -9));
		assertEquals(MinusInf,	div(Inf, m9));
		assertEquals(Inf,		div(Inf, 0));
		assertEquals(Inf,		div(Inf, Zero));
		assertEquals(Inf, 		div(Inf, 9));
		assertEquals(Inf,		div(Inf, p9));
		assertEquals(Dnum.One,	div(Inf, Inf));
	}

	@Test
	public void test_catchMatch() {
		match("abc", "a");
		nomatch("abc", "b");
		match("abc", "b|ab");
		nomatch("abc", "x|y|z");
		match("abc", "*bc|*xy");
		nomatch("abc", "*x|*y");
		match("abc", "*");
	}

	private static void match(String exception, String pattern) {
		try {
			catchMatch(new SuException(exception), pattern);
		} catch (Throwable e) {
			fail();
		}
	}
	private static void nomatch(String exception, String pattern) {
		try {
			catchMatch(new SuException(exception), pattern);
			fail();
		} catch (Throwable e) {
			// expected
		}
	}

	@Test
	public void dont_catch_block_return_exception() {
		try {
			catchMatch(new BlockReturnException(null,0));
			fail();
		} catch (Throwable e) {
			assertThat(e, instanceOf(BlockReturnException.class));
		}
		try {
			catchMatch(new BlockReturnException(null,0), "*");
			fail();
		} catch (Throwable e) {
			assertThat(e, instanceOf(BlockReturnException.class));
		}
	}

	@Test
	public void getString() {
		assertEquals("a", Ops.get("abcd", 0));
		assertEquals("d", Ops.get("abcd", 3));
		assertEquals("", Ops.get("abcd", 4));
		assertEquals("d", Ops.get("abcd", -1));
		assertEquals("a", Ops.get("abcd", -4));
		assertEquals("", Ops.get("abcd", -5));
	}

	@Test
	public void porttests() {
		PortTests.addTest("compare", OpsTest::pt_compare);
		PortTests.skipTest("lang_sub");
		PortTests.skipTest("lang_range");
		assert PortTests.runFile("lang.test");
	}

	public static boolean pt_compare(String... data) {
		int n = data.length;
		for (int i = 0; i < n; ++i) {
			Object x = compile("test", data[i]);
			if (Ops.cmp(x, x) != 0)
				return false;
			for (int j = i + 1; j < n; ++j) {
				Object y = compile("test", data[j]);
				if (Ops.cmp(x, y) >= 0 || Ops.cmp(y, x) <= 0) {
					System.out.println(x + " should be less than " + y);
					return false;
				}
			}
		}
		return true;
	}

}
