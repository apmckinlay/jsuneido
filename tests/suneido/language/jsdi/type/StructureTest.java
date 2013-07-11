package suneido.language.jsdi.type;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.Assumption;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.MarshallPlan;

/**
 * Test for {@link Structure}.
 * 
 * @author Victor Schappert
 * @since 20130703
 * @see suneido.language.ParseAndCompileStructTest
 */
public class StructureTest {

	@BeforeClass
	public static void beforeClass() {
		Assumption.jvmIs32BitOnWindows();
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_STRUCTS = {
		"RECT", "struct { long left; long top; long right; long bottom; }",
		"POINT", "struct { long x; long y; }",
		"TwoTierStruct",
			"struct\n" +
				"\t{\n" +
				"\tPOINT[2] pts ;\n" +
				"\tRECT * prcOptional ;\n" +
				"\tlong[10] extra ;\n" +
				"\tlong * pLong ;\n" +
				"\t}",
		"ThreeTierStruct",
			"struct\n" +
				"\t{\n" +
				"\tTwoTierStruct[1] tts1\n" +
				"\tTwoTierStruct *  ttp\n" +
				"\tTwoTierStruct    tts2\n" +
				"\tTwoTierStruct[2] tts3\n" +
				"}\n",
			// THIS IS WHAT ThreeTierStruct's MARSHALLED REPRESENTATION LOOKS
			// LIKE:
			//
			//     area   byte   size   desc
			//      D       0     64    tts1
			//        d       16     4    tts1.prcOptional
			//        d       60     4    tts1.pLong
			//      D      64      4    ttp
			//      D      68     64    tts2
			//        d       84     4    tts2.prcOptional
			//        d      128     4    tts2.pLong
			//      D     132     64    tts3[0]
			//        d      148     4     tts3[0].prcOptional
			//        d      192     4     tts3[0].pLong
			//      D     196     64    tts3[1]
			//        d      212     4     tts3[1].prcOptional
			//        d      256     4     tts3[1].prcOptional
			//      I     260     16    *tts1.prcOptional
			//      I     276      4    *tts1.pLong
			//      I     280     64    *ttp
			//        i     296      4  ttp->prcOptional
			//        i     340      4  ttp->pLong
			//          i     344     16  *ttp->prcOptional
			//          i     360      4  *ttp->pLong
			//      I     364     16    *tts2.prcOptional
			//      I     380      4    *tts2.pLong
			//      I     384     16    *tts3[0].prcOptional
			//      I     400      4    *tts3[0].pLong
			//      I     404     16    *tts3[1].prcOptional
			//      I     420      4    *tts3[1].pLong
			//
		"CycleA", "struct { CycleB cycleB }",
		"CycleB", "struct { CycleA cycleA }"
	};

	private static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	private static Structure get(String name) {
		return (Structure)CONTEXT.get(name);
	}

	private static MarshallPlan getMarshallPlan(String name) {
		Structure struct = get(name);
		struct.resolve(0);
		return struct.getMarshallPlan();
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		CONTEXT = new SimpleContext(NAMED_STRUCTS);
	}

	@Test
	public void testSize() {
		assertEquals(16, eval("RECT.Size()"));
		assertEquals( 8, eval("POINT.Size()"));
		assertEquals(64, eval("TwoTierStruct.Size()"));
	}

	@Test
	public void testMarshallPlan() {
		assertEquals("MarshallPlan[ 16, 0, { } ]",
			getMarshallPlan("RECT").toString());
		assertEquals("MarshallPlan[ 8, 0, { } ]",
			getMarshallPlan("POINT").toString());
		assertEquals("MarshallPlan[ 64, 20, { 16:64, 60:80 } ]",
			getMarshallPlan("TwoTierStruct").toString());
		assertEquals("MarshallPlan[ 260, 164, { 16:260, 60:276, 64:280, 84:364, 128:380, 148:384, 192:400, 212:404, 256:420, 296:344, 340:360 } ]",
			getMarshallPlan("ThreeTierStruct").toString());
	}

	@Test(expected=JSDIException.class)
	public void testCycle() {
		eval("CycleA.Size()");
	}
}
