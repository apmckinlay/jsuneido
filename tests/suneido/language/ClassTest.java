package suneido.language;

import static org.junit.Assert.fail;
import static suneido.language.Compiler.compile;
import static suneido.language.Compiler.eval;
import static suneido.language.ExecuteTest.test;

import org.junit.Test;

import suneido.SuException;

public class ClassTest {
	@Test
	public void test1() {
		define("A", "class { }");
		test("new A", "A()");

		define("A", "class { N: 123 }");
		test("A.N", "123");
		notFound("A.M");

		define("A", "class { F() { 123 } }");
		test("A().F()", "123");

		define("A", "class { F?() { 123 } G!() { 456 } }");
		test("A.F?()", "123");
		test("A.G!()", "456");

		define("A", "class { F() { .g() } g() { 123 } }");
		test("A().F()", "123");

		define("A", "class { F() { .x } x: 123 }");
		test("A().F()", "123");

		define("A", "class { New(x) { .X = x } }");
		test("A(456).X", "456");

		define("A", "class { Default(method) { 'missing: ' $ method } }");
		test("A().F()", "'missing: F'");

		define("A", "class { F(n) { n * 2 } }");
		define("B", "A { }");
		test("B().F(123)", "246");

		define("A", "class { G() { .x = 456 } }");
		define("B", "A { F() { .x = 123; .G(); .x } }");
		test("B().F()", "123");

		define("A", "class { G() { .X = 123 } }");
		define("B", "A { F() { .G(); .X } }");
		test("B().F()", "123");

		define("A", "class { Call() { 123 } CallClass() { 456 } }");
		test("A()", "456");
		test("a = new A; a()", "123");

		define("A", "class { N: 123 F() { .N } }");
		test("A.N", "123");
		test("A.F()", "123");
		test("A().N", "123");
		test("A().F()", "123");
		define("B", "A { G() { .N } }");
		test("B.N", "123");
		test("B.F()", "123");
		test("B.G()", "123");
		test("B().N", "123");
		test("B().F()", "123");
		test("B().G()", "123");

		define("A", "class { New() { .A = 123 } }");
		define("B", "A { New() { .B = 456 } }");
		test("b = B(); b.A + b.B", "579");

		define("A", "class { New(n) { .A = n } }");
		define("B", "A { New() { super(123) } }");
		test("B().A", "123");

		define("A", "class { F() { 123 } }");
		define("B", "A { F() { 456 } G() { super.F() } }");
		test("B().G()", "123");

		define("A", "class { B: class { F() { 123 } } }");
		test("(new A.B).F()", "123");
		test("new A.B", "A_c0()");

		define("A", "class { F() { 123 } N: 123 }");
		define("B", "A { }");
		test("A.F", "A.F");
		test("B.F", "B.F");
		test("B.N", "123");
		notFound("B.M");

		define("A", "class { New(args) { super(@args) } }");

		define("A", "class { ToString() { 'an A' } }");
		test("A()", "an A");
		define("A", "class { New(n) { .n = n } ToString() { 'A' $ .n } }");
		test("A(123)", "A123");

		define("A", "class { CallClass() { 123 } }");
		define("B", "A { }");
		test("A()", "123");
		test("B()", "123");

		define("A", "class { F() { } }");
		define("B", "A { G() { } }");
		test("B.Members()", "#('G')");

		define("A", "class { F() { b = { .G() }; b() } }");
		define("B", "A { G() { 123 } }");
		test("B.F()", "123");
	}
	@Test public void test_static_getter() {
		define("A", "class { " +
				"Get_N() { 'getter' }" +
				"Get_(m) { 'get ' $ m }" +
				" }");
		test("A.N", "'getter'");
		test("A.X", "'get X'");
		define("B", "A { }");
		test("B.N", "'getter'");
		test("B.X", "'get X'");
	}
	@Test public void test_instance_getter() {
		define("A", "class { "
				+ "New(x) { .X = x } "
				+ "Get_N() { .X $ ' getter' } "
				+ "Get_(m) { .X $ ' get ' $ m } "
				+ "}");
		test("A(1).N", "'1 getter'");
		test("A(1).Z", "'1 get Z'");
		define("B", "A { }");
		test("B(2).N", "'2 getter'");
		test("B(2).Z", "'2 get Z'");
	}
	@Test public void test_private_instance_getter() {
		define("A", "class { "
				+ "New(x) { .x = x } "
				+ "get_n() { .x $ ' getter' } "
				+ "N() { .n }"
				+ "Z() { .z }"
				+ "}");
		test("A(1).N()", "'1 getter'");
	}

	@Test
	public void test_eval() {
		define("F", "function () { this }");
		test("#(1).Eval(F)", "#(1)");

		define("F", "class { CallClass() { this } }");
		test("#(1).Eval(F)", "#(1)");
	}

	private static void notFound(String expr) {
		try {
			eval(expr);
			fail();
		} catch (SuException e) {
			assert e.toString().startsWith("member not found");
		}
	}

	void define(String name, String definition) {
		Globals.put(name, compile(name, definition));
	}
}
