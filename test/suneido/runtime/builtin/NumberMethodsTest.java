/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import org.junit.Test;

import suneido.PortTests;
import suneido.compiler.ExecuteTest;

public class NumberMethodsTest {

	@Test
	public void porttests() {
		PortTests.addTest("method", ExecuteTest::pt_method);
		assert PortTests.runFile("number.test");
	}

}
