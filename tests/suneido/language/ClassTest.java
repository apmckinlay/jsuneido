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

		defineClass("A", "class { F() { .g() } g() { 123 } }");
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
		test("B.F", "A.F");
		test("B.N", "123");
		notFound("B.M");
	}

	@Test
	public void test_getter() {
		defineClass("A", "class { Get_N() { 'getter' } }");
		test("A.N", "'getter'");
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
