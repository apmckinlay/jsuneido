package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import suneido.SuException;
import suneido.language.jsdi.DllInterface;

/**
 * Tests parsing and compiling of Suneido language <code>dll</code> elements.
 * @author Victor Schappert
 * @since 20130705
 * @see ParseAndCompileStructTest
 */
@DllInterface
public class ParseAndCompileDllTest {

	public final String EVERYTHING =
			"dll        void L:F@1\n" +
				"\t(\n" +
				"\tbool a,\n" +
				"\tbool * pa,\n" +
				"\tbool[2] aa,\n" +
				"\tchar b,\n" +
				"\tchar * pb,\n" +
				"\tchar[2] ab,\n" +
				"\tshort c,\n" +
				"\tshort * pc,\n" +
				"\tshort[2] ac,\n" +
				"\tlong d     ,\n" +
				"\tlong * pd,\n" +
				"\tlong[2] ad,\n" +
				"\tint64 e,\n" +
				"\tint64 * pe,\n" +
				"\tint64[2] ae,\n" +
				"\tfloat f,\n" +
				"\tfloat * pf,\n" +
				"\tfloat[2] af,\n" +
				"\tdouble g,\n" +
				"\tdouble * pg,\n" +
				"\tdouble [2] ag\n" +
			"\t)";

	//
	// PARSING TESTS
	//

	private static final AstNode EMPTY_LIST = new AstNode(Token.LIST);
	private static AstNode parse(CharSequence code) {
		Lexer lexer = new Lexer(code.toString());
		AstGenerator generator = new AstGenerator();
		ParseDll<AstNode, Generator<AstNode>> pd = new ParseDll<AstNode, Generator<AstNode>>(
				lexer, generator);
		return pd.parse();
	}

	@Test
	public void parseNoParams() {
		AstNode node = parse("dll x y:z()");
		assertEquals(node, new AstNode(Token.DLL, new AstNode(Token.IDENTIFIER,
				"y"), new AstNode(Token.STRING, "z"), new AstNode(
				Token.IDENTIFIER, "x"), EMPTY_LIST));
	}

	@Test
	public void parseOneValueParam() {
		parse("dll x y:z(a a_)");
	}

	@Test
	public void parseOnePointerParam() {
		parse("dll x y:z(a * pa)");
	}

	@Test
	public void parseOneArrayParam() {
		parse("dll x y:z(a[12345] arr)");
	}

	@Test
	public void parseAllStorageTypeParams() {
		parse("dll x y:z(a a_, a*pa, a [999] arr)");
	}

	@Test(expected=SuException.class)
	public void parseNoCommaParams() {
		parse("dll x y:z(a a_ b b_)"); // weirdly, allowed in CSuneido
	}

	@Test(expected=SuException.class)
	public void parseTooManyParams() {
		StringBuilder code = new StringBuilder("dll ReturnType Library : FuncName ( ");
		final int N = 101;
		int j = 0;
		for (int i = 0; i < 4; ++i)
		{
			for (char ch = 'a'; ch <= 'z' && j < N; ++ch, ++j)
			{
				code.append("long ");
				code.append(new String(new char[i+1]).replace('\0', ch));
				code.append(',');
			}
		}
		code.deleteCharAt(code.length() - 1); // delete trailing comma
		code.append(')');
		parse(code);
		assertFalse("Control should never pass here", true);
	}

	@Test
	public void parseAtSignFuncName() {
		parse("dll x y:z@1234567890( )");
	}

	@Test(expected=SuException.class)
	public void parseAtSignFuncNameBad_NothingFollowing() {
		parse("dll x y:z@()");
	}

	@Test(expected=SuException.class)
	public void parseAtSignFuncNameBad_Decimal() {
		parse("dll x y:z@123.456()");
	}

	@Test
	public void parseMultiNewline() {
		parse(
			"dll\n" +
			"R\n\n\n" +
			"  Library:FunctionName\n\n\n" +
			"  (" +
				"\tTypeA a\n,\n" +
				"\tTypeB b\n\n,\n" +
				"\tTypeC c)"
		);
	}

	@Test
	public void parseAllSupportedTypes() {
		parse(EVERYTHING);
	}

	//
	// COMPILING TESTS
	//

	

	// TODO:compiling tests
}
