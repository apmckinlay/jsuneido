package suneido.language;

import org.junit.Test;
import static org.junit.Assert.*;

import suneido.SuException;
import suneido.language.jsdi.DllInterface;

/**
 * Tests parsing compiling of Suneido language {@code struct} elements.
 * @author Victor Schappert
 * @since 20130621
 * @see suneido.language.jsdi.type.StructureTest
 */
@DllInterface
public class ParseAndCompileStructTest {

	//
	// CONSTANTS
	//

	public final String EVERYTHING =
		"struct\n" +
			"\t{\n" +
			"\tbool a\n" +
			"\tbool * pa\n" +
			"\tbool[2] aa\n" +
			"\tchar b\n" +
			"\tchar * pb\n" +
			"\tchar[2] ab\n" +
			"\tshort c\n" +
			"\tshort * pc\n" +
			"\tshort[2] ac\n" +
			"\tlong d\n" +
			"\tlong * pd\n" +
			"\tlong[2] ad\n" +
			"\tint64 e\n" +
			"\tint64 * pe\n" +
			"\tint64[2] ae\n" +
		"\t}";


	//
	// PARSING TESTS
	//

	private static final AstNode EMPTY_LIST = new AstNode(Token.LIST);
	private static AstNode parse(CharSequence code) {
		Lexer lexer = new Lexer(code.toString());
		AstGenerator generator = new AstGenerator();
		ParseStruct<AstNode, Generator<AstNode>> ps =
				new ParseStruct<AstNode, Generator<AstNode>>(lexer, generator);
		return ps.parse();
	}

	@Test
	public void parseEmptyStruct() {
		AstNode node = parse("struct { }");
		assertEquals(node, new AstNode(Token.STRUCT, EMPTY_LIST));
	}

	@Test
	public void parseSimpleType() {
		parse("struct { long a }");
	}

	@Test
	public void parsePointerType() {
		parse("struct { long * a }");
	}

	@Test
	public void parseArrayType() {
		parse("struct { long[100]a;}");
	}

	@Test
	public void parseAllStorageTypes() {
		parse(
			"struct\n" +
			"{\n" +
				"\tTypeA *  ptrA;\n" +
				"\tTypeB[1] arrB\n" +
				"\tTypeC    simC; char ch;\n" +
			"}"
		);
	}

	@Test
	public void parseAllSupportedTypes() {
		parse(EVERYTHING);
	}

	@Test(expected=SuException.class)
	public void parseTooManyMembers() {
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
	public void parseSyntaxErrors() {
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
	public void parseArrayPointer() {
		// In the future, we may want to support this syntax.
		parse("struct { long[] * ptrToArr }");
	}

	//
	// COMPILING TESTS
	//

	private static Object compile(CharSequence code) {
		return Compiler.compile(
				ParseAndCompileStructTest.class.getSimpleName(),
				code.toString());
	}

	@Test(expected=SuException.class)
	public void compileDuplicateMemberName() {
		compile("struct { long a; short a }");
	}

	@Test
	public void compileAllSupportedTypes() {
		compile(EVERYTHING);
	}

	@Test(expected=SuException.class)
	public void compileZeroSizeArray() {
		compile("struct { long[0] arr }");
	}
}
