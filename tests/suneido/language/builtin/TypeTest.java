package suneido.language.builtin;

import static org.junit.Assert.assertEquals;
import static suneido.language.Compiler.eval;

import org.junit.Test;

public class TypeTest {
	@Test
	public void test() {
		test("Type(123)", "Number");
		test("Type('sss')", "String");
		test("Type(1.2)", "Number");
		test("Type(#())", "Object");
		test("Type(#{})", "Record");
		test("Type({|x| })", "Block");
		test("Type(class { })", "Class");
		test("Type(new class { })", "Instance");
		test("Type(function () { })", "Function");
	}

	public static void test(String expr, String result) {
		assertEquals(result, eval(expr));
	}
}
