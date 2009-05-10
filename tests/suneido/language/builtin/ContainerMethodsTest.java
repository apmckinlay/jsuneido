package suneido.language.builtin;

import static org.junit.Assert.assertEquals;
import static suneido.language.Compiler.eval;
import static suneido.language.Ops.display;

import org.junit.Test;

public class ContainerMethodsTest {

	@Test
	public void test_add() {
		test("[].Add(123)", "[123]");
		test("[].Add(123)", "[123]");
		test("[].Add(1,2,3)", "[1, 2, 3]");
		test("[1,2].Add(3,4)", "[1, 2, 3, 4]");
		test("[1,2,3].Add(11,22,at: 1)", "[1, 11, 22, 2, 3]");
		test("[].Add(123, at: 'a')", "[a: 123]");
	}

	public static void test(String expr, String result) {
		assertEquals(result, display(eval(expr)));
	}

}
