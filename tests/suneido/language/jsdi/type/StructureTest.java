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
		"CycleB", "struct { CycleA cycleA }",
		"StringStruct1", "struct { short a; short b; string c; string[4] d; }",
		"StringStruct2", "struct { StringStruct1[2] a; buffer[8] b; buffer c; }",
		"StringStruct3", "struct { long a; StringStruct2 b; StringStruct2 * c }"
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
		assertEquals(12, eval("StringStruct1.Size()"));
		assertEquals(36, eval("StringStruct2.Size()"));
		assertEquals(44, eval("StringStruct3.Size()"));
	}

	@Test
	public void testMarshallPlan() {
		assertEquals("MarshallPlan[ 16, 0, { }, { 0, 4, 8, 12 }, #vi:0 ]",
				getMarshallPlan("RECT").toString());
		assertEquals("MarshallPlan[ 8, 0, { }, { 0, 4 }, #vi:0 ]",
				getMarshallPlan("POINT").toString());
		assertEquals(
				"MarshallPlan[ 64, 20, { 16:64, 60:80 }, " +
					"{ 0, 4, 8, 12, 16, 64, 68, 72, 76, 20, 24, 28, 32, 36, " +
					"40, 44, 48, 52, 56, 60, 80 }, #vi:0 ]",
				getMarshallPlan("TwoTierStruct").toString());
		assertEquals(
				"MarshallPlan[ 260, 164, " +
					"{ 16:260, 60:276, 64:280, 296:344, 340:360, 84:364, 128:380, 148:384, " +
					"192:400, 212:404, 256:420 }, " +
						// tts1
					"{ 0, 4, 8, 12, 16, 260, 264, 268, 272, 20, 24, 28, 32, 36, " +
						"40, 44, 48, 52, 56, 60, 276, " +
						// ttp
					"64, 280, 284, 288, 292, 296, 344, 348, 352, 356, 300, 304, "+ 
						"308, 312, 316, 320, 324, 328, 332, 336, 340, 360, " +
						// tts2
					"68, 72, 76, 80, 84, 364, 368, 372, 376, 88, 92, 96, 100, 104, " +
						"108, 112, 116, 120, 124, 128, 380, " +
						// tts3[0]
					"132, 136, 140, 144, 148, 384, 388, 392, 396, 152, 156, 160, " +
						"164, 168, 172, 176, 180, 184, 188, 192, 400, " +
						// tts3[1]
					"196, 200, 204, 208, 212, 404, 408, 412, 416, 216, 220, 224, " +
						"228, 232, 236, 240, 244, 248, 252, 256, 420 " +
					"}, #vi:0 ]",
				getMarshallPlan("ThreeTierStruct").toString());
		assertEquals("MarshallPlan[ 12, 0, { 4:-1 }, { 0, 2, 4, 8 }, #vi:1 ]",
				getMarshallPlan("StringStruct1").toString());
		assertEquals(
				"MarshallPlan[ 36, 0, { 4:-1, 16:-1, 32:-1 }, " +
				"{ 0, 2, 4, 8, 12, 14, 16, 20, 24, 32 }, #vi:3 ]",
				getMarshallPlan("StringStruct2").toString());
		assertEquals(
				"MarshallPlan[ 44, 36, { 8:-1, 20:-1, 36:-1, 40:44, 48:-1, 60:-1, 76:-1 }, " +
					"{ 0, 4, 6, 8, 12, 16, 18, 20, 24, 28, 36, 40, " +
					"44, 46, 48, 52, 56, 58, 60, 64, 68, 76 }, #vi:6 ]",
				getMarshallPlan("StringStruct3").toString());
	}

	@Test(expected=JSDIException.class)
	public void testCycle() {
		eval("CycleA.Size()");
	}
}
