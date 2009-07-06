package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuValue;

public class SuMethodTest {
	@Test
	public void test() {
		SuValue x = new TestClass();
		SuValue m = new SuMethod(x, "Size");
		Object result = Ops.call(m);
		assertEquals(0, result);
	}
}
