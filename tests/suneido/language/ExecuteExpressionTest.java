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
		test("f = function (@x) { x.a = 0; ++x.a }; f()", "1");
		test("f = function (@x) { x.a = 0; x.a++ }; f()", "0");
		test("f = function (@x) { x.a = 0; x.a++; x.a }; f()", "1");
		test("f = function (@x) { x.a = 0; --x.a }; f()", "-1");
		test("f = function (@x) { x.a = 0; x.a-- }; f()", "0");
		test("f = function (@x) { x.a = 0; x.a--; x.a }; f()", "-1");

		test("f = function (@x) { x[0] = 0; ++x[0] }; f()", "1");
		test("f = function (@x) { x[0] = 0; x[0]++ }; f()", "0");
		test("f = function (@x) { x[0] = 0; x[0]++; x[0] }; f()", "1");
		test("f = function (@x) { x[0] = 0; --x[0] }; f()", "-1");
		test("f = function (@x) { x[0] = 0; x[0]-- }; f()", "0");
		test("f = function (@x) { x[0] = 0; x[0]--; x[0] }; f()", "-1");

		test("f = function (@x) { x }; f(1, a: 2)", "#(1, a: 2)");
		test("f = function (@x) { x }; f(a: 1, b: 2)", "#(a: 1, b: 2)");

		test("true && false", "false");
		test("false && true", "false");
		test("true && true", "true");
		test("true || false", "true");
		test("false || true", "true");
		test("false || false", "false");
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
