/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.ArrayList;

import org.junit.Test;

import suneido.SuException;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.type.BasicType;
import suneido.language.jsdi.type.StringType;
import suneido.util.testing.Assumption;

/**
 * Tests parsing and compiling of Suneido language <code>dll</code> elements.
 * @author Victor Schappert
 * @since 20130705
 * @see ParseAndCompileStructTest
 * @see ParseAndCompileCallbackTest
 */
@DllInterface
public class ParseAndCompileDllTest {

	public final String EVERYTHING =
			"dll        void jsdi:_TestVoid@0\n" +
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
				"\tdouble [2] ag,\n" +
				"\thandle h,\n" +
				"\thandle * ph,\n" +
				"\thandle [2] ah,\n" +
				"\tgdiobj i,\n" +
				"\tgdiobj * pi,\n" +
				"\tgdiobj [2] ai,\n" +
				"\tstring j,\n" +
				"\tstring [2] aj,\n" +
				"\tbuffer k,\n" +
				"\tbuffer [2] ak,\n" +
				"\t[in] string l,\n" +
				"\tresource m\n" +
			"\t)";
			// TODO: add callback

	public final String[] VALID_RETURN_TYPES = {
		"void", "bool", "char", "short", "long", "int64", "float", "double",
		"handle", "gdiobj", "string"
	};

	public final String[] INVALID_RETURN_TYPES = {
		"resource", "buffer", "FakeTypeABC1"
	};

	public final String[] INVALID_RETURN_TYPES_SYNTAX = {
		"void *", "void[10]", "long *", "long[1]",
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
		parse("dll x y:z(a a_ b b_)"); // weirdly, allowed in CSuneido
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
		int n = 0;
		for (String s : bad)
			try
				{ parse(s); }
			catch (SuException e)
				{ ++n; }
		assertEquals(bad.length, n);
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
		Assumption.jvmIs32BitOnWindows();
		return Compiler.compile(
				ParseAndCompileStructTest.class.getSimpleName(),
				code.toString());
	}

	@Test(expected=SuException.class)
	public void compileDuplicateParameterName() {
		compile("dll long jsdi:_TestVoid@0(bool a, long a)");
	}

	@Test
	public void compileValidReturnTypes() {
		StringBuilder sb = new StringBuilder("dll ");
		for (String returnType : VALID_RETURN_TYPES) {
			sb.delete(4, sb.length());
			sb.append(returnType);
			sb.append(" jsdi:_TestVoid@0()");
			compile(sb.toString());
		}
	}

	@Test
	public void compileInvalidReturnTypes() {
		Assumption.jvmIs32BitOnWindows();
		final StringBuilder sb = new StringBuilder("dll ");
		for (String returnType : INVALID_RETURN_TYPES) {
			sb.delete(4, sb.length());
			sb.append(returnType);
			sb.append(" jsdi:_TestVoid@0()");
			assertThrew(
				new Runnable() { @Override
				public void run() { compile(sb.toString()); } },
				SuException.class, "invalid dll return type"
			);
		}
	}

	@Test
	public void compileInvalidReturnTypes_Syntax() {
		Assumption.jvmIs32BitOnWindows();
		final StringBuilder sb = new StringBuilder("dll ");
		for (String returnType : INVALID_RETURN_TYPES_SYNTAX) {
			sb.delete(4, sb.length());
			sb.append(returnType);
			sb.append(" jsdi:_TestVoid@0()");
			assertThrew(
				new Runnable() { @Override
				public void run() { compile(sb.toString()); } },
				SuException.class, "syntax error"
			);
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
		compile("dll void jsdi:_TestVoid@0([in] string a)");
	}

	@Test
	public void compileInStringInvalidUses() {
		final ArrayList<String> typeNames = new ArrayList<>();
		for (BasicType basicType : BasicType.values()) {
			typeNames.add(basicType.toIdentifier());
			typeNames.add(basicType.toIdentifier() + "*");
			typeNames.add(basicType.toIdentifier() + "[207]");
		}
		typeNames.add(StringType.IDENTIFIER_BUFFER);
		typeNames.add(StringType.IDENTIFIER_BUFFER + "*");
		typeNames.add(StringType.IDENTIFIER_BUFFER + "[207]");
		typeNames.add(StringType.IDENTIFIER_STRING + "*");
		typeNames.add(StringType.IDENTIFIER_STRING + "[207]");
		typeNames.add("FAKEType19820207");
		typeNames.add("FAKEType19820207*");
		typeNames.add("FAKEType19820207[207]");
		int N = 0;
		for (String typeName : typeNames) {
			try {
				final String code = "dll void jsdi:_TestVoid@0([in] " + typeName
						+ " x)";
				compile(code);
			} catch (SuException e) {
				++N;
			}
		}
		assertEquals(typeNames.size(), N);
	}

	@Test
	public void compileAllSupportedTypes() {
		compile(EVERYTHING);
	}

	@Test
	public void compileErrors() {
		String bad[] = {
			"dll NotARealType jsdi:_TestVoid@0()",
			"dll long[4] jsdi:_TestVoid@0()",
			"dll char * jsdi:_TestVoid@0()",
			"dll buffer jsdi:_TestVoid@0()",
			"dll [in] string jsdi:_TestVoid@0()",
			"dll void jsdi:_TestVoid@0(string * ps)",
			"dll void jsdi:_TestVoid@0(buffer * pb)",
			"dll void jsdi:_TestVoid@0(resource * pr)",
			"dll void jsdi:_TestVoid@0(resource[2] ar)"
		};
		for (final String s : bad)
			assertThrew(new Runnable() {
				@Override
				public void run() {
					compile(s);
				}
			});
	}
}
