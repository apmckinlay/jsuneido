package suneido.language;

import static org.junit.Assert.*;
import static suneido.language.SuClass.*;

import org.junit.Test;

import suneido.*;

public class SuClassTest {
	@Test
	public void test() {
		SuValue c = new TestClass();
		assertEquals(SuString.EMPTY, c.invoke("Substr"));
		assertEquals(SuInteger.ZERO, c.invoke("Size"));
	}

	@Test(expected=SuException.class)
	public void unknown() {
		new TestClass().invoke("Foo");
	}

	@Test
	public void massage() {
		SuValue[] empty = new SuValue[0];
		SuValue i = SuInteger.valueOf(123);
		SuValue s = SuString.valueOf("hello");
		SuString a = SuString.valueOf("a");
		SuString x = SuString.valueOf("x");
		SuContainer c = new SuContainer();
		c.append(i);
		c.putdata(a, s);
		SuContainer c2 = new SuContainer();
		c2.append(s);
		c2.putdata(x, i);
		SuContainer c3 = new SuContainer();
		c3.append(i);
		c3.append(s);
		c3.putdata(a, s);
		c3.putdata(x, i);
		SuValue[] args1 = { i, NAMED, a, s };
		SuValue[] args2 = { EACH, c };
		SuValue[] args3 = { EACH, c, EACH, c2 };
		SuValue[] locals1 = { i };
		SuValue[] locals2 = { i, s };
		SuValue[] locals4 = { c };
		SuValue[] locals5 = { c3 };
		SuValue[] locals6 = { s, null };

		// function () () => []
		assertArrayEquals(empty, SuClass.massage(0, new SuValue[0]));
		assertArrayEquals(new SuValue[1], SuClass.massage(1, empty));
		// function (@args) () => []
		assertArrayEquals(empty, SuClass.massage(0, empty, EACH.string()));
		assertArrayEquals(new SuValue[1], SuClass.massage(1, empty,
				EACH.string()));
		// function (@args) (123, a: "hello") => [[123, a: "hello]]
		assertArrayEquals(locals4, SuClass.massage(1, args1, EACH.string()));
		// function (@args) (@(123, a: "hello")) => [[123, a: "hello]]
		assertArrayEquals(locals4, SuClass.massage(1, args2, EACH.string()));
		// function (@args) (@(123, a: "hello"), @("hello", x: 123))
		//		=> [[123, "hello", a: "hello", x: 123]]
		assertArrayEquals(locals5, SuClass.massage(1, args3, EACH.string()));
		// function (x) (123, a: "hello") => [123]
		assertArrayEquals(locals1, SuClass.massage(1, args1, "x"));
		// function (x, a) (123, a: "hello") => [123]
		assertArrayEquals(locals2, SuClass.massage(2, args1, "x", "a"));
		// function (a, x) (123, a: "hello") => ["hello", null]
		assertArrayEquals(locals6, SuClass.massage(2, args1, "a", "x"));
		// function (x, a) (@(123, a: "hello")) => [123, "hello"]
		assertArrayEquals(locals2, SuClass.massage(2, args2, "x", "a"));
		// function (x, a) (@(123, a: "hello")) => too many arguments
		try {
			SuClass.massage(1, args3, "x");
			fail();
		} catch (SuException e) { }
	}

	@Test
	public void test_new() {
		DefaultClass dc = new DefaultClass();
		SuValue instance = dc.invoke(NEW);
		assertEquals(SuString.EMPTY, instance.invoke("Substr"));
		assertArrayEquals(new SuValue[] { SuString.valueOf("Substr") },
				DefaultClass.args);
		instance.invoke("Substr", SuInteger.ONE);
		assertArrayEquals(
				new SuValue[] { SuString.valueOf("Substr"), SuInteger.ONE },
				DefaultClass.args);
	}
	static class DefaultClass extends SuClass {
		public static SuValue[] args;
		@Override
		public SuValue methodDefault(SuValue[] args) {
			DefaultClass.args = args;
			return SuString.EMPTY;
		}
		@Override
		SuClass createInstance() {
			return new DefaultClass();
		}
	}

	@Test
	public void test_inheritance() {
		SuValue subClass = new SubClass();
		SuValue instance = subClass.invoke(NEW);
		assertTrue(instance instanceof SubClass);
		assertEquals(SuInteger.valueOf(99), instance.invoke("Size"));
		assertEquals(SuString.EMPTY, instance.invoke("Substr"));
	}
	static class SubClass extends DefaultClass {
		@Override
		public SuValue invoke(String method, SuValue ... args) {
			if (method == "Size")
				return SuInteger.valueOf(99);
			else
				return super.invoke(method, args);
		}
		@Override
		SuClass createInstance() {
			return new SubClass();
		}
	}
}
