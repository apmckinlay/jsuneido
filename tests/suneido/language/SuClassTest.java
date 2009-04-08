package suneido.language;

import static org.junit.Assert.*;
import static suneido.language.SuClass.SpecialArg.EACH;
import static suneido.language.SuClass.SpecialArg.NAMED;
import static suneido.util.Util.array;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import suneido.*;

public class SuClassTest {
	@Test
	public void test() {
		SuValue c = new TestClass();
		assertEquals("", c.invoke("Substr"));
		assertEquals(0, c.invoke("Size"));
	}

	@Test(expected=SuException.class)
	public void unknown() {
		new TestClass().invoke("Foo");
	}

	@Test
	public void massage() {
		Object[] empty = new Object[0];
		Object i = 6;
		Object s = "hello";
		String a = "a";
		String x = "x";
		SuContainer c = new SuContainer();
		SuContainer ias = new SuContainer();
		ias.append(i);
		ias.put(a, s);
		SuContainer sxi = new SuContainer();
		sxi.append(s);
		sxi.put(x, i);
		SuContainer isasxi = new SuContainer();
		isasxi.append(i);
		isasxi.append(s);
		isasxi.put(a, s);
		isasxi.put(x, i);

		//	 params					args							resulting locals
		good(f(),					empty,							empty);
		good(f(1),					empty,							new SuValue[1]);
		good(f("@args"),			empty,							array(c));
		good(f(1, "@args"),			empty,							array(c, null));
		good(f("@args"),			array(i, NAMED, a, s),			array(ias));
		good(f("@args"),			array(EACH, ias),				array(ias));
		good(f("@args"),			array(EACH, ias, EACH, sxi),	array(isasxi));
		good(f("x"),				array(i, NAMED, a, s),			array(i));
		good(f("x", "a"),			array(i, s),					array(i, s));
		good(f("x", "a"),			array(i, NAMED, a, s),			array(i, s));
		good(f("x", "a"),			array(EACH, ias),				array(i, s));
		good(f(1, "x"),				array(EACH, sxi),				array(i, null));
		good(f("x=6"),				empty,							array(i));
		good(f("a", "x=6"), 		array(s), 						array(s, i));
		good(f("a", "x=6"), 		array(i, s), 					array(i, s));
		good(f("a", "y=6", "x=2"),	array(i, NAMED, x, s), 			array(i, i, s));

		bad(f("x", "a"),	array(i, NAMED, x, s)); // missing a
		bad(f("x"),			empty); // too few arguments
		bad(f(),			array(s)); // too many arguments
		bad(f("x"),			array(EACH, ias, EACH, sxi)); // too many arguments
	}
	private void good(FunctionSpec f, Object[] args, Object[] locals) {
		assertArrayEquals(locals, SuClass.massage(f, args));
	}
	private void bad(FunctionSpec f, Object[] args) {
		try {
			SuClass.massage(f, args);
			fail();
		} catch (SuException e) {
		}
	}
	private FunctionSpec f(String... params) {
		return f(0, params);
	}
	private FunctionSpec f(int extra, String... params) {
		boolean atParam = (params.length == 1 && params[0].startsWith("@"));
		if (atParam)
			params[0] = params[0].substring(1, params[0].length());
		String[] locals = Arrays.copyOf(params, params.length + extra);
		for (int i = 0; i < extra; ++i)
			locals[params.length + i] = "local" + i;
		Object[] defaults = defaults(locals, params);
		return new FunctionSpec("", locals, params.length, defaults,
				defaults.length, atParam);
	}
	private Object[] defaults(String[] locals, String... params) {
		ArrayList<Object> defaults = new ArrayList<Object>();
		int j;
		for (int i = 0; i < params.length; ++i)
			if (-1 != (j = params[i].indexOf('='))) {
				locals[i] = params[i].substring(0, j);
				String s = params[i].substring(j + 1);
				defaults.add(Character.isDigit(s.charAt(0))
						? Ops.stringToNumber(s) : s);
			}
		return defaults.toArray(new Object[0]);
	}

	@Test
	public void test_new() {
		DefaultClass dc = new DefaultClass();
		SuValue instance = dc.newInstance();
		assertEquals("", instance.invoke("Substr"));
		assertArrayEquals(new Object[] { "Substr" }, DefaultClass.args);
		instance.invoke("Substr", 1);
		assertArrayEquals(new Object[] { "Substr", 1 }, DefaultClass.args);
	}
	static class DefaultClass extends SampleClass {
		public static Object[] args;
		@Override
		public Object methodDefault(Object[] args) {
			DefaultClass.args = args;
			return "";
		}
		@Override
		public SuClass newInstance() {
			return new DefaultClass();
		}
	}

	@Test
	public void test_inheritance() {
		Object subClass = new SubClass();
		Object instance = Ops.invoke(subClass, "<new>");
		assertTrue(instance instanceof SubClass);
		assertEquals(99, Ops.invoke(instance, "Size"));
		assertEquals("", Ops.invoke(instance, "Substr"));
	}
	static class SubClass extends DefaultClass {
		@Override
		public SuClass newInstance() {
			return new SubClass();
		}

		@Override
		public Object invoke(String method, Object... args) {
			if (method == "Size")
				return 99;
			else
				return super.invoke(method, args);
		}
	}

	@Test
	public void test_constructor() {
		SuClass wc = new WrapClass();
		Object s = "hello";
		SuClass instance = (SuClass) wc.invoke("<new>", s);
		assertEquals(s, instance.vars.get("value"));
	}

	static class WrapClass extends SampleClass {
		{
			vars.put("Name", "Wrap");
		}
		static final FunctionSpec params = new FunctionSpec("",
				new String[] { "value" }, 1);
		@Override
		public SuClass newInstance() {
			return new WrapClass();
		}

		@Override
		public Object invoke(String method, Object... args) {
			if (method == "<new>") {
				WrapClass wc = (WrapClass) super.invoke(method);
				wc.vars.put("value", args[0]);
				return wc;
			} else
				return super.invoke(method, args);
		}
	}
}
