package suneido.compiler;

import static org.junit.Assert.*;

import org.junit.Test;

import suneido.SuException;
import suneido.debug.Callstack;
import suneido.debug.Frame;
import suneido.runtime.CallableType;
import static suneido.runtime.CallableType.*;

/**
 * Test to ensure compiled Suneido source code is tagged with the correct line
 * numbers.
 *
 * @author Victor Schappert
 * @since 20140919
 * @see LineNumbersParseTest
 */
public class LineNumbersCompileTest {

	@Test
	public void testFunctionSimple() {
		test("throw 'a'", 1, "a", FUNCTION);
	}

	@Test
	public void testFunctionSimple2() {
		test("throw\n'a'", 1, "a", FUNCTION);
	}

	@Test
	public void testFunctionExtraTokenAfter() {
		test("throw 'abc'\n'irrelevant extra string literal'", 1, "abc",
				FUNCTION);
	}

	@Test
	public void testFunctionExtraTokenAfter2() {
		test("x = 4\nthrow 'abc'\n'irrelevant extra string literal'", 2, "abc",
				FUNCTION);
	}

	@Test
	public void testFunctionSemi() {
		test("x = 4 ; \n throw 'abc'\n'irrelevant extra string literal'", 2,
				"abc", FUNCTION);
	}

	@Test
	public void testFunctionCRLF() {
		test("throw 'abc'\r\n'irrelevant extra string literal'\r\n", 1, "abc",
				FUNCTION);
	}

	@Test
	public void testFunctionIf() {
		test("x = 4\nif 3 < x\nthrow x", 3, "4", FUNCTION);
	}

	@Test
	public void testFunctionElse() {
		test("x = 4\nif x < 3\n\tthrow false\nelse\n\tthrow x", 5, "4",
				FUNCTION);
	}

	@Test
	public void testFunctionLoop() {
		test("for (k = 0; k < 10; ++k)\n\tif 5 < k\n\t\tthrow k", 3, "6",
				FUNCTION);
	}

	@Test
	public void testBlockSimple() {
		test("(x = { throw 'x' })()", 1, "x", BLOCK);
	}

	@Test
	public void testBlockSimple2() {
		test("(x = {\n\r\n    throw 'x' })()", 3, "x", BLOCK);
	}

	@Test
	public void testBlockIf() {
		test("x = {\n y = 4\nif 3 < y\nthrow y\n}\nx()", 4, "4", BLOCK);
	}

	@Test
	public void testBlockElse() {
		test("x = {\n y = 4\nif y < 3\nthrow false\nelse\nthrow y\n}\nx()", 6,
				"4", BLOCK);
	}

	@Test
	public void testBlockLoop() {
		test("x = {\n for (y = 0; y < 10; ++y)\nif 9 <= y\nthrow y\n}\nx()", 4,
				"9", BLOCK);
	}

	//
	// INTERNALS
	//

	void test(String code, int lineNumber, String message,
			CallableType callableType) {
		String code2 = "function(){" + code + "}()";
		try {
			Compiler.eval(code2);
		} catch (SuException e) {
			assertEquals(message, e.getMessage());
			Callstack c = e.getCallstack();
			Frame f = c.frames().get(0);
			assertEquals(lineNumber, f.getLineNumber());
			assertEquals(callableType, f.getCallableType());
			return;
		}
		throw new AssertionError("Failed to catch an exception running code '"
				+ code + "'");

	}
}
