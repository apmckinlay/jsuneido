package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuValue;

public class ExecuteExpressionTest {

	@Test
	public void tests() {
		test("123 + 456", "579");
		test("'hello' $ ' ' $ 'world'", "'hello world'");
		test("1 + 2 * 3", "7");
		test("'hello'.Size()", "5");
		test("'hello'.Substr(3, 2)", "'lo'");
		test("x = 123; y = 456; return x + y", "579");
		test("f = function (x, y) { x + y }; f(123, 456)", "579");
		test("'hello world'.Size()", "11");
		test("s = 'hello'; s.Substr(s.Size() - 2, 99)", "'lo'");
		test("f = function (@x) { x }; f()", "#()");
	}

	private static void test(String expr, String result) {
		assertEquals(result, eval(expr).toString());
	}

	private static SuValue eval(String s) {
		SuValue f = compile("function () { " + s + " }");
		SuValue[] locals = new SuValue[0];
		return f.invoke("call", locals);
	}

	private static SuValue compile(String s) {
		Lexer lexer = new Lexer(s);
		CompileGenerator generator = new CompileGenerator();
		ParseFunction<Object, Generator<Object>> pc =
				new ParseFunction<Object, Generator<Object>>(lexer, generator);
		return (SuValue) pc.parse();
	}
}
