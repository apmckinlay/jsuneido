package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static suneido.language.Compiler.compile;
import static suneido.language.Compiler.eval;
import static suneido.language.ExecuteTest.test;

import org.junit.Before;
import org.junit.Test;

import suneido.Suneido;

public class ClassTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@Test
	public void test1() {
		define("A", "class { }");
		test("new A", "A()");

		define("A", "class { N: 123 }");
		test("A.N", "123");
		notFound("A.M");

		define("A", "class { F() { 123 } }");
		test("A.F()", "123");
		test("A().F()", "123");

		define("A", "class { F?() { 123 } G!() { 456 } }");
		test("A.F?()", "123");
		test("A.G!()", "456");

		define("A", "class { F() { .G() } G() { 123 } }");
		test("A().F()", "123");

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

		define("A", "class { New(n) { .A = n } }");
		define("B", "A { }"); // New args pass through
		define("C", "B { New(n) { super(123) } }");
		test("C(123).A", "123");

		define("A", "class { F() { 123 } }");
		define("B", "A { F() { 456 } G() { super.F() } }");
		test("B.G()", "123");
		test("B().G()", "123");

		// super call in a block
		define("A", "class { F() { 123 } }");
		define("B", "A { F() { 456 } G() { b = { super.F() }; b() } }");
		test("B.G()", "123");
		test("B().G()", "123");

		define("A", "class { B: class { F() { 123 } } }");
		test("(new A.B).F()", "123");
		test("new A.B", "A.B()");

		define("A", "class { F() { 123 } N: 123 }");
		define("B", "A { }");
		test("A.F", "A.F");
		test("B.F", "A.F");
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

		define("C", "class { X: function () { 123 } }");
		test("C.X()", "123");

		define("C", "#( X: (function () {}) )");
		test("Type(C.X[0])", "'Function'");

		define("C", "class { X: (function () {}) }");
		test("Type(C.X[0])", "'Function'");

		define("X", "#(function () { 123 })");
		test("(X[0])()", "123");

		define("X", "#(func: function () { 123 })");
		test("(X.func)()", "123");

		define("A", "class { CallClass() { new this() } }");
		test("A()", "A()");

		define("A", "class { New() { .a = 123; } }");
		test("A().Members()", "#('A_a')");

		define("A", "class { New() { this.a = 123; } }");
		test("A().Members()", "#('a')");

		define("A", "class { New() { this['a'] = 123; } }");
		test("A().Members()", "#('a')");

		// method should be bound to starting point, not where found
		define("A", "class { F() { .G() } }");
		define("B", "class : A { G() { 123 } }");
		test("f = B.F; f()", "123");
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

		define("C", "class { CallClass() { this } }");
		test("#(1).Eval(C)", "#(1)");

		test("C.Eval(F)", "C");
		test("(new C).Eval(F)", "C()");

		define("B", "C { }");
		test("B.Eval(F)", "B");
	}

	@Test
	public void test_privatize() {
		define("C", "class { F() { .p() }; Default(@a) { a } }");
		test("C.F()", "#('C_p')");
	}

	@Test
	public void test_super() { // super must be first
		try {
			compile("A", "class { New() { F(); super() } }");
			fail("call to super must come first");
		} catch (Exception e) {
			assertEquals("call to super must come first", e.toString());
		}
	}

	@Test
	public void test_super2() { // super must be in New
		try {
			compile("A", "class { F() { super() } }");
			fail("should only allow super(...) in New");
		} catch (Exception e) {
			assertEquals("super call only allowed in New", e.toString());
		}
	}

	@Test
	public void test_super_Default() {
		define("A", "class { }");
		define("B", "class : A { Default(@args) { super.Default(@args) } }");
		try {
			test("B.Fn()", "");
			fail();
		} catch (Exception e) {
			assert(e.toString().contains("method not found"));
		}
	}

	@Test
	public void test_getdefault() {
		define("C", "class { X: 123 }");
		test("C.GetDefault('X', 456)", "123");
		test("C.GetDefault('Y', 456)", "456");
		test("C.GetDefault('Y', { 456 })", "456");
		test("C.GetDefault('Y', function () { 456 })", "eval$f");
		test("x = C(); x.GetDefault('X', 456)", "123");
		test("x = C(); x.GetDefault('Y', 456)", "456");
		test("x = C(); x.GetDefault('Y', { 456 })", "456");
		test("x = C(); x.GetDefault('Y', { x; 456 })", "456"); // closure
		test("x = C(); x.GetDefault('Y', function () { 456 })", "eval$f");
		test("x = C(); x.X = 999; x.GetDefault('X', 456)", "999");
	}

	private static void notFound(String expr) {
		try {
			eval(expr);
			fail();
		} catch (Exception e) {
			assert e.toString().startsWith("member not found");
		}
	}

	void define(String name, String definition) {
		Suneido.context.set(name, compile(name, definition));
	}
}
