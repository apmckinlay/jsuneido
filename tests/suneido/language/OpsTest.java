package suneido.language;

import static org.junit.Assert.*;
import static suneido.language.Ops.*;

import java.math.BigDecimal;

import org.junit.Test;

import suneido.SuException;

public class OpsTest {

	@Test
	public void test_equals() {
		assertTrue(is(null, null));
		assertFalse(is(null, 123));
		assertFalse(is(123, null));
		assertTrue(is(123, 123));
		assertTrue(is("abc", "abc"));
		assertTrue(is(123, BigDecimal.valueOf(123)));
		assertTrue(is(BigDecimal.valueOf(123), 123));
	}

	@Test
	public void test_cmp() {
		assertEquals(0, cmp(123, 123));
		assertEquals(0, cmp("abc", "abc"));
		assertTrue(cmp(true, false) > 0);
		assertTrue(cmp(true, 123) < 0);
		assertTrue(cmp(456, 123) > 0);
		assertTrue(cmp(123, "def") < 0);
		assertTrue(cmp("abc", "def") < 0);
		assertTrue(cmp(1, BigDecimal.valueOf(1.1)) < 0);
		assertTrue(cmp(1, BigDecimal.valueOf(.9)) > 0);
		assertTrue(cmp(BigDecimal.valueOf(.9), 1) < 0);
		assertTrue(cmp(BigDecimal.valueOf(1.1), 1) > 0);
		assertTrue(cmp(123, this) < 0);
		assertTrue(cmp(this, "hello") > 0);
	}

	@Test
	public void test_cat() {
		assertEquals("onetwo", cat("one", "two"));
		assertEquals("one.2", cat("one", BigDecimal.valueOf(.2)));
		assertEquals("1two", cat(1, "two"));
	}

	private final static Object x = 123;
	private final static Object y = 456;
	private final static Object z = BigDecimal.valueOf(.9);

	@Test
	public void test_add() {
		assertEquals(579, add(x, y));
		assertEquals(579, add(x, "456"));
		assertEquals(579, add("123", y));
		assertEquals(new BigDecimal("456.9"), add(z, y));
		assertEquals(new BigDecimal("123.9"), add(x, z));
		assertEquals(new BigDecimal("1.8"), add(z, z));

		assertEquals(inf, add(inf, x));
		assertEquals(inf, add(x, inf));
		assertEquals(inf, add(inf, z));
		assertEquals(inf, add(z, inf));
		assertEquals(inf, add(inf, "1"));
		assertEquals(inf, add("1", inf));
		assertEquals(inf, add(inf, inf));
		assertEquals(0, add(inf, minus_inf));

		assertEquals(minus_inf, add(minus_inf, x));
		assertEquals(minus_inf, add(x, minus_inf));
		assertEquals(minus_inf, add(minus_inf, z));
		assertEquals(minus_inf, add(z, minus_inf));
		assertEquals(minus_inf, add(minus_inf, minus_inf));
		assertEquals(0, add(minus_inf, inf));
	}

	@Test
	public void test_sub() {
		assertEquals(-333, sub(x, y));
		assertEquals(333, sub(y, x));
		assertEquals(-333, sub(x, "456"));
		assertEquals(-333, sub("123", y));
		assertEquals(new BigDecimal("-455.1"), sub(z, y));
		assertEquals(new BigDecimal("122.1"), sub(x, z));
		assertTrue(is(zero, sub(z, z)));

		assertEquals(inf, sub(inf, x));
		assertEquals(minus_inf, sub(x, inf));
		assertEquals(inf, sub(inf, z));
		assertEquals(minus_inf, sub(z, inf));
		assertEquals(inf, sub(inf, "1"));
		assertEquals(minus_inf, sub("1", inf));
		assertEquals(0, sub(inf, inf));
		assertEquals(inf, sub(inf, minus_inf));

		assertEquals(minus_inf, sub(minus_inf, x));
		assertEquals(inf, sub(x, minus_inf));
		assertEquals(minus_inf, sub(minus_inf, z));
		assertEquals(inf, sub(z, minus_inf));
		assertEquals(0, sub(minus_inf, minus_inf));
		assertEquals(minus_inf, sub(minus_inf, inf));
	}

