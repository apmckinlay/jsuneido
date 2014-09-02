/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.Suneido;
import suneido.compiler.Compiler;
import suneido.runtime.Ops;

public class LibrariesTest {

	@Test
	public void overload_function() {
		Suneido.context.set("F", Compiler.compile("F", "function () { 123 }"));
		Object f = Compiler.compile("F", "function () { _F() }");
		Object x = Ops.call(f);
		assertEquals(123, x);
	}

	@Test
	public void overload_class() {
		Suneido.context.set("X", Compiler.compile("X", "class { A() { 123 } }"));
		Object c = Compiler.compile("X", "class : _X { }");
		Object x = Ops.invoke(c, "A");
		assertEquals(123, x);
	}

}
