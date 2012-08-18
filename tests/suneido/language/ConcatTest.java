package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.base.Strings;

public class ConcatTest {

	@Test
	public void test_cat() {
		Object x = Ops.cat("hello", "world");
		assertTrue(x instanceof String);
		assertEquals("helloworld", x);

		String s = Strings.repeat("helloworld", 30);
		x = Ops.cat(s, ".");
		assertTrue(x instanceof Concat);
		assertEquals(s + ".", x.toString());

		x = Ops.cat(x, ">");
		x = Ops.cat("<", x);
		assertTrue(x instanceof Concat);
		assertEquals("<" + s + "." + ">", x.toString());
		}

	@Test
	public void test_is() {
		Object x = new Concat("hello", "world");
		assertTrue(Ops.is(x, "helloworld"));
		assertTrue(Ops.is("helloworld", x));
	}

	@Test
	public void test_cmp() {
		Object x = new Concat("hello", "world");
		Object y = new Concat("hello", "world");
		assertEquals(0, Ops.cmp(x, y));
		assertEquals(0, Ops.cmp(x, "helloworld"));
		assertEquals(0, Ops.cmp("helloworld", x));
		y = new Concat("fred", "dy");
		assertEquals(+1, Integer.signum(Ops.cmp(x, y)));
		assertEquals(-1, Integer.signum(Ops.cmp(y, x)));
	}

	@Test
	public void equals_bug() {
		Object x = new Concat("hello", "world");
		Object y = new Concat("hello", "world");
		assertEquals(x, y);
	}

}
