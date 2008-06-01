package suneido;

import suneido.SuClassTest.TestClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SuMethodTest {
	@Test
	public void test() {
		SuValue x = new TestClass();
		SuValue m = new SuMethod(x, 5678);
		SuValue result = m.invoke(Symbols.CALLi);
		assertEquals(SuInteger.ZERO, result);
	}
}
