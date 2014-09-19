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
	public void testThrowSimple() {
		test("throw 'a'", 1, "a", FUNCTION);
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
