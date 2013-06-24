package suneido.language;

import org.junit.Test;
import static org.junit.Assert.*;

import suneido.SuException;
import suneido.language.jsdi.DllInterface;

@DllInterface
public class ParseStructTest {

	private static final AstNode EMPTY_LIST = new AstNode(Token.LIST);

	@Test
	public void empty_struct() {
		AstNode node = parse("struct { }");
		assertEquals(node, new AstNode(Token.STRUCT, EMPTY_LIST));
	}

	@Test
	public void simple_type() {
		parse("struct { long a }");
	}

	@Test
	public void pointer_type() {
		parse("struct { long * a }");
	}

	@Test
	public void array_type() {
		parse("struct { long[100]a;}");
	}

	@Test
	public void all_types() {
		parse(
			"struct\n" +
			"{\n" +
				"\tTypeA *  ptrA;\n" +
				"\tTypeB[1] arrB\n" +
				"\tTypeC    simC; char ch;\n" +
			"}"
		);
	}

	@Test(expected=SuException.class)
	public void too_many_members() {
		StringBuilder code = new StringBuilder("struct { ");
		final int N = 101;
		int j = 0;
		for (int i = 0; i < 4; ++i)
		{
			for (char ch = 'a'; ch <= 'z' && j < N; ++ch, ++j)
			{
				code.append("long ");
				code.append(new String(new char[i+1]).replace('\0', ch));
				code.append(';');
			}
		}
		code.append('}');
		parse(code);
		assertFalse("Control should never pass here", true);
	}

	@Test
	public void syntax_errors() {
		String bad[] = {
			"struct { a b",
			"struct { a }",
			"struct { a *}",
			"struct { a[] b}",
			"struct { a[-1] b}",
			"struct { a[b }",
		};
		int n = 0;
		for (String s : bad)
			try
				{ parse(s); }
			catch (SuException e)
				{ ++n; }
		assertEquals(n, bad.length);
	}

	@Test(expected=SuException.class)
	public void array_pointer() {
		// In the future, we may want to support this syntax.
		parse("struct { long[] * ptrToArr }");
	}

	private static AstNode parse(CharSequence code) {
		Lexer lexer = new Lexer(code.toString());
		AstGenerator generator = new AstGenerator();
		ParseStruct<AstNode, Generator<AstNode>> ps =
				new ParseStruct<AstNode, Generator<AstNode>>(lexer, generator);
		return ps.parse();
	}
}
