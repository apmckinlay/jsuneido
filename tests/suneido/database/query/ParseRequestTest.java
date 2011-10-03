package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.language.Lexer;


public class ParseRequestTest {

	@Test
	public void test_requests() {
		test("drop mytable", "drop(mytable)");
		test("destroy mytable", "drop(mytable)");
		test("drop 'mytable'", "drop(mytable)");
		test("Drop mytable", "drop(mytable)");
		test("rename one to two", "rename(one, two)");
		test("RENAME one TO two", "rename(one, two)");
		test("view myview = one join two", "view(myview, 'one join two')");
		test("sview myview = three", "sview(myview, 'three')");
		test("create a (b,c,d) key(b) index(c,d)");
		test("ensure a (b,c,d) key(b) index(c,d)");
		test("ensure a key(b)");
		test("ensure a key(b) in c");
		test("ensure a index(b) in c (d)");
		test("ensure a index(b) in c cascade");
		test("ensure a key(b) in c cascade update");
		test("alter a create (b)");
		test("alter a drop key(b)");
		test("alter a rename b to c");
		test("alter a rename b to c, d to e");
	}

	private void test(String request) {
		test(request, request);
	}
	void test(String request, String result) {
		assertEquals(result, parse(request));
	}

	@Test(expected = RuntimeException.class)
	public void bad() {
		parse("create a key(b)"); // no columns
	}

	private static String parse(String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		StringRequestGenerator generator = new StringRequestGenerator();
		ParseRequest<String> pc = new ParseRequest<String>(lexer, generator);
		return pc.parse();
	}

}
