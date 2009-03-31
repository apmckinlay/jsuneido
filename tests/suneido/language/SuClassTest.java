package suneido.language;

import static org.junit.Assert.*;
import static suneido.Util.array;
import static suneido.language.FunctionSpec.noParams;
import static suneido.language.SuClass.EACH;
import static suneido.language.SuClass.NAMED;

import java.util.ArrayList;
import java.util.Arrays;

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
		SuValue i = SuInteger.valueOf(6);
		SuValue s = SuString.valueOf("hello");
		SuString a = SuString.valueOf("a");
		SuString x = SuString.valueOf("x");
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
	private void good(FunctionSpec f, SuValue[] args, SuValue[] locals) {
		assertArrayEquals(locals, SuClass.massage(f, args));
	}
	private void bad(FunctionSpec f, SuValue[] args) {
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
		SuValue[] defaults = defaults(locals, params);
		return new FunctionSpec("", locals, params.length, defaults,
				defaults.length, atParam);
	}
	private SuValue[] defaults(String[] locals, String... params) {
		ArrayList<SuValue> defaults = new ArrayList<SuValue>();
		int j;
		for (int i = 0; i < params.length; ++i)
			if (-1 != (j = params[i].indexOf('='))) {
				locals[i] = params[i].substring(0, j);
				String s = params[i].substring(j + 1);
				defaults.add(Character.isDigit(s.charAt(0))
						? SuInteger.valueOf(s) : SuString.valueOf(s));
			}
		return defaults.toArray(new SuValue[0]);
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
	static class DefaultClass extends SampleClass {
		public static SuValue[] args;
		@Override
		public SuValue methodDefault(SuValue[] args) {
			DefaultClass.args = args;
			return SuString.EMPTY;
		}
		@Override
		public SuClass newInstance(SuValue... args) {
			massage(noParams, args);
			return new DefaultClass();
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

	static class WrapClass extends SampleClass {
		{
			vars.put("Name", SuString.valueOf("Wrap"));
		}
		static final FunctionSpec params = new FunctionSpec("",
				new String[] { "value" }, 1);
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
	}
}
