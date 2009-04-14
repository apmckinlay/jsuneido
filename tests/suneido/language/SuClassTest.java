package suneido.language;

import static org.junit.Assert.*;

import org.junit.Test;

import suneido.SuException;
import suneido.SuValue;

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
