package suneido.language;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

import suneido.SuException;
import suneido.SuValue;

public class SuClassTest {

	private static final Object[] noArgs = new Object[0];

	@Test
	public void test() {
		SuValue c = new TestClass();
		assertEquals("", c.invoke("Substr", noArgs));
		assertEquals(0, c.invoke("Size", noArgs));
	}

	@Test(expected=SuException.class)
	public void unknown() {
		new TestClass().invoke("Foo", noArgs);
	}

	@Test
	public void test_new() {
		DefaultClass dc = new DefaultClass();
		SuValue instance = new SuInstance(dc);
		assertEquals("", instance.invoke(instance, "Substr", noArgs));
		assertArrayEquals(new Object[] { "Substr" }, DefaultClass.args);
		instance.invoke(instance, "Substr", new Object[] { 1 });
		assertArrayEquals(new Object[] { "Substr", 1 }, DefaultClass.args);
	}
	static class DefaultClass extends SampleClass {
		public static Object[] args;

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "Default") {
				DefaultClass.args = args;
				return "";
			}
			return super.invoke(self, method, args);
		}
	}

	@Test
	public void test_inheritance() {
		Object subClass = new SubClass();
		Object instance = Ops.invoke(subClass, "<new>");
		assertTrue(instance instanceof SuInstance);
		assertEquals(99, Ops.invoke(instance, "Size"));
		assertEquals("", Ops.invoke(instance, "Substr"));
	}
	static class SubClass extends DefaultClass {
		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "Size")
				return 99;
			else
				return super.invoke(self, method, args);
		}
	}

	@Test
	public void test_constructor() {
		SuClass wc = new WrapClass();
		Object s = "hello";
		SuInstance instance = (SuInstance) wc.invoke(wc, "<new>", s);
		assertEquals(s, instance.get("value"));
	}

	static class WrapClass extends SampleClass {
		{
			vars = new HashMap<String, Object>();
			vars.put("Name", "Wrap");
		}
		static final FunctionSpec params = new FunctionSpec("",
				new String[] { "value" }, 1);

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "<new>") {
				SuInstance wc = new SuInstance(this);
				wc.put("value", args[0]);
				return wc;
			} else
				return super.invoke(self, method, args);
		}
	}
}
