/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.Arrays;
import java.util.Collection;

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
import suneido.compiler.ParseStruct;
import suneido.compiler.Token;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.JSDIException;
import suneido.jsdi.marshall.PrimitiveSize;
import suneido.util.testing.Assumption;

/**
 * Tests parsing and compiling of Suneido language {@code struct} elements.
 *
 * @author Victor Schappert
 * @since 20130621
 * @see suneido.jsdi.type.StructureTest
 * @see ParseAndCompileDllTest
 * @see ParseAndCompileCallbackTest
 */
@DllInterface
@RunWith(Parameterized.class)
public class ParseAndCompileStructTest {

	@Parameters
	public static Collection<Object[]> isFast() {
		return Arrays.asList(new Object[][] { { Boolean.FALSE }, { Boolean.TRUE } }); 
	}

	public ParseAndCompileStructTest(boolean isFast) {
		JSDI.getInstance().setFastMode(isFast);
	}

	//
	// CONSTANTS
	//

	public final String EVERYTHING =
		"struct\n" +
			"\t{\n" +
			"\tbool a\n" +
			"\tbool[2] aa\n" +
			"\tint8 b\n" +
			"\tint8[2] ab\n" +
			"\tint16 c\n" +
			"\tint16[2] ac\n" +
			"\tint32 d\n" +
			"\tint32[2] ad\n" +
			"\tint64 e\n" +
			"\tint64[2] ae\n" +
			"\tpointer f\n" +
			"\tpointer[2] af\n" +
			"\tfloat g\n" +
			"\tfloat[2] ag\n" +
			"\tdouble h\n" +
			"\tdouble [2] ah\n" +
			"\thandle i\n" +
			"\thandle [2] ai\n" +
			"\tgdiobj j\n" +
			"\tgdiobj [2] aj\n" +
			"\tstring k\n" +
			"\tstring [2] ak\n" +
			"\tbuffer l\n" +
			"\tbuffer [2] al\n" +
			"\tresource m\n" +
			"\tcallback n\n" +
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
			assertThrew(() -> { parse(s); },
					SuException.class, "syntax error");
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
		Assumption.jvmIsOnWindows();
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
	public void compileStructWhoseSizeNotDivisibleByWordSize() {
		assertTrue(PrimitiveSize.INT8 < PrimitiveSize.WORD);
		compile("struct { char x }");
	}

	@Test
	public void compileMisalignedWord() {
		assertTrue(PrimitiveSize.INT8 < PrimitiveSize.WORD);
		compile("struct { char x ; long y }");
	}

	@Test
	public void compileErrors() {
		Assumption.jvmIsOnWindows();
		String bad[] = {
			"struct { string * ps }",
			"struct { buffer * pb }",
			"struct { resource[2] ar }",
			"struct { resource * pr }"
		};
		for (final String s : bad)
			assertThrew(() -> {
				compile(s);
			}, JSDIException.class);
	}
}
