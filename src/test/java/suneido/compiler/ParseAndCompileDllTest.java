/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import suneido.SuException;
import suneido.compiler.AstGenerator;
import suneido.compiler.AstNode;
import suneido.compiler.Compiler;
import suneido.compiler.Generator;
import suneido.compiler.Lexer;
import suneido.compiler.ParseDll;
import suneido.compiler.Token;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.JSDIException;
import suneido.jsdi.type.BasicType;
import suneido.jsdi.type.StringType;
import suneido.util.testing.Assumption;

/**
 * Tests parsing and compiling of Suneido language <code>dll</code> elements.
 * @author Victor Schappert
 * @since 20130705
 * @see ParseAndCompileStructTest
 * @see ParseAndCompileCallbackTest
 */
@DllInterface
@RunWith(Parameterized.class)
public class ParseAndCompileDllTest {

	@BeforeClass
	public static void setupBeforeClass() {
		Assumption.jsdiIsAvailable(); // Prevent failure on Mac OS, Linux, etc.
	}

	@Parameters
	public static Collection<Object[]> isFast() {
		return Arrays.asList(new Object[][] { { Boolean.FALSE }, { Boolean.TRUE } }); 
	}

	public ParseAndCompileDllTest(boolean isFast) {
		JSDI.getInstance().setFastMode(isFast);
	}

	public final String EVERYTHING =
			"dll        void jsdi:TestVoid\n" +
				"\t(\n" +
				"\tbool a,\n" +
				"\tbool[2] aa,\n" +
				"\tint8 b,\n" +
				"\tint8[2] ab,\n" +
				"\tint16 c,\n" +
				"\tint16[2] ac,\n" +
				"\tint32 d,\n" +
				"\tint32[2] ad,\n" +
				"\tint64 e,\n" +
				"\tint64[2] ae,\n" +
				"\tpointer f,\n" +
				"\tpointer[2] af,\n" +
				"\tfloat g,\n" +
				"\tfloat[2] ag,\n" +
				"\tdouble h,\n" +
				"\tdouble [2] ah,\n" +
				"\thandle i,\n" +
				"\thandle [2] ai,\n" +
				"\tgdiobj j,\n" +
				"\tgdiobj [2] aj,\n" +
				"\tstring k,\n" +
				"\tstring [2] ak,\n" +
				"\tbuffer l,\n" +
				"\tbuffer [2] al,\n" +
				"\t[in] string m,\n" +
				"\tresource n\n" +
			"\t)";
			// TODO: add callback

	public final String[] VALID_RETURN_TYPES = { "void", "bool", "int8",
			"int16", "int32", "int64", "pointer", "float", "double", "handle",
			"gdiobj", "string" };

	public final String[] INVALID_RETURN_TYPES = {
		"resource", "buffer", "FakeTypeABC1"
	};

