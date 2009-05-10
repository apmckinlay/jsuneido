package suneido.language;

import static org.junit.Assert.fail;
import static suneido.language.ExecuteTest.eval;
import static suneido.language.ExecuteTest.test;

import org.junit.Test;

import suneido.SuException;


public class ClassTest {
	@Test
	public void test1() {
		defineClass("A", "class { }");
		test("new A", "A()");

		defineClass("A", "class { N: 123 }");
		test("A.N", "123");
		notFound("A.M");

		defineClass("A", "class { F() { 123 } }");
		test("A().F()", "123");

		defineClass("A", "class { F?() { 123 } G!() { 456 } }");
		test("A.F?()", "123");
		test("A.G!()", "456");

		defineClass("A", "class { F() { .g() } g() { 123 } }");
		test("A().F()", "123");

		defineClass("A", "class { F() { .x } x: 123 }");
		test("A().F()", "123");

		defineClass("A", "class { New(x) { .X = x } }");
		test("A(456).X", "456");

		defineClass("A", "class { Default(method) { 'missing: ' $ method } }");
		test("A().F()", "'missing: F'");

		defineClass("A", "class { F(n) { n * 2 } }");
		defineClass("B", "A { }");
		test("B().F(123)", "246");

		defineClass("A", "class { G() { .x = 456 } }");
		defineClass("B", "A { F() { .x = 123; .G(); .x } }");
		test("B().F()", "123");

		defineClass("A", "class { G() { .X = 123 } }");
		defineClass("B", "A { F() { .G(); .X } }");
		test("B().F()", "123");

		defineClass("A", "class { Call() { 123 } CallClass() { 456 } }");
		test("A()", "456");
		test("a = new A; a()", "123");

		defineClass("A", "class { N: 123 F() { .N } }");
		test("A.N", "123");
		test("A.F()", "123");
		test("A().N", "123");
		test("A().F()", "123");
		defineClass("B", "A { G() { .N } }");
		test("B.N", "123");
		test("B.F()", "123");
		test("B.G()", "123");
		test("B().N", "123");
		test("B().F()", "123");
		test("B().G()", "123");

		defineClass("A", "class { New() { .A = 123 } }");
		defineClass("B", "A { New() { .B = 456 } }");
		test("b = B(); b.A + b.B", "579");

		defineClass("A", "class { New(n) { .A = n } }");
		defineClass("B", "A { New() { super(123) } }");
		test("B().A", "123");

		defineClass("A", "class { F() { 123 } }");
		defineClass("B", "A { F() { 456 } G() { super.F() } }");
		test("B().G()", "123");

		defineClass("A", "class { B: class { F() { 123 } } }");
		test("(new A.B).F()", "123");
		test("new A.B", "A_c0()");

		defineClass("A", "class { F() { 123 } N: 123 }");
		defineClass("B", "A { }");
		test("A.F", "A.F");
		test("B.F", "B.F");
		test("B.N", "123");
		notFound("B.M");

		defineClass("A", "class { New(args) { super(@args) } }");

		defineClass("A", "class { ToString() { 'an A' } }");
		test("A()", "an A");
		defineClass("A", "class { New(n) { .n = n } ToString() { 'A' $ .n } }");
		test("A(123)", "A123");
	}
	@Test public void test_static_getter() {
		defineClass("A", "class { " +
				"Get_N() { 'getter' }" +
				"Get_(m) { 'get ' $ m }" +
				" }");
		test("A.N", "'getter'");
		test("A.X", "'get X'");
		defineClass("B", "A { }");
		test("B.N", "'getter'");
		test("B.X", "'get X'");
	}
	@Test public void test_instance_getter() {
		defineClass("A", "class { "
				+ "New(x) { .X = x } "
				+ "Get_N() { .X $ ' getter' } "
				+ "Get_(m) { .X $ ' get ' $ m } "
				+ "}");
		test("A(1).N", "'1 getter'");
		test("A(1).Z", "'1 get Z'");
		defineClass("B", "A { }");
		test("B(2).N", "'2 getter'");
		test("B(2).Z", "'2 get Z'");
	}
	@Test public void test_private_instance_getter() {
		defineClass("A", "class { "
				+ "New(x) { .x = x } "
				+ "get_n() { .x $ ' getter' } "
				+ "N() { .n }"
				+ "Z() { .z }"
				+ "}");
		test("A(1).N()", "'1 getter'");
	}

	private static void notFound(String expr) {
		try {
			eval(expr);
			fail();
		} catch (SuException e) {
			assert e.toString().startsWith("member not found");
		}
	}

	void defineClass(String name, String definition) {
		Lexer lexer = new Lexer(definition);
		CompileGenerator generator = new CompileGenerator(name);
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		Object x = pc.parse();
		Globals.put(name, x);
	}
}
