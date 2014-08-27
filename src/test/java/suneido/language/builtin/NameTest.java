/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static org.junit.Assert.assertEquals;
import static suneido.language.Compiler.compile;
import static suneido.language.Compiler.eval;

import org.junit.Before;
import org.junit.Test;

import suneido.Suneido;
import suneido.jsdi.DllInterface;
import suneido.util.testing.Assumption;

/**
 * Test for the {@link Name} built-in.
 *
 * @author Victor Schappert
 * @since 20130815
 * @see TypeTest
 */
public class NameTest {

	@Test
	public void testBasicLocal() {
		test("Name(Name)", "Name");
		test("Name('')", "");
		test("Name(x = '')", "");
		test("Name(0)", "");
		test("Name(x = 0)", "");
		test("Name(-1)", "");
		test("Name(x = -1)", "");
		test("Name(false)", "");
		test("Name(x = false)", "");
		test("Name(true)", "");
		test("Name(x = true)", "");
		test("Name(#20130815.163730500)", "");
		test("Name(x = #20130815.163730500)", "");
		test("Name(#())", "");
		test("Name(#{})", "");
		test("Name(Object())", "");
		test("Name(Record())", "");
		test("Name(function() { })", "");
//		test("Name(x = function() { })", "x"); FIXME: This test case should work, as it does on CSuneido
//		test("Name(class { })", "");           FIXME: This test case should work, as it does on CSuneido
//		test("Name(x = class { })", "x");      FIXME: This test case should work, as it does on CSuneido
		test("Name(new (class { }))", "");
		test("Name((class { })())", "");
		test("Name(x = (class { })())", "");
		test("Name(x = new (class { }))", "");
		test("Name({ })", "");
		test("Name(x = { })", "");
		test("x = class { Method() { } }; Name(x.Method)", "");
		test("x = class { Method() { } }; Name(x().Method)", "");
		// TODO: need testing for named functions
//		test("Name(x = class { })", "x");
	}

	@Test
	public void testBasicInContext() {
		define("A", "''");
		test("Name(A)", "");

		define("A", "A");
		test("Name(A)", "");

		define("A", "0");
		test("Name(A)", "");

		define("A", "-1");
		test("Name(A)", "");

		define("A", "true");
		test("Name(A)", "");

		define("A", "false");
		test("Name(A)", "");

		define("A", "#19820207.123456789");
		test("Name(A)", "");

		define("A", "#()");
		test("Name(A)", "");

		define("A", "#{}");
		test("Name(A)", "");

		define("A", "function() { }");
//		test("Name(A)", "A");                  FIXME: This test case should work, as it does on CSuneido
//		test("x = A; Name(x)", "A");           FIXME: This test case should work, as it does on CSuneido

		define("B", "class { }");
		test("Name(B)", "B");
		test("x = B; Name(x)", "B");
	}

	@Test
	@DllInterface
	public void testJSDILocal() {
		Assumption.jvmIsOnWindows();
		test("Name(Buffer(1, ''))", "");
		test("Name(x = Buffer(1, ''))", "");
//		test("Name(struct { char x })", "");      FIXME: This test case should work, as it does on CSuneido
//		test("Name(x = struct { char x })", "");  FIXME: This test case should work, as it does on CSuneido
//		test("Name(callback())", "");             FIXME: This test case should work, as it does on CSuneido
//		test("Name(x = callback())", "");         FIXME: This test case should work, as it does on CSuneido
//		test("Name(dll void void:f())", "");      FIXME: This test case should work, as it does on CSuneido
//		test("Name(x = dll void void:f())", "");  FIXME: This test case should work, as it does on CSuneido
	}

	@Test
	@DllInterface
	public void testJSDIInContext() {
		Assumption.jvmIsOnWindows();

		define("A", "struct { char x }");
		test("Name(A)", "A");
		test("x = A; Name(x)", "A");

		define("B", "callback()");
		test("Name(B)", "B");
		test("x = B; Name(x)", "B");

		define("C", "dll void void:f()");
		test("Name(C)", "C");
		test("x = C; Name(x)", "C");
	}

	@Before
	public void beforeTest() {
		Suneido.context.clearAll();
	}

	public static void test(String expr, String result) {
		assertEquals(result, eval(expr));
	}

	static void define(String name, String definition) {
		Suneido.context.set(name, compile(name, definition));
	}
}
