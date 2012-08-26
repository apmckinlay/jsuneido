package suneido.language;

import static org.hamcrest.CoreMatchers.is;
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
		assertTrue(is((long) 123, 123));
		assertTrue(is(123, (long) 123));
		assertTrue(is((byte) 123, (long) 123));
		assertTrue(is(1.0F, 1.0D));
		assertTrue(is((byte) 1, 1.0D));
		assertTrue(is(1.0F, BigDecimal.valueOf(1)));
		assertTrue(is("abc", "abc"));
		assertTrue(is(123, BigDecimal.valueOf(123)));
		assertTrue(is(BigDecimal.valueOf(123), 123));
	}

	@Test
	public void test_cmp() {
		assertEquals(0, cmp(123, 123));
		assertEquals(0, cmp((long) 123, 123));
		assertEquals(0, cmp(123, (long) 123));
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

	private static final Object x = 123;
	private static final Object y = 456;
	private static final Object z = BigDecimal.valueOf(.9);

	@Test
	public void test_add() {
		assertEquals(579, add(x, y));
		assertEquals(579, add(x, "456"));
		assertEquals(579, add("123", y));
		assertEquals(new BigDecimal("456.9"), add(z, y));
		assertEquals(new BigDecimal("123.9"), add(x, z));
		assertEquals(new BigDecimal("1.8"), add(z, z));

		assertEquals(INF, add(INF, x));
		assertEquals(INF, add(x, INF));
		assertEquals(INF, add(INF, z));
		assertEquals(INF, add(z, INF));
		assertEquals(INF, add(INF, "1"));
		assertEquals(INF, add("1", INF));
		assertEquals(INF, add(INF, INF));
		assertEquals(0, add(INF, MINUS_INF));

		assertEquals(MINUS_INF, add(MINUS_INF, x));
		assertEquals(MINUS_INF, add(x, MINUS_INF));
		assertEquals(MINUS_INF, add(MINUS_INF, z));
		assertEquals(MINUS_INF, add(z, MINUS_INF));
		assertEquals(MINUS_INF, add(MINUS_INF, MINUS_INF));
		assertEquals(0, add(MINUS_INF, INF));
	}

	@Test
	public void test_sub() {
		assertEquals(-333, sub(x, y));
		assertEquals(333, sub(y, x));
		assertEquals(-333, sub(x, "456"));
		assertEquals(-333, sub("123", y));
		assertEquals(new BigDecimal("-455.1"), sub(z, y));
		assertEquals(new BigDecimal("122.1"), sub(x, z));
		assertTrue(is(ZERO, sub(z, z)));

		assertEquals(INF, sub(INF, x));
		assertEquals(MINUS_INF, sub(x, INF));
		assertEquals(INF, sub(INF, z));
		assertEquals(MINUS_INF, sub(z, INF));
		assertEquals(INF, sub(INF, "1"));
		assertEquals(MINUS_INF, sub("1", INF));
		assertEquals(0, sub(INF, INF));
		assertEquals(INF, sub(INF, MINUS_INF));

		assertEquals(MINUS_INF, sub(MINUS_INF, x));
		assertEquals(INF, sub(x, MINUS_INF));
		assertEquals(MINUS_INF, sub(MINUS_INF, z));
		assertEquals(INF, sub(z, MINUS_INF));
		assertEquals(0, sub(MINUS_INF, MINUS_INF));
		assertEquals(MINUS_INF, sub(MINUS_INF, INF));
	}

	private static final Object p9 = BigDecimal.valueOf(9);
	private static final Object m9 = BigDecimal.valueOf(-9);
	private static final Object p81 = BigDecimal.valueOf(81);
	private static final Object m81 = BigDecimal.valueOf(-81);

	@Test
	public void test_mul() {
		assertEquals(INF,		mul(MINUS_INF, MINUS_INF));
		assertEquals(INF,		mul(MINUS_INF, -9));
		assertEquals(INF,		mul(MINUS_INF, m9));
		assertEquals(0,			mul(MINUS_INF, ZERO));
		assertEquals(MINUS_INF, mul(MINUS_INF, 9));
		assertEquals(MINUS_INF, mul(MINUS_INF, p9));
		assertEquals(MINUS_INF, mul(MINUS_INF, INF));

		assertEquals(INF,		mul(m9, MINUS_INF));
		assertEquals(p81,		mul(m9, -9));
		assertEquals(p81,		mul(m9, m9));
		assertEquals(0,			mul(m9, 0));
		assertEquals(0,			mul(m9, ZERO));
		assertEquals(m81,		mul(m9, 9));
		assertEquals(m81,		mul(m9, p9));
		assertEquals(MINUS_INF, mul(m9, INF));

		assertEquals(INF,		mul(-9, MINUS_INF));
		assertEquals(p81,		mul(-9, m9));
		assertEquals(81,		mul(-9, -9));
		assertEquals(0,			mul(-9, 0));
		assertEquals(0,			mul(-9, ZERO));
		assertEquals(-81,		mul(-9, 9));
		assertEquals(m81,		mul(-9, p9));
		assertEquals(MINUS_INF, mul(-9, INF));

		assertEquals(0,			mul(0, MINUS_INF));
		assertEquals(0,			mul(0, m9));
		assertEquals(0,			mul(0, 0));
		assertEquals(0,			mul(0, 0));
		assertEquals(0,			mul(0, ZERO));
		assertEquals(0,			mul(0, 9));
		assertEquals(0,			mul(0, p9));
		assertEquals(0, 		mul(0, INF));

		assertEquals(0,			mul(ZERO, MINUS_INF));
		assertEquals(0,			mul(ZERO, m9));
		assertEquals(0,			mul(ZERO, 0));
		assertEquals(0,			mul(ZERO, 0));
		assertEquals(0,			mul(ZERO, ZERO));
		assertEquals(0,			mul(ZERO, 9));
		assertEquals(0,			mul(ZERO, p9));
		assertEquals(0, 		mul(ZERO, INF));

		assertEquals(MINUS_INF,	mul(9, MINUS_INF));
		assertEquals(m81,		mul(9, m9));
		assertEquals(-81,		mul(9, -9));
		assertEquals(0,			mul(9, 0));
		assertEquals(0,			mul(9, ZERO));
		assertEquals(81,		mul(9, 9));
		assertEquals(p81,		mul(9, p9));
		assertEquals(INF,		mul(9, INF));

		assertEquals(MINUS_INF,	mul(p9, MINUS_INF));
		assertEquals(m81,		mul(p9, m9));
		assertEquals(m81,		mul(p9, -9));
		assertEquals(0,			mul(p9, 0));
		assertEquals(0,			mul(p9, ZERO));
		assertEquals(p81,		mul(p9, 9));
		assertEquals(p81,		mul(p9, p9));
		assertEquals(INF,		mul(p9, INF));

		assertEquals(MINUS_INF,	mul(INF, MINUS_INF));
		assertEquals(MINUS_INF,	mul(INF, -9));
		assertEquals(MINUS_INF,	mul(INF, m9));
		assertEquals(0,			mul(INF, ZERO));
		assertEquals(INF, 		mul(INF, 9));
		assertEquals(INF,		mul(INF, p9));
		assertEquals(INF,		mul(INF, INF));
	}

	@Test
	public void test_div() {
		Object p1 = BigDecimal.valueOf(1);
		Object m1 = BigDecimal.valueOf(-1);

		assertEquals(1,			div(MINUS_INF, MINUS_INF));
		assertEquals(INF,		div(MINUS_INF, -9));
		assertEquals(INF,		div(MINUS_INF, m9));
		assertEquals(MINUS_INF,	div(MINUS_INF, ZERO));
		assertEquals(MINUS_INF, div(MINUS_INF, 9));
		assertEquals(MINUS_INF, div(MINUS_INF, p9));
		assertEquals(-1, 		div(MINUS_INF, INF));

		assertEquals(0,			div(m9, MINUS_INF));
		assertEquals(p1,		div(m9, -9));
		assertEquals(p1,		div(m9, m9));
		assertEquals(MINUS_INF, div(m9, 0));
		assertEquals(MINUS_INF, div(m9, ZERO));
		assertEquals(m1,		div(m9, 9));
		assertEquals(m1,		div(m9, p9));
		assertEquals(0,			div(m9, INF));

		assertEquals(0,			div(-9, MINUS_INF));
		assertEquals(p1,		div(-9, m9));
		assertEquals(p1,		div(-9, -9));
		assertEquals(MINUS_INF,	div(-9, 0));
		assertEquals(MINUS_INF,	div(-9, ZERO));
		assertEquals(m1,		div(-9, 9));
		assertEquals(m1,		div(-9, p9));
		assertEquals(0, 		div(-9, INF));

		assertEquals(0,			div(0, MINUS_INF));
		assertEquals(0,			div(0, m9));
		assertEquals(0,			div(0, 0));
		assertEquals(0,			div(0, 0));
		assertEquals(0,			div(0, ZERO));
		assertEquals(0,			div(0, 9));
		assertEquals(0,			div(0, p9));
		assertEquals(0, 		div(0, INF));

		assertEquals(0,			div(ZERO, MINUS_INF));
		assertEquals(0,			div(ZERO, m9));
		assertEquals(0,			div(ZERO, 0));
		assertEquals(0,			div(ZERO, 0));
		assertEquals(0,			div(ZERO, ZERO));
		assertEquals(0,			div(ZERO, 9));
		assertEquals(0,			div(ZERO, p9));
		assertEquals(0, 		div(ZERO, INF));

		assertEquals(0,			div(9, MINUS_INF));
		assertEquals(m1,		div(9, m9));
		assertEquals(m1,		div(9, -9));
		assertEquals(INF,		div(9, 0));
		assertEquals(INF,		div(9, ZERO));
		assertEquals(p1,		div(9, 9));
		assertEquals(p1,		div(9, p9));
		assertEquals(0,			div(9, INF));

		assertEquals(0,			div(p9, MINUS_INF));
		assertEquals(m1,		div(p9, m9));
		assertEquals(m1,		div(p9, -9));
		assertEquals(INF,		div(p9, 0));
		assertEquals(INF,		div(p9, ZERO));
		assertEquals(p1,		div(p9, 9));
		assertEquals(p1,		div(p9, p9));
		assertEquals(0,			div(p9, INF));

		assertEquals(-1,		div(INF, MINUS_INF));
		assertEquals(MINUS_INF,	div(INF, -9));
		assertEquals(MINUS_INF,	div(INF, m9));
		assertEquals(INF,		div(INF, 0));
		assertEquals(INF,		div(INF, ZERO));
		assertEquals(INF, 		div(INF, 9));
		assertEquals(INF,		div(INF, p9));
		assertEquals(1,			div(INF, INF));
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
			assertThat(e, is(BlockReturnException.class));
		}
		try {
			catchMatch(new BlockReturnException(null,0), "*");
			fail();
		} catch (Throwable e) {
			assertThat(e, is(BlockReturnException.class));
		}
	}

	@Test
	public void test_toStringBD() {
		assertEquals("10000000000000000000", Ops.toStringBD(new BigDecimal("1e19")));
		assertEquals("1e20", Ops.toStringBD(new BigDecimal("1e20")));
	}

	@Test
	public void getString() {
		assertThat(Ops.get("abcd", 0), is((Object) "a"));
		assertThat(Ops.get("abcd", 3), is((Object) "d"));
		assertThat(Ops.get("abcd", 4), is((Object) ""));
		assertThat(Ops.get("abcd", -1), is((Object) "d"));
		assertThat(Ops.get("abcd", -4), is((Object) "a"));
		assertThat(Ops.get("abcd", -5), is((Object) ""));
	}
}