	private final static Object p9 = BigDecimal.valueOf(9);
	private final static Object m9 = BigDecimal.valueOf(-9);
	private final static Object p81 = BigDecimal.valueOf(81);
	private final static Object m81 = BigDecimal.valueOf(-81);

	@Test
	public void test_mul() {
		assertEquals(inf,		mul(minus_inf, minus_inf));
		assertEquals(inf,		mul(minus_inf, -9));
		assertEquals(inf,		mul(minus_inf, m9));
		assertEquals(0,			mul(minus_inf, zero));
		assertEquals(minus_inf, mul(minus_inf, 9));
		assertEquals(minus_inf, mul(minus_inf, p9));
		assertEquals(minus_inf, mul(minus_inf, inf));

		assertEquals(inf,		mul(m9, minus_inf));
		assertEquals(p81,		mul(m9, -9));
		assertEquals(p81,		mul(m9, m9));
		assertEquals(0,			mul(m9, 0));
		assertEquals(0,			mul(m9, zero));
		assertEquals(m81,		mul(m9, 9));
		assertEquals(m81,		mul(m9, p9));
		assertEquals(minus_inf, mul(m9, inf));

		assertEquals(inf,		mul(-9, minus_inf));
		assertEquals(p81,		mul(-9, m9));
		assertEquals(81,		mul(-9, -9));
		assertEquals(0,			mul(-9, 0));
		assertEquals(zero,		mul(-9, zero));
		assertEquals(-81,		mul(-9, 9));
		assertEquals(m81,		mul(-9, p9));
		assertEquals(minus_inf, mul(-9, inf));

		assertEquals(0,			mul(0, minus_inf));
		assertEquals(0,			mul(0, m9));
		assertEquals(0,			mul(0, 0));
		assertEquals(0,			mul(0, 0));
		assertEquals(0,			mul(0, zero));
		assertEquals(0,			mul(0, 9));
		assertEquals(0,			mul(0, p9));
		assertEquals(0, 		mul(0, inf));

		assertEquals(0,			mul(zero, minus_inf));
		assertEquals(0,			mul(zero, m9));
		assertEquals(0,			mul(zero, 0));
		assertEquals(0,			mul(zero, 0));
		assertEquals(0,			mul(zero, zero));
		assertEquals(zero,		mul(zero, 9));
		assertEquals(0,			mul(zero, p9));
		assertEquals(0, 		mul(zero, inf));

		assertEquals(minus_inf,	mul(9, minus_inf));
		assertEquals(m81,		mul(9, m9));
		assertEquals(-81,		mul(9, -9));
		assertEquals(0,			mul(9, 0));
		assertEquals(zero,		mul(9, zero));
		assertEquals(81,		mul(9, 9));
		assertEquals(p81,		mul(9, p9));
		assertEquals(inf,		mul(9, inf));

		assertEquals(minus_inf,	mul(p9, minus_inf));
		assertEquals(m81,		mul(p9, m9));
		assertEquals(m81,		mul(p9, -9));
		assertEquals(0,			mul(p9, 0));
		assertEquals(0,			mul(p9, zero));
		assertEquals(p81,		mul(p9, 9));
		assertEquals(p81,		mul(p9, p9));
		assertEquals(inf,		mul(p9, inf));

		assertEquals(minus_inf,	mul(inf, minus_inf));
		assertEquals(minus_inf,	mul(inf, -9));
		assertEquals(minus_inf,	mul(inf, m9));
		assertEquals(0,			mul(inf, zero));
		assertEquals(inf, 		mul(inf, 9));
		assertEquals(inf,		mul(inf, p9));
		assertEquals(inf,		mul(inf, inf));
	}

