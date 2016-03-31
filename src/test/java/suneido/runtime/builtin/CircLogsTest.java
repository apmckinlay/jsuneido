/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static org.junit.Assert.assertEquals;
import static suneido.compiler.Compiler.eval;

import org.junit.Test;

public class CircLogsTest {

	@Test
	public void test_circLog() {
		CircLog.clear();
		test("CircLog()", "");
		CircLog.clear();
		test("CircLog('test')", null);
		test("CircLog()", "test\n");
		CircLog.clear();
		test("CircLog('test')", null);
		test("CircLog('test2')", null);
		test("CircLog()", "test\ntest2\n");
		CircLog.clear();
		test("CircLog()", "");
		CircLog.clear();
		test("CircLog(\" test \")", null);
		test("CircLog(\" test2 \")", null);
		test("CircLog()", "test\ntest2\n");
	}

	@Test
	public void test_emptyString(){
		CircLog.clear();

		test("CircLog('')", null);
		assertEquals(0, CircLog.index());
		test("CircLog()", "");

		test("CircLog(' \t ')", null);
		assertEquals(0, CircLog.index());
		test("CircLog()", "");

		test("CircLog(' abc ')", null);
		assertEquals(0 + 1, CircLog.index());
		test("CircLog()", "abc\n");
	}

	private static void test(String expr, String result) {
		assertEquals(result, eval(expr));
	}

}
