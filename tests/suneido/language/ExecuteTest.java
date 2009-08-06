package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static suneido.language.Compiler.eval;
import static suneido.language.Ops.display;

import org.junit.Before;
import org.junit.Test;

public class ExecuteTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@Test public void tests() {
		test("123 + 456", "579");
		test("'hello' $ ' ' $ 'world'", "'hello world'");
		test("1 + 2 * 3", "7");
		test("a = 2; -a", "-2");
		test("a = true; not a", "false");
		test("'hello'.Size()", "5");
		test("'hello'.Substr(3, 2)", "'lo'");
		test("x = 123; y = 456; return x + y", "579");
		test("f = function (x, y) { x + y }; f(123, 456)", "579");
		test("'hello world'.Size()", "11");
		test("s = 'hello'; s.Substr(s.Size() - 2, 99)", "'lo'");
		test("f = function (@x) { x }; f()", "#()");
	}
	@Test public void test_bigdecimal_is() {
		test("1000 is 1e3", "true");
		test("1e3 is 1000", "true");
		test("10e3 is 1e4", "true");
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
		test("f = function (@x) { x }; f(a: 1, b: 2, c: 3, d: 4, e: 5, f: 6"
				+ "g: 7, h: 8, i: 9, j: 10, k: 11)",
				"#(f: 6, g: 7, d: 4, e: 5, b: 2, c: 3, a: 1, j: 10, k: 11, h: 8, i: 9)");
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
	@Test public void test_block() {
		test("b = { 123 }; b()", "123");
		test("a = 123; b = { a + 456 }; b()", "579");
		test("b = {|x| x * 2 }; b(123)", "246");
		test("x = 111; b = {|x| x * 2 }; b(123) + x", "357");
		test("b = { 2 * it }; b(123)", "246");
		test("b = {|it| 2 * it }; b(123)", "246");
		test("it = 111; b = {|it| 2 * it }; b(123) + it", "357");
		test("function () { b = { return 123 }; b(); 456 }()", "123");
		test("function () { b = { do { return 123 } while(false) }; b(); 456 }()",
				"123");
		test("function () { b = { return 123 }; do { b() } while(false); 456 }()",
				"123");
	}
	@Test public void test_exceptions() {
		test("try return 123", "123");
		test("try return 123 catch ;", "123");
		test("try throw 'abc' catch (e) return e", "'abc'");
		test("try { try throw 'x' catch (e) return e } return 'y'", "'x'");
		test("try { " +
				"try 123 catch (e) return 'y'; " +
				"throw 'x' } catch(e) return e; " +
				"return 'y'", "'x'");
		blockReturn("f = function () { return { return 123 } }; b = f(); b()");
	}
	private static void blockReturn(String expr) {
		try {
			eval(expr);
			fail();
		} catch (BlockReturnException e) {
			// expected
		}
	}

	@Test public void test_nested_class() {
		test("c = class { }; new c", "eval_c0()");
		test("c = class { F() { 123 } }; c.F()", "123");
	}

	@Test public void test_function_in_object() {
		test("x = #(F: function (n) { n + 1 }); (x.F)(123)", "124");
	}

	@Test
	public void test_eval() {
		test("#(1).Eval(function () { this })", "#(1)");
		test("#(1).Eval({ this })", "#(1)");
		test("#(A: 123).Eval(function () { .A })", "123");
		test("#(a: 123).Eval(function () { .a })", "123");
	}

	public static void test(String expr, String result) {
		assertEquals(result, display(eval(expr)));
	}

}
