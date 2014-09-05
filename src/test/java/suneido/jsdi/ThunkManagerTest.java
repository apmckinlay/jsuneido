package suneido.jsdi;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.compiler.Compiler;
import suneido.util.testing.Assumption;

/**
 * Test for {@link ThunkManager}.
 *
 * @author Victor Schappert
 * @since 20140813
 */
public class ThunkManagerTest {

	@BeforeClass
	public static void setupBeforeClass() {
		Assumption.jsdiIsAvailable(); // Prevent failure on Mac OS, Linux, etc.
	}

	private static Object eval(String src) {
		return Compiler.eval(src);
	}

	@Test
	public void testClearNonCallable() {
		assertSame(Boolean.FALSE,
				ThunkManager.ClearCallback.ClearCallback(Boolean.FALSE));
		assertSame(Boolean.FALSE, eval("ClearCallback('not a callback!')"));
	}

	@Test
	public void testClearNotBound() {
		assertSame(Boolean.FALSE, eval("ClearCallback(function() {})"));
	}
}
