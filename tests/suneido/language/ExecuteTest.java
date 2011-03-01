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
		test("f = function (adv?) { adv? }; f(adv?:)", "true");
		test("f = function (@args) { args.adv! }; f(adv!: false)", "false");
		test("f = function (a, b, c = 3, d = 4) { [a,b,c,d] }; f(1, 2, d: 9)",
				"[1, 2, 3, 9]");
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
		test("true ? x = 1 : x = 2", "1");
		test("true ? x = 1 : x = 2; x", "1");
	}
	@Test public void test_if() {
		test("if (true) x = 1; else x = 2; x", "1");
		test("if (false) x = 1; else x = 2; x", "2");
		test("if (true) x = 1; else x = 2; x", "1");
		test("if (x = true) y = 123; else y = 456; y", "123");
	}
	@Test public void test_lt() {
		test("2 < 3", "true");
		test("3 < 2", "false");
		test("2 < 2", "false");
		test("1.1 < 1.2", "true");
		test("1.2 < 1.1", "false");
		test("[99] < [.5]", "false");
		test("#{99} < #(.5)", "false");
		test("#{.5} < #(99)", "true");
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
		test("x=''; switch(1) { case 0: ; case 1: x='one'; }; x", "'one'");
		test("x=''; switch(1) { case 0,1,2: x='one'; }; x", "'one'");
		test("x=''; switch(2) { case 1: x='one'; default: x='def' }; x", "'def'");
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
		test("run = function (block) { block() }; run() { run() { return 123 } }; 456", "123");

		test("b = { break }; try b() catch (e) return e", "'block:break'");
		test("b = { continue }; try b() catch (e) return e", "'block:continue'");

		test("b = { forever break; 123 }; b()", "123");
		test("b1 = {|f| this; b2 = { f }; b2() }; b1(123)", "123");
	}
	@Test public void test_exceptions() {
		test("try return 123", "123");
		test("try return 123 catch ;", "123");
		test("try throw 'abc' catch (e) return e", "'abc'");
		test("try throw 'x' catch (e) { return e } return 'y'", "'x'");
		test("try { try throw 'x' catch (e) return e } return 'y'", "'x'");
		test("try { " +
				"try Object() catch (e) return 'y'; " +
				"throw 'x' } catch(e) return e; " +
				"return 'y'", "'x'");
		blockReturn("f = function () { return { return 123 } }; b = f(); b()");

		test("try throw 'abc' catch (e) return Type(e)", "'Except'");
		test("try throw 'abc' catch (e) return e.As('def')", "'def'");
		test("try { try throw 'abc' catch (e) throw e.As('def') } " +
				"catch (x) return x", "'def'");
		test("c = 0;" +
				"try { try throw 'abc' catch (e) { c = e.Calls(); throw e.As('def') } } " +
				"catch (x) { return c is x.Calls() }", "true");
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
		test("c = class { }; new c", "eval$c()");
		test("c = class { F() { 123 } }; c.F()", "123");
	}

	@Test public void test_function_in_object() {
		test("x = #(F: function (n) { n + 1 }); (x.F)(123)", "124");
	}

	@Test
	public void test_eval() {
		test("#(1).Eval(function () { this })", "#(1)");
		test("#(A: 123).Eval(function () { .A })", "123");
		test("#(a: 123).Eval(function () { .a })", "123");

		def("F", "function () { b = { .a }; b() }");
		test("[a: 123].Eval(F)", "123");

		test("#(1).Eval({ this })", "#(1)");
		test("#(a: 123).Eval({ .a })", "123");
	}

	@Test
	public void test_naming() {
		def("F", "function () { }");
		test("F", "F");

		def("C", "class { }");
		test("C", "C");
		test("C()", "C()");

		def("C", "class { M() { } }");
		test("C.M", "C.M");
	}

	@Test
	public void test_params() {
		test("f = function () { }; f.Params()", "'()'");
		test("f = function (a, b = 0) { }; f.Params()", "'(a,b=0)'");
		test("c = class { F() { } }; c.F.Params()", "'()'");
	}

	private void def(String name, String source) {
		Globals.put(name, Compiler.compile(name, source));
	}

	public static void test(String expr, String expected) {
		Object result = eval(expr);
		if (result instanceof SuBoundMethod)
			result = ((SuBoundMethod) result).method;
		assertEquals(expected, display(result));
	}

}
