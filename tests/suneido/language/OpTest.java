package suneido.language;

import static org.junit.Assert.*;
import static suneido.language.Ops.*;

import java.math.BigDecimal;

import org.junit.Test;

import suneido.SuException;

public class OpTest {

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

	@Test
	public void test_add() {
		Object x = 123;
		Object y = 456;
		assertEquals(579, add(x, y));
		Object z = BigDecimal.valueOf(.9);
		assertEquals(456.9, add(z, y));
		assertEquals(123.9, add(x, z));
	}

	@Test
	public void test_catchMatch() {
		match("abc", null);
		match("abc", "a");
		nomatch("abc", "b");
		match("abc", "b|ab");
		nomatch("abc", "x|y|z");
		match("abc", "*bc");
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
}
