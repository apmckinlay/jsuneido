package suneido.language;

import static org.junit.Assert.assertEquals;
import static suneido.language.Ops.display;

import org.junit.Test;

public class ExecuteTest {

	@Test public void tests() {
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
	@Test public void test_incdec() {
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
	}
	@Test public void test_args() {
		test("f = function (@x) { x }; f(1, a: 2)", "#(1, a: 2)");
		test("f = function (@x) { x }; f(a: 1, b: 2)", "#(b: 2, a: 1)");
	}
	@Test public void test_and() {
		test("true && false", "false");
		test("false && true", "false");
		test("true && true", "true");
	}
	@Test public void test_or() {
		test("true || false", "true");
		test("false || true", "true");
		test("false || false", "false");
	}
	@Test public void test_conditional() {
		test("true ? 123 : 456", "123");
		test("false ? 123 : 456", "456");
		test("true ? x = 1 : 2", "1");
		test("true ? x = 1 : 2", "1");
		test("true ? x = 1 : 2; x", "1");
	}
	@Test public void test_if() {
		test("if (true) x = 1; x", "1");
		test("if (false) x = 1; else x = 2; x", "2");
		test("if (true) x = 1; else x = 2; x", "1");
		test("if (x = true) y = 123; y", "123");
	}
	@Test public void test_lt() {
		test("2 < 3", "true");
		test("3 < 2", "false");
		test("2 < 2", "false");
	}
	@Test public void test_lte() {
		test("2 <= 3", "true");
		test("3 <= 2", "false");
		test("2 <= 2", "true");
	}
	@Test public void test_gt() {
		test("2 > 3", "false");
		test("3 > 2", "true");
		test("2 > 2", "false");
	}
	@Test public void test_gte() {
		test("2 >= 3", "false");
		test("3 >= 2", "true");
		test("2 >= 2", "true");
	}
	@Test public void test_while() {
		test("i = 0; while (i < 5) i += 2; i", "6");
		test("i = 6; while (i < 5) i += 2; i", "6");
	}
	@Test public void test_do_while() {
		test("i = 0; do i += 2; while (i < 5); i", "6");
		test("i = 6; do i += 2; while (i < 5); i", "8");
	}
	@Test public void test_assignOpOrder() {
		test("s = 1; s $= 2", "'12'");
		test("n = 10; n -= 5", "5");
	}
	@Test public void test_forever_break() {
		test("i = 0; forever { ++i; if (i > 4) break }; i", "5");
	}
	@Test public void test_for_classic() {
		test("for (i = 0; i < 5; ++i) ; i", "5");
		test("i = 0; for (; i < 5; ++i) ; i", "5");
		test("for (i = 0; i < 5; ) ++i; i", "5");
		test("for (i = 0; ; ++i) if (i > 5) break; i", "6");
	}
	@Test public void test_switch() {
		test("switch(1) { case 0: ; case 1: x='one'; }; x", "'one'");
		test("switch(1) { case 0,1,2: x='one'; }; x", "'one'");
		test("switch(2) { case 1: x='one'; default: x='def' }; x", "'def'");
	}

	private static void test(String expr, String result) {
		assertEquals(result, display(eval(expr)));
	}

	private static Object eval(String s) {
		Object f = compile("function () { " + s + " }");
		Object[] locals = new Object[0];
		return Ops.invoke(f, "call", locals);
	}

	private static Object compile(String s) {
		Lexer lexer = new Lexer(s);
		CompileGenerator generator = new CompileGenerator();
		ParseFunction<Object, Generator<Object>> pc =
				new ParseFunction<Object, Generator<Object>>(lexer, generator);
		return pc.parse();
	}
}
