package suneido.language;

import static org.junit.Assert.*;
import static suneido.Util.array;
import static suneido.language.SuClass.EACH;
import static suneido.language.SuClass.NAMED;

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
		c.put(a, s);
		SuContainer c2 = new SuContainer();
		c2.append(s);
		c2.put(x, i);
		SuContainer c3 = new SuContainer();
		c3.append(i);
		c3.append(s);
		c3.put(a, s);
		c3.put(x, i);
		SuValue[] args1 = { i, NAMED, a, s };
		SuValue[] args2 = { EACH, c };
		SuValue[] args3 = { EACH, c, EACH, c2 };
		SuValue[] locals1 = { i };
		SuValue[] locals2 = { i, s };
		SuValue[] locals4 = { c };
		SuValue[] locals5 = { c3 };
		SuValue[] locals6 = { s, null };
		FunctionSpec noParams =
				new FunctionSpec(new String[0], 0, new SuValue[0], 0);
		FunctionSpec noParams1 =
				new FunctionSpec(array("local"), 0, new SuValue[0], 0);
		FunctionSpec xParam =
				new FunctionSpec(array("x"), 1, new SuValue[0], 0);
		FunctionSpec xaParams =
				new FunctionSpec(array("x", "a"), 2, new SuValue[0], 0);
		FunctionSpec axParams =
				new FunctionSpec(array("a", "x"), 2, new SuValue[0], 0);
		FunctionSpec atParam =
				new FunctionSpec(array("@args"), 1, new SuValue[0], 0);
		FunctionSpec atParam1 =
				new FunctionSpec(array("@args", "local"), 1, new SuValue[0], 0);

		// function () () => []
		assertArrayEquals(empty, SuClass.massage(noParams, new SuValue[0]));
		assertArrayEquals(new SuValue[1], SuClass.massage(noParams1, empty));
		// function (@args) () => []
		assertArrayEquals(new SuValue[1], SuClass.massage(atParam, empty));
		assertArrayEquals(new SuValue[2], SuClass.massage(atParam1, empty));
		// function (@args) (123, a: "hello") => [[123, a: "hello]]
		assertArrayEquals(locals4, SuClass.massage(atParam, args1));
		// function (@args) (@(123, a: "hello")) => [[123, a: "hello]]
		assertArrayEquals(locals4, SuClass.massage(atParam, args2));
		// function (@args) (@(123, a: "hello"), @("hello", x: 123))
		//		=> [[123, "hello", a: "hello", x: 123]]
		assertArrayEquals(locals5, SuClass.massage(atParam, args3));
		// function (x) (123, a: "hello") => [123]
		assertArrayEquals(locals1, SuClass.massage(xParam, args1));
		// function (x, a) (123, a: "hello") => [123]
		assertArrayEquals(locals2, SuClass.massage(xaParams, args1));
		// function (a, x) (123, a: "hello") => ["hello", null]
		assertArrayEquals(locals6, SuClass.massage(axParams, args1));
		// function (x, a) (@(123, a: "hello")) => [123, "hello"]
		assertArrayEquals(locals2, SuClass.massage(xaParams, args2));
		// function (x, a) (@(123, a: "hello")) => too many arguments
		try {
			SuClass.massage(xParam, empty); // too few arguments
			fail();
		} catch (SuException e) {
		}
		try {
			SuClass.massage(noParams, new SuValue[] { s }); // too many arguments
			fail();
		} catch (SuException e) {
		}
		try {
			SuClass.massage(xParam, args3);
			fail();
		} catch (SuException e) {
		}
	}

	@Test
	public void test_new() {
		DefaultClass dc = new DefaultClass();
		SuValue instance = dc.newInstance();
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
		static final FunctionSpec noParams =
				new FunctionSpec(new String[0], 0, new SuValue[0], 0);
		@Override
		public SuClass newInstance(SuValue... args) {
			massage(noParams, args);
			return new DefaultClass();
		}
		@Override
		public String toString() {
			return "DefaultClass";
		}
	}

	@Test
	public void test_inheritance() {
		SuValue subClass = new SubClass();
		SuValue instance = subClass.newInstance();
		assertTrue(instance instanceof SubClass);
		assertEquals(SuInteger.valueOf(99), instance.invoke("Size"));
		assertEquals(SuString.EMPTY, instance.invoke("Substr"));
	}
	static class SubClass extends DefaultClass {
		static final FunctionSpec noParams =
				new FunctionSpec(new String[0], 0, new SuValue[0], 0);
		@Override
		public SuClass newInstance(SuValue... args) {
			massage(noParams, args);
			return new SubClass();
		}

		@Override
		public SuValue invoke(String method, SuValue ... args) {
			if (method == "Size")
				return SuInteger.valueOf(99);
			else
				return super.invoke(method, args);
		}
	}

	@Test
	public void test_constructor() {
		SuClass wc = new WrapClass();
		SuValue s = SuString.valueOf("hello");
		SuClass instance = wc.newInstance(s);
		assertEquals(s, instance.vars.get("value"));
	}

	static class WrapClass extends SuClass {
		{
			vars.put("Name", SuString.valueOf("Wrap"));
		}
		static final FunctionSpec params =
				new FunctionSpec(new String[] { "value" }, 1, new SuValue[0], 0);
		@Override
		public SuClass newInstance(SuValue... args) {
			massage(params, args);
			return new WrapClass(args);
		}
		WrapClass() {
		}
		WrapClass(SuValue[] args) {
			vars.put("value", args[0]);
		}

		@Override
		public String toString() {
			return "WrapClass";
		}
	}
}
