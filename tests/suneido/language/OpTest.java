package suneido.language;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

import suneido.SuException;

public class OpTest {

	boolean equals(Object x, Object y) {
		if (x == y)
			return true;
		if (x == null || y == null)
			return false;
		Class<?> xType = x.getClass();
		if (xType == Integer.class) {
			if (y.getClass() == BigDecimal.class)
				x = BigDecimal.valueOf((Integer) x);
		} else if (xType == BigDecimal.class) {
			if (y.getClass() == Integer.class)
				y = BigDecimal.valueOf((Integer) y);
		}
		return x.equals(y);
	}

	@Test
	public void test_equals() {
		assertTrue(equals(null, null));
		assertFalse(equals(null, 123));
		assertFalse(equals(123, null));
		assertTrue(equals(123, 123));
		assertTrue(equals("abc", "abc"));
		assertTrue(equals(123, BigDecimal.valueOf(123)));
		assertTrue(equals(BigDecimal.valueOf(123), 123));
	}

	int cmp(Object x, Object y) {
		if (x == y)
			return 0;
		Class<?> xType = x.getClass();
		Class<?> yType = y.getClass();
		if (xType == yType) {
			if (xType == Integer.class)
				return ((Integer) x).compareTo((Integer) y);
			if (xType == String.class)
				return ((String) x).compareTo((String) y);
			if (xType == Boolean.class)
				return ((Boolean) x).compareTo((Boolean) y);
			if (xType == BigDecimal.class)
				return ((BigDecimal) x).compareTo((BigDecimal) y);
			int xHash = x.hashCode();
			int yHash = y.hashCode();
			return xHash < yHash ? -1 : xHash > yHash ? +1 : 0;
		}
		if (xType == Boolean.class)
			return -1;
		if (xType == Integer.class) {
			if (yType == Boolean.class)
				return +1;
			if (yType == BigDecimal.class)
				return BigDecimal.valueOf((Integer) x).compareTo((BigDecimal) y);
			return -1;
		}
		if (xType == BigDecimal.class) {
			if (yType == Boolean.class)
				return +1;
			if (yType == Integer.class)
				return ((BigDecimal) x).compareTo(BigDecimal.valueOf((Integer) y));
			return -1;
		}
		if (xType == String.class) {
			if (yType == Boolean.class || yType == Integer.class
					|| yType == BigDecimal.class)
				return +1;
			return -1;
		}
		if (yType == Boolean.class || yType == Integer.class
				|| yType == BigDecimal.class || yType == String.class)
			return +1;
		int xHash = xType.hashCode();
		int yHash = yType.hashCode();
		return xHash < yHash ? -1 : xHash > yHash ? +1 : 0;
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

	String cat(Object x, Object y) {
		if (x instanceof String) {
			if (y instanceof String)
				return ((String) x).concat((String) y);
			if (y instanceof Number)
				return ((String) x).concat(y.toString());
		} else if (x instanceof Number) {
			if (y instanceof String)
				return x.toString().concat((String) y);
			if (y instanceof Number)
				return x.toString().concat(y.toString());
		}
		throw new SuException("can't concatenate " + x.getClass() + " $ "
				+ y.getClass());
	}

	@Test
	public void test_cat() {
		assertEquals("onetwo", cat("one", "two"));
		assertEquals("one0.2", cat("one", BigDecimal.valueOf(.2)));
		assertEquals("1two", cat(1, "two"));
	}

	String test(Integer x, Integer y) {
		return "" + x + y;
	}

	Number add(Object x, Object y) {
		Class<?> xType = x.getClass();
		Class<?> yType = y.getClass();
		if (xType == Integer.class) {
			if (yType == Integer.class)
				return (Integer) x + (Integer) y;
			if (yType == BigDecimal.class)
				return BigDecimal.valueOf((Integer) x).add((BigDecimal) y);
		} else if (xType == BigDecimal.class) {
			if (yType == BigDecimal.class)
				return ((BigDecimal) x).add((BigDecimal) y);
			else if (yType == Integer.class)
				return ((BigDecimal) x).add(BigDecimal.valueOf((Integer) y));
		}
		throw new SuException("can't add " + xType + " + " + yType);
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

}
