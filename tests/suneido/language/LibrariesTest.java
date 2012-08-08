package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LibrariesTest {

	@Test
	public void overload_function() {
		Globals.setForTest("F", Compiler.compile("F", "function () { 123 }"));
		Object f = Compiler.compile("F", "function () { _F() }");
		Object x = Ops.call(f);
		assertEquals(123, x);
	}

	@Test
	public void overload_class() {
		Globals.setForTest("X", Compiler.compile("X", "class { F() { 123 } }"));
		Object c = Compiler.compile("X", "class : _X { }");
		Object x = Ops.invoke(c, "F");
		assertEquals(123, x);
	}

}
