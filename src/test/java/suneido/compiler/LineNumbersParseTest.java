/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suneido.compiler.AstGenerator;
import suneido.compiler.AstNode;
import suneido.compiler.Lexer;
import suneido.compiler.ParseConstant;
import suneido.jsdi.DllInterface;
import suneido.util.testing.Assumption;

/**
 * Test to ensure the abstract syntax tree nodes are tagged with the correct
 * line numbers where needed.
 *
 * @author Victor Schappert
 * @since 20140827
 * @see LineNumbersCompileTest
 */
public class LineNumbersParseTest {

	@Test
	public void testBasicCases() {
		runTests(BASIC_TEST_CASES);
	}

	@Test
	@DllInterface
	public void testJSDICases() {
		Assumption.jsdiIsAvailable();
		runTests(JSDI_TEST_CASES);
	}

	//
	// TEST CASES
	//

	private static final String[] BASIC_TEST_CASES = new String[] {
		"10",
			"NUMBER=10@1",
		"false",
			"FALSE@1",
		"'string'",
			"STRING=string@1",
		"#20140827.123456",
			"DATE=20140827.123456@1",
		"#()",
			"OBJECT@1",
		"#(a\nb)",
			"OBJECT@1\n\tMEMBER@1\n\t\tSTRING=a@1\n\tMEMBER@2\n\t\tSTRING=b@2",

		"function(){}",
			"FUNCTION@1\n\tLIST@?\n\tLIST@?\n\t\tNIL@?",

		"function(){4}",
			"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tNUMBER=4@1",

		"function(){call()}",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tCALL@1\n\t\t\tIDENTIFIER=call@1\n\t\t\tLIST@?",

		"function() { \n#() }",
			"FUNCTION@1\n\tLIST@?\n\tLIST@2\n\t\tOBJECT@2",

		"function(){return}",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tRETURN@1",

		"function() {\n" +
			"return { break; }\n" +
		"}",
		"FUNCTION@1\n\tLIST@?\n\tLIST@2\n\t\tRETURN@2\n\t\t\tBLOCK@2\n\t\t\t\tLIST@?\n\t\t\t\tLIST@2\n\t\t\t\t\tBREAK@2",

		"function(@args) { return { continue; } $ { it } }",
		"FUNCTION@1\n" +
				"\tLIST@?\n" +
				"\t\tIDENTIFIER=@args@?\n" +
				"\tLIST@1\n" +
				"\t\tRETURN@1\n" +
				"\t\t\tBINARYOP@1\n" +
				"\t\t\t\tCAT@?\n" +
				"\t\t\t\tBLOCK@1\n" +
				"\t\t\t\t\tLIST@?\n" +
				"\t\t\t\t\tLIST@1\n" +
				"\t\t\t\t\t\tCONTINUE@1\n" +
				"\t\t\t\tBLOCK@1\n" +
				"\t\t\t\t\tLIST@?\n" +
				"\t\t\t\t\t\tIDENTIFIER=it@?\n" +
				"\t\t\t\t\tLIST@1\n" +
				"\t\t\t\t\t\tIDENTIFIER=it@1",

		"function(){throw\n'x'}",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tTHROW@1\n\t\t\tSTRING=x@2",

		"function(){(x)}",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tRVALUE@1\n\t\t\tIDENTIFIER=x@1",

		"function(){x in (1,2\n'3')}",
		"FUNCTION@1\n" +
				"\tLIST@?\n" +
				"\tLIST@2\n" +
				"\t\tIN@2\n" +
				"\t\t\tIDENTIFIER=x@1\n" +
				"\t\t\tLIST@2\n" +
				"\t\t\t\tNUMBER=1@1\n" +
				"\t\t\t\tNUMBER=2@1\n" +
				"\t\t\t\tSTRING=3@2",

		"function() {\n x\n?y\n:z }",
		"FUNCTION@1\n\tLIST@?\n\tLIST@4\n\t\tQ_MARK@4\n\t\t\tIDENTIFIER=x@2\n\t\t\tIDENTIFIER=y@3\n\t\t\tIDENTIFIER=z@4",

		"function(x){\n'literal'and 4 and true and false or -5 + x - class{}*function(){}${#20140827}\n}",
		"FUNCTION@1\n" +
				"\tLIST@?\n" +
				"\t\tIDENTIFIER=x@?\n" +
				"\tLIST@2\n" +
				"\t\tOR@2\n" +
				"\t\t\tAND@2\n" +
				"\t\t\t\tSTRING=literal@2\n" +
				"\t\t\t\tNUMBER=4@2\n" +
				"\t\t\t\tTRUE@2\n" +
				"\t\t\t\tFALSE@2\n" +
				"\t\t\tBINARYOP@2\n" +
				"\t\t\t\tCAT@?\n" +
				"\t\t\t\tBINARYOP@2\n" +
				"\t\t\t\t\tSUB@?\n" +
				"\t\t\t\t\tBINARYOP@2\n" +
				"\t\t\t\t\t\tADD@?\n" +
				"\t\t\t\t\t\tSUB@2\n" +
				"\t\t\t\t\t\t\tNUMBER=5@2\n" +
				"\t\t\t\t\t\tIDENTIFIER=x@2\n" +
				"\t\t\t\t\tBINARYOP@2\n" +
				"\t\t\t\t\t\tMUL@?\n" +
				"\t\t\t\t\t\tCLASS@2\n" +
				"\t\t\t\t\t\t\tLIST@?\n" +
				"\t\t\t\t\t\tFUNCTION@2\n" +
				"\t\t\t\t\t\t\tLIST@?\n" +
				"\t\t\t\t\t\t\tLIST@?\n" +
				"\t\t\t\t\t\t\t\tNIL@?\n" +
				"\t\t\t\tBLOCK@2\n" +
				"\t\t\t\t\tLIST@?\n" +
				"\t\t\t\t\tLIST@2\n" +
				"\t\t\t\t\t\tDATE=20140827@2",

		"function() { switch(x) { } }",
		"FUNCTION@1\n" +
				"\tLIST@?\n" +
				"\tLIST@1\n" +
				"\t\tSWITCH@1\n" +
				"\t\t\tRVALUE@1\n" +
				"\t\t\t\tIDENTIFIER=x@1\n" +
				"\t\t\tLIST@1\n" +
				"\t\t\t\tCASE@1\n" +
				"\t\t\t\t\tLIST@?\n" +
				"\t\t\t\t\tLIST@1\n" +
				"\t\t\t\t\t\tTHROW@1\n" +
				"\t\t\t\t\t\t\tSTRING=unhandled switch case@1",

		"function () { .a }",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tMEMBER=a@1\n\t\t\tSELFREF@1",

		"function() { return [] } ",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tRETURN@1\n\t\t\tCALL@1\n\t\t\t\tIDENTIFIER=Record@1\n\t\t\t\tLIST@?",

		"function() { return x[..1] }",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tRETURN@1\n\t\t\tSUBSCRIPT@1\n\t\t\t\tIDENTIFIER=x@1\n\t\t\t\tRANGETO@1\n\t\t\t\t\tNUMBER=0@1\n\t\t\t\t\tNUMBER=1@1",

		"function() { return x[1..] }",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tRETURN@1\n\t\t\tSUBSCRIPT@1\n\t\t\t\tIDENTIFIER=x@1\n\t\t\t\tRANGETO@1\n\t\t\t\t\tNUMBER=1@1\n\t\t\t\t\tNUMBER=2147483647@1",

		"function() { super.Destroy() }",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tCALL@1\n\t\t\tSUPER=Destroy@1\n\t\t\tLIST@?",

		"function() { x(y:1) }",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tCALL@1\n\t\t\tIDENTIFIER=x@1\n\t\t\tLIST@1\n\t\t\t\tARG@1\n\t\t\t\t\tSTRING=y@?\n\t\t\t\t\tNUMBER=1@1",

		"function() { x(y:) }",
		"FUNCTION@1\n\tLIST@?\n\tLIST@1\n\t\tCALL@1\n\t\t\tIDENTIFIER=x@1\n\t\t\tLIST@1\n\t\t\t\tARG@1\n\t\t\t\t\tSTRING=y@?\n\t\t\t\t\tTRUE@1",

//		"function() { new X }",
//		"",
//
		"class { method() { .x } }",
		"CLASS@1\n\tLIST@1\n\t\tMEMBER@1\n\t\t\tSTRING=method@1\n\t\t\tMETHOD@1\n\t\t\t\tLIST@?\n\t\t\t\tLIST@1\n\t\t\t\t\tMEMBER=x@1\n\t\t\t\t\t\tSELFREF@1",

		"class { Method() { .X() } }",
		"CLASS@1\n" +
				"\tLIST@1\n" +
				"\t\tMEMBER@1\n" +
				"\t\t\tSTRING=Method@1\n" +
				"\t\t\tMETHOD@1\n" +
				"\t\t\t\tLIST@?\n" +
				"\t\t\t\tLIST@1\n" +
				"\t\t\t\t\tCALL@1\n" +
				"\t\t\t\t\t\tMEMBER=X@1\n" +
				"\t\t\t\t\t\t\tSELFREF@1\n" +
				"\t\t\t\t\t\tLIST@?",

		"class { method() {\n\n4\n $\n#20140828} }",
		"CLASS@1\n" +
				"\tLIST@1\n" +
				"\t\tMEMBER@1\n" +
				"\t\t\tSTRING=method@1\n" +
				"\t\t\tMETHOD@1\n" +
				"\t\t\t\tLIST@?\n" +
				"\t\t\t\tLIST@5\n" +
				"\t\t\t\t\tBINARYOP@5\n" +
				"\t\t\t\t\t\tCAT@?\n" +
				"\t\t\t\t\t\tNUMBER=4@3\n" +
				"\t\t\t\t\t\tDATE=20140828@5",
	};