	public final String[] INVALID_RETURN_TYPES_SYNTAX = {
		"void *", "void[10]", "int32 *", "int32[1]",
		"buffer[2]", "buffer *", "FakeTypeABC1[23]", "FakeTypeABC1*"
	};

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
		parse("dll x y:z(a a_ b b_)"); // weirdly, allowed in cSuneido
	}

	@Test(expected=SuException.class)
	public void parseBadSecondParam() {
		parse("dll long x:y(a a, +b)");
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

	@Test
	public void parseInString() {
		parse("dll a b:c( [in] string param)");
		parse("dll a b:c(x * y, [in] string z)");
	}

	@Test
	public void parseSyntaxErrors() {
		String bad[] = {
			"dll void a()",
			"dll void a:b(",
			"dll a:b()",
			"dll x y:z(a)",
			"dll x y:z(a, b, c)",
			"dll x y:z(A a , B b , )",
			"dll x y:z( , )",
			"dll x y:z([in] string)",
			"dll x y:z([IN] string a)",
			"dll x y:z(long[-2] a)",
			"dll x y:z(long[1.1] a)",
			"dll x y:z(long ** a)",
		};
		for (String s : bad)
			assertThrew(() -> compile(s), SuException.class);
	}

	@Test(expected=SuException.class)
	public void parseArrayPointer() {
		// In the future, we may want to support this syntax.
		parse("dll x y:z(long[] * ptrToArr)");
	}

	//
	// COMPILING TESTS
	//

	private static Object compile(CharSequence code) {
		Assumption.jvmIsOnWindows();
		return Compiler.compile(
				ParseAndCompileStructTest.class.getSimpleName(),
				code.toString());
	}

	@Test(expected=SuException.class)
	public void compileDuplicateParameterName() {
		compile("dll long jsdi:TestVoid(bool a, long a)");
	}

	@Test
	public void compileValidReturnTypes() {
		StringBuilder sb = new StringBuilder("dll ");
		for (String returnType : VALID_RETURN_TYPES) {
			sb.delete(4, sb.length());
			sb.append(returnType);
			sb.append(" jsdi:TestVoid()");
			compile(sb.toString());
		}
	}

	@Test
	public void compileInvalidReturnTypes() {
		Assumption.jvmIsOnWindows();
		final StringBuilder sb = new StringBuilder("dll ");
		for (String returnType : INVALID_RETURN_TYPES) {
			sb.delete(4, sb.length());
			sb.append(returnType);
			sb.append(" jsdi:TestVoid()");
			assertThrew(() -> { compile(sb.toString()); },
				SuException.class, "invalid dll return type");
		}
	}

	@Test
	public void compileInvalidReturnTypes_Syntax() {
		Assumption.jvmIsOnWindows();
		final StringBuilder sb = new StringBuilder("dll ");
		for (String returnType : INVALID_RETURN_TYPES_SYNTAX) {
			sb.delete(4, sb.length());
			sb.append(returnType);
			sb.append(" jsdi:TestVoid()");
			assertThrew(() -> { compile(sb.toString()); },
				SuException.class, "syntax error");
		}
	}

	@Test(expected=SuException.class)
	public void compileStringPointer() {
		compile("dll x y:z(string * ptrToStr)");
	}

	@Test(expected=SuException.class)
	public void compileBufferPointer() {
		compile("dll x y:z(buffer * ptrToBuf)");
	}

	@Test
	public void compileInStringParam() {
		compile("dll void jsdi:TestVoid([in] string a)");
	}

	@Test
	public void compileInStringInvalidUses() {
		final ArrayList<String> typeNames = new ArrayList<>();
		for (BasicType basicType : BasicType.values()) {
			typeNames.add(basicType.getName());
			typeNames.add(basicType.getName() + "*");
			typeNames.add(basicType.getName() + "[207]");
		}
		typeNames.add(StringType.IDENTIFIER_BUFFER);
		typeNames.add(StringType.IDENTIFIER_BUFFER + "*");
		typeNames.add(StringType.IDENTIFIER_BUFFER + "[207]");
		typeNames.add(StringType.IDENTIFIER_STRING + "*");
		typeNames.add(StringType.IDENTIFIER_STRING + "[207]");
		typeNames.add("FAKEType19820207");
		typeNames.add("FAKEType19820207*");
		typeNames.add("FAKEType19820207[207]");
		for (String typeName : typeNames) {
			final String code = "dll void jsdi:TestVoid([in] " + typeName
						+ " x)";
			assertThrew(() -> compile(code));
		}
	}

	@Test
	public void compileAllSupportedTypes() {
		compile(EVERYTHING);
	}

	@Test
	public void compileErrors() {
		String bad[] = {
			"dll NotARealType jsdi:TestVoid()",
			"dll int32[4] jsdi:TestVoid()",
			"dll int8 * jsdi:TestVoid()",
			"dll buffer jsdi:TestVoid()",
			"dll [in] string jsdi:TestVoid()",
			"dll void jsdi:TestVoid(string * ps)",
			"dll void jsdi:TestVoid(buffer * pb)",
			"dll void jsdi:TestVoid(resource * pr)",
			"dll void jsdi:TestVoid(resource[2] ar)"
		};
		for (final String s : bad)
			assertThrew(() -> { compile(s); });
	}

	@Test
	public void compileInvalidBasicTypePointer() {
		// Pointers to basic types not allowed
		for (final BasicType b : BasicType.values()) {
			assertThrew(() -> {
				compile("dll x y:z(" + b.getName() + " * badParam)");
			}, JSDIException.class, "pointer to basic type not allowed");
		}
	}
}