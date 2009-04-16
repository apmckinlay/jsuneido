package suneido.language;

import static suneido.language.ExecuteTest.test;

import org.junit.Test;


public class ClassTest {
	@Test
	public void test1() {
		defineClass("A", "class { }");
		test("new A", "A()");

		defineClass("A", "class { F() { .g() } g() { 123 } }");
		test("A().F()", "123");

		defineClass("A", "class { New(x) { .X = x } }");
		test("A(456).X", "456");

		defineClass("A", "class { Default(method) { 'missing: ' $ method } }");
		test("A().F()", "'missing: F'");

		defineClass("A", "class { F(n) { n * 2 } }");
		defineClass("B", "A { }");
		test("B().F(123)", "246");

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
