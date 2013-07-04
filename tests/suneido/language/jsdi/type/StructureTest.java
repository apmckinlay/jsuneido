package suneido.language.jsdi.type;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.language.Compiler;
import suneido.language.Context;
import suneido.language.ContextLayered;
import suneido.language.Contexts;

/**
 * Test for {@link Structure}.
 * 
 * @author Victor Schappert
 * @since 20130703
 */
public class StructureTest {

	private static ContextLayered CONTEXT;
	private static final String[] CONTEXT_STRUCTS = {
		"RECT", "struct { long left; long top; long right; long bottom; }",
		"POINT", "struct { long x; long y; }",
		"Everything", "struct\n" +
		                  "\t{\n" +
		                  "\tbool a\n" +
		                  "\tbool * pa\n" +
		                  "\tbool[2] aa\n" +
		                  "\tchar b\n" +
		                  "\tchar * pb\n" +
		                  "\tchar[2] ab\n" +
		                  "\tshort c\n" +
		                  "\tshort * pc\n" +
		                  "\tshort[2] ac\n" +
		                  "\tlong d\n" +
		                  "\tlong * pd\n" +
		                  "\tlong[2] ad\n" +
		                  "\tint64 e\n" +
		                  "\tint64 * pe\n" +
		                  "\tint64[2] ae\n" +
		                  "\t}",
		"CycleA", "struct { CycleB cycleB }",
		"CycleB", "struct { CycleA cycleA }"
	};

	private static Structure get(String name) {
		return (Structure)CONTEXT.get(name);
	}

	private static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		CONTEXT = new SimpleContext(CONTEXT_STRUCTS);
	}

	@Test
	public void testSize() {
		assertEquals(16, eval("RECT.Size()"));
	}

}
