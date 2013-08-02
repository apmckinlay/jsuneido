package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.util.testing.Throwing.assertThrew;

import org.junit.Test;

import suneido.SuException;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.util.testing.Assumption;

/**
 * Tests parsing and compiling of Suneido language {@code struct} elements.
 * @author Victor Schappert
 * @since 20130621
 * @see suneido.language.jsdi.type.StructureTest
 * @see ParseAndCompileDllTest
 * @see ParseAndCompileCallbackTest
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
			"\tfloat f\n" +
			"\tfloat * pf\n" +
			"\tfloat[2] af\n" +
			"\tdouble g\n" +
			"\tdouble * pg\n" +
			"\tdouble [2] ag\n" +
			"\thandle h\n" +
			"\thandle * ph\n" +
			"\thandle [2] ah\n" +
			"\tgdiobj i\n" +
			"\tgdiobj * pi\n" +
			"\tgdiobj [2] ai\n" +
			"\tstring j\n" +
			"\tstring [2] aj\n" +
			"\tbuffer k\n" +
			"\tbuffer [2] ak\n" +
			"\tresource l\n" +
		"\t}";
		// TODO: add callback

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
	public void parseValueType() {
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
	public void parseMultiNewline() {
		parse(
			"struct\n" +
			"{\n\n\n" +
				"\tTypeA a\n\n" +
				"\tTypeB b\n\n\n" +
				"\tTypeC c}"
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
			"struct { A a B b }",
			"struct { a[1.1] }",
			"struct { a a; &= b }",
			"struct { [in] string a }" /* consistent with CSuneido */,
			"struct { string a; [in] string b }" /* consistent with CSuneido */,
			"string { [in] buffer a }",
			"string { [in] char a }",
		};
		for (final String s : bad)
			assertThrew(
				new Runnable() { public void run() { parse(s); } },
				SuException.class, "syntax error"
			);
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
		Assumption.jvmIs32BitOnWindows();
		return Compiler.compile(
				ParseAndCompileStructTest.class.getSimpleName(),
				code.toString());
	}

	@Test(expected=SuException.class)
	public void compileDuplicateMemberName() {
		compile("struct { long a; short a }");
	}

	@Test(expected=SuException.class)
	public void compileStringPointer() {
		compile("struct { string * ptrToStr }");
	}

	@Test(expected=SuException.class)
	public void compileBufferPointer() {
		compile("struct { buffer * ptrToBuf }");
	}

	@Test
	public void compileAllSupportedTypes() {
		compile(EVERYTHING);
	}

	@Test(expected=SuException.class)
	public void compileZeroSizeArray() {
		compile("struct { long[0] arr }");
	}

	@Test
	public void compileErrors() {
		Assumption.jvmIs32BitOnWindows();
		String bad[] = {
			"struct { string * ps }",
			"struct { buffer * pb }",
			"struct { resource[2] ar }",
			"struct { resource * pr }"
		};
		for (final String s : bad)
			assertThrew(
				new Runnable() { public void run() { compile(s); } },
				JSDIException.class
			);
	}
}
