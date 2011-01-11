package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LibrariesTest {

	@Test
	public void overload_function() {
		Globals.put("X", Compiler.compile("X", "function () { 123 }"));
		Object f = Compiler.compile("X", "function () { _X() }");
		Object x = Ops.call(f);
		assertEquals(123, x);
	}

	@Test
	public void overload_class() {
		Globals.put("X", Compiler.compile("X", "class { F() { 123 } }"));
		Object c = Compiler.compile("X", "class : _X { }");
		Object x = Ops.invoke(c, "F");
		assertEquals(123, x);
	}

}
