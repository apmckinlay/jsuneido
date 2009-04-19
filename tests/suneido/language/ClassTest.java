package suneido.language;

import static suneido.language.ExecuteTest.test;

import org.junit.Test;


public class ClassTest {
	@Test
	public void test1() {
		defineClass("A", "class { }");
		test("new A", "A()");

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
