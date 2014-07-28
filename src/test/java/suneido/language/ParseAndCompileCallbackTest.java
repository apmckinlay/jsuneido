/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.util.testing.Throwing.assertThrew;

import org.junit.Test;

import suneido.SuException;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.type.BasicType;
import suneido.util.testing.Assumption;

/**
 * Tests parsing and compiling of Suneido language {@code callback} elements.
 * @author Victor Schappert
 * @since 20130710
 * @see ParseAndCompileStructTest
 * @see ParseAndCompileDllTest
 */
@DllInterface
public class ParseAndCompileCallbackTest {

	public final String EVERYTHING =
			"callback\n" +
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
				"\tbuffer [2] al\n" +
			"\t)";

	//
	// PARSING TESTS
	//

	private static final AstNode EMPTY_LIST = new AstNode(Token.LIST);
	private static AstNode parse(CharSequence code) {
		Lexer lexer = new Lexer(code.toString());
		AstGenerator generator = new AstGenerator();
		ParseCallback<AstNode, Generator<AstNode>> ps =
				new ParseCallback<AstNode, Generator<AstNode>>(lexer, generator);
		return ps.parse();
	}

	@Test
	public void parseEmptyCallback() {
		AstNode node = parse("callback()");
		assertEquals(node, new AstNode(Token.CALLBACK, EMPTY_LIST));
	}

	@Test
	public void parseValueType() {
		parse("callback ( long a )");
	}

	@Test
	public void parsePointerType() {
		parse("callback(long*a)");
	}

	@Test
	public void parseArrayType() {
		parse("callback ( long[100]a )");
	}

	@Test
	public void parseAllStorageTypes() {
		parse(
			"callback\n" +
			"(\n" +
				"\tTypeA *  ptrA,\n" +
				"\tTypeB[1] arrB,\n" +
				"\tTypeC    simC, char ch\n" +
			")"
		);
	}

	@Test
	public void parseMultiNewline() {
		parse(
			"callback\n" +
			"    (\n\n\n" +
				"    \tTypeA a\n\n," +
				"    \tTypeB b\n\n\n," +
				"    \tTypeC c)"
		);
	}

	@Test
	public void parseAllSupportedTypes() {
		parse(EVERYTHING);
	}

	@Test(expected=SuException.class)
	public void parseTooManyParameters() {
		StringBuilder code = new StringBuilder("callback ( ");
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
		code.append(')');
		parse(code);
		assertFalse("Control should never pass here", true);
	}

	@Test
	public void parseSyntaxErrors() {
		String bad[] = {
			"callback ( a b",
			"callback ( a )",
			"callback ( a *)",
			"callback ( a[] b)",
			"callback ( a[-1] b)",
			"callback ( a[b )",
			"callback ( A a B b )",
			"callback ( a[1.1] )",
			"callback ( a a , &= b )",
			"callback ( A a , B b , )",
			"callback (a, b)",
			"callback ( , )",
			"callback ( [in] string a )" /* consistent with CSuneido */,
			"callback ( string a, [in] string b )" /* consistent with CSuneido */,
			"callback ( [in] buffer a )",
			"callback ( [in] char a )",
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
		parse("callback  (  long [] * ptrToArr )  ");
	}

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
		compile("callback(bool a, long a)");
	}

	@Test(expected=SuException.class)
	public void compileBuffer() {
		// A dll function can't send a buffer to a Suneido callback because it
		// has no protocol by which to tell Suneido how big the buffer is and
		// consequently the buffer can't be unmarshalled. This should be caught
		// at the compile stage for direct parameters and at the plan generation
		// stage for indirect ones (those wrapped in a struct).
		compile("callback(buffer b)");
	}

	@Test(expected=SuException.class)
	public void compileResource() {
		// This is not currently supported, but unlike with buffer, it's not a
		// technical limitation. It could easily be implemented.
		compile("callback(resource r)");
	}

	@Test
	public void compileAllSupportedTypes() {
		compile(EVERYTHING);
	}

	@Test
	public void compileErrors() {
		String bad[] = {
			"callback(string * ptrToStr)",
			"callback(buffer * ptrToBuf)",
			"callback(resource * ptrToRes)",
			"callback(resource[2] resArr)"
		};
		int n = 0;
		for (String s : bad)
			try
				{ compile(s); }
			catch (SuException e)
				{ ++n; }
		assertEquals(n, bad.length);
	}

	@Test
	public void compileInvalidBasicTypePointer() {
		// Pointers to basic types not allowed
		for (final BasicType b : BasicType.values()) {
			assertThrew(() -> {
				compile("callback(" + b.getName() + " * badParam)");
			}, JSDIException.class, "pointer to basic type not allowed");
		}
	}
}
