/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */
package suneido.runtime.builtin;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static suneido.compiler.Compiler.eval;

public class CircLogsTest {

	@Test
	public void test_circLog() {
		clear();
		test("CircLog()", "");
		clear();
		test("CircLog('test')", null);
		test("CircLog()", "test\n");
		clear();
		test("CircLog('test')", null);
		test("CircLog('test2')", null);
		test("CircLog()", "test\ntest2\n");
		clear();
		test("CircLog()", "");
		clear();
		test("CircLog(\" test \")", null);
		test("CircLog(\" test2 \")", null);
		test("CircLog()", "test\ntest2\n");
	}
	
	@Test
	public void test_emptyString(){
		clear();
		int prevQi = CircLog.qi;
		
		test("CircLog('')", null);
		assertEquals(prevQi, CircLog.qi);
		test("CircLog()", "");
		
		test("CircLog(' \t ')", null);
		assertEquals(prevQi, CircLog.qi);
		test("CircLog()", "");

		test("CircLog(' abc ')", null);
		assertEquals(prevQi + 1, CircLog.qi);
		test("CircLog()", "abc\n");
	}
	
	private static void test(String expr, String result) {
		assertEquals(result, eval(expr));
	}
	
	private static void clear() {
		Arrays.fill(CircLog.queue, 0, CircLog.QSIZE - 1, "");
	}
}
