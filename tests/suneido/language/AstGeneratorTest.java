package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AstGeneratorTest {

	@Test
	public void constants() {
		constant("true", "(TRUE)");
		constant("false", "(FALSE)");
		constant("123", "(NUMBER=123)");
		constant("'xyz'", "(STRING=xyz)");
		constant("#abc", "(SYMBOL=abc)");
		constant("#()", "(OBJECT)");
		constant("#{}", "(RECORD)");
		constant("#(123)", "(OBJECT (MEMBER null (NUMBER=123)))");
		constant("#{name: fred}", "(RECORD (MEMBER (STRING=name) (STRING=fred)))");
		constant("#(1, 'x', a: #y, b: true)",
				"(OBJECT " +
					"(MEMBER null (NUMBER=1)) " +
					"(MEMBER null (STRING=x)) " +
					"(MEMBER (STRING=a) (SYMBOL=y)) " +
					"(MEMBER (STRING=b) (TRUE)))");
	}

	@Test
	public void code() {
		code("123 + 456", "(BINARYOP (ADD) (NUMBER=123) (NUMBER=456))");
		code("s = 'fred'", "(EQ (IDENTIFIER=s) (STRING=fred))");
	}

	private void code(String code, String expected) {
		constant("function () { " + code + "}",
				"(FUNCTION=Test (LIST) (LIST " + expected + "))");
	}

	private void constant(String code, String expected) {
		Lexer lexer = new Lexer(code);
		AstGenerator generator = new AstGenerator("Test");
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		String actual = ast.toString().replace("\n", "").replaceAll(" +", " ");
		assertEquals(expected, actual);
	}

}