	@Test
	public void test_div() {
		Object p1 = BigDecimal.valueOf(1);
		Object m1 = BigDecimal.valueOf(-1);

		assertEquals(1,			div(minus_inf, minus_inf));
		assertEquals(inf,		div(minus_inf, -9));
		assertEquals(inf,		div(minus_inf, m9));
		assertEquals(minus_inf,	div(minus_inf, zero));
		assertEquals(minus_inf, div(minus_inf, 9));
		assertEquals(minus_inf, div(minus_inf, p9));
		assertEquals(-1, 		div(minus_inf, inf));

		assertEquals(0,			div(m9, minus_inf));
		assertEquals(p1,		div(m9, -9));
		assertEquals(p1,		div(m9, m9));
		assertEquals(minus_inf, div(m9, 0));
		assertEquals(minus_inf, div(m9, zero));
		assertEquals(m1,		div(m9, 9));
		assertEquals(m1,		div(m9, p9));
		assertEquals(0,			div(m9, inf));

		assertEquals(0,			div(-9, minus_inf));
		assertEquals(p1,		div(-9, m9));
		assertEquals(p1,		div(-9, -9));
		assertEquals(minus_inf,	div(-9, 0));
		assertEquals(minus_inf,	div(-9, zero));
		assertEquals(m1,		div(-9, 9));
		assertEquals(m1,		div(-9, p9));
		assertEquals(0, 		div(-9, inf));

		assertEquals(0,			div(0, minus_inf));
		assertEquals(0,			div(0, m9));
		assertEquals(0,			div(0, 0));
		assertEquals(0,			div(0, 0));
		assertEquals(0,			div(0, zero));
		assertEquals(0,			div(0, 9));
		assertEquals(0,			div(0, p9));
		assertEquals(0, 		div(0, inf));

		assertEquals(0,			div(zero, minus_inf));
		assertEquals(0,			div(zero, m9));
		assertEquals(0,			div(zero, 0));
		assertEquals(0,			div(zero, 0));
		assertEquals(0,			div(zero, zero));
		assertEquals(0,			div(zero, 9));
		assertEquals(0,			div(zero, p9));
		assertEquals(0, 		div(zero, inf));

		assertEquals(0,			div(9, minus_inf));
		assertEquals(m1,		div(9, m9));
		assertEquals(m1,		div(9, -9));
		assertEquals(inf,		div(9, 0));
		assertEquals(inf,		div(9, zero));
		assertEquals(p1,		div(9, 9));
		assertEquals(p1,		div(9, p9));
		assertEquals(0,			div(9, inf));

		assertEquals(0,			div(p9, minus_inf));
		assertEquals(m1,		div(p9, m9));
		assertEquals(m1,		div(p9, -9));
		assertEquals(inf,		div(p9, 0));
		assertEquals(inf,		div(p9, zero));
		assertEquals(p1,		div(p9, 9));
		assertEquals(p1,		div(p9, p9));
		assertEquals(0,			div(p9, inf));

		assertEquals(-1,		div(inf, minus_inf));
		assertEquals(minus_inf,	div(inf, -9));
		assertEquals(minus_inf,	div(inf, m9));
		assertEquals(inf,		div(inf, 0));
		assertEquals(inf,		div(inf, zero));
		assertEquals(inf, 		div(inf, 9));
		assertEquals(inf,		div(inf, p9));
		assertEquals(1,			div(inf, inf));
	}

	@Test
	public void test_catchMatch() {
		match("abc", null);
		match("abc", "a");
		nomatch("abc", "b");
		match("abc", "b|ab");
		nomatch("abc", "x|y|z");
		match("abc", "*bc|*xy");
		nomatch("abc", "*x|*y");
	}

	private void match(String exception, String pattern) {
		catchMatch(new SuException(exception), pattern);
	}
	private void nomatch(String exception, String pattern) {
		try {
			catchMatch(new SuException(exception), pattern);
			fail();
		} catch (SuException e) {
			// expected
		}
	}

	@Test
	public void test_toStringBD() {
		assertEquals("10000000000000000000", Ops.toStringBD(new BigDecimal("1e19")));
		assertEquals("1e20", Ops.toStringBD(new BigDecimal("1e20")));
	}
}