	@DllInterface
	private static final String[] JSDI_TEST_CASES = {
		"struct {}",
		"STRUCT@1\n\tLIST@?",

		"dll void void:fakeFunction()",
		"DLL@1\n\tIDENTIFIER=void@?\n\tSTRING=fakeFunction@?\n\tIDENTIFIER=void@?\n\tLIST@?",

		"callback()",
		"CALLBACK@1\n\tLIST@?",
	};

	
	

	//
	// INTERNALS
	//

	private static void runTests(String[] testCases) {
		assertTrue(0 == testCases.length % 2);
		for (int k = 0; k < testCases.length; k += 2) {
			final String sourceCode = testCases[k + 0];
			final String expectedPrintout = testCases[k + 1];
			test(sourceCode, expectedPrintout);
		}
	}

	private static class Printer extends AstNode.Visitor {
		public final StringBuilder builder = new StringBuilder(512);
		private final StringBuilder indent = new StringBuilder(16);

		void print(AstNode ast) {
			builder.append(indent);
			if (null != ast) {
				builder.append(ast.token);
				if (null != ast.value) {
					builder.append('=').append(ast.value);
				}
				if (AstNode.UNKNOWN_LINE_NUMBER != ast.lineNumber) {
					builder.append('@').append(ast.lineNumber);
				} else {
					builder.append("@?");
				}
			} else {
				builder.append("null");
			}
			builder.append('\n');
		}

		@Override
		boolean topDown(AstNode ast) {
			print(ast);
			indent.append('\t');
			return true;
		}

		@Override
		void bottomUp(AstNode ast) {
			trim(indent);
		}

		public void trim() {
			trim(builder);
		}

		private static void trim(StringBuilder x) {
			x.delete(x.length() - 1, x.length());
		}
	}

	private static String print(AstNode tree) {
		Printer printer = new Printer();
		tree.depthFirst(printer);
		printer.trim();
		return printer.builder.toString();
	}

	private static void test(String sourceCode, String expectedPrintout) {
		Lexer lexer = new Lexer(sourceCode);
		ParseConstant<AstNode, AstGenerator> parser = new ParseConstant<AstNode, AstGenerator>(
				lexer, new AstGenerator());
		AstNode ast = parser.constant();
		String printout = print(ast);
		assertEquals(expectedPrintout, printout);
	}
}
