package suneido;

import suneido.SuClassTest.TestClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static suneido.Symbols.*;

public class SuMethodTest {
	@Test
	public void test() {
		SuValue x = new TestClass();
		SuValue m = new SuMethod(x, Num.SIZE);
		SuValue result = m.invoke(Num.CALL);
		assertEquals(SuInteger.ZERO, result);
	}
}
