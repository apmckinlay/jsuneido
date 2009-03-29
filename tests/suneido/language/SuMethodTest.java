package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuInteger;
import suneido.SuValue;

public class SuMethodTest {
	@Test
	public void test() {
		SuValue x = new TestClass();
		SuValue m = new SuMethod(x, "Size");
		SuValue result = m.invokeN();
		assertEquals(SuInteger.ZERO, result);
	}
}
