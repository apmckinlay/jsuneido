package suneido.language.jsdi.type;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.Assumption;
import suneido.SuContainer;
import suneido.language.Compiler;
import suneido.language.Concats;
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
		"StringStruct2", "struct { StringStruct1[2] a; buffer[4] b; buffer c; }",
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

	private static int countVariableIndirect(String name, Object value) {
		Structure struct = get(name);
		struct.resolve(0);
		return struct.countVariableIndirect(value);
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
		assertEquals("MarshallPlan[ 16, 0, { }, no-vi ]",
			getMarshallPlan("RECT").toString());
		assertEquals("MarshallPlan[ 8, 0, { }, no-vi ]",
			getMarshallPlan("POINT").toString());
		assertEquals("MarshallPlan[ 64, 20, { 16:64, 60:80 }, no-vi ]",
			getMarshallPlan("TwoTierStruct").toString());
		assertEquals("MarshallPlan[ 260, 164, { 16:260, 60:276, 64:280, 84:364, 128:380, 148:384, 192:400, 212:404, 256:420, 296:344, 340:360 }, no-vi ]",
			getMarshallPlan("ThreeTierStruct").toString());
		assertEquals("MarshallPlan[ 12, 0, { 4:-1 }, vi ]",
			getMarshallPlan("StringStruct1").toString());
		assertEquals("MarshallPlan[ 32, 0, { 4:-1, 16:-1, 28:-1 }, vi ]",
			getMarshallPlan("StringStruct2").toString());
		assertEquals("MarshallPlan[ 40, 32, { 8:-1, 20:-1, 32:-1, 36:40, 44:-1, 56:-1, 68:-1 }, vi ]",
			getMarshallPlan("StringStruct3").toString());
	}

	@Test(expected=JSDIException.class)
	public void testCycle() {
		eval("CycleA.Size()");
	}

	@Test
	public void testCountVariableIndirect() {
		// Passing anything that's not an SuContainer to a structure's
		// countVariableIndirect() method should return 0.
		final int N = NAMED_STRUCTS.length;
		final Object ob = new Object();
		for (int k = 0; k < N; k += 2) {
			assertEquals(0, get(NAMED_STRUCTS[k]).countVariableIndirect(ob));
		}
		// Test passing string values to members that can't be strings
		SuContainer c;
		c = SuContainer.fromKVPairs("left", "abcdefg", "top", new Concats("x", "y"));
		assertEquals(0, get("RECT").countVariableIndirect(c));
		// Test passing non-string values to members which expect strings
		c = SuContainer.fromKVPairs("c", 1, "d", "xyz" );
		assertEquals(0, get("StringStruct1").countVariableIndirect(c));
		// Test passing string value to a member which expects a string
		c = SuContainer.fromKVPairs("c", new Concats("hello ", "world"));
		assertEquals(12, get("StringStruct1").countVariableIndirect(c)); // 11 + 1 for NUL
		// Two tiers
		c = SuContainer.fromKVPairs(
				"a",
				SuContainer.fromKVPairs(
					0, SuContainer.fromKVPairs("c", "1"),
					1, SuContainer.fromKVPairs("c", "22")
				),
				"c", "this buffer contains 61 characters and is not zero-terminated"
		);
		assertEquals(66, countVariableIndirect("StringStruct2", c));
		// Three tiers plus pointer
		c = SuContainer.fromKVPairs(
				"a", 123,
				"b", // StringStruct2 b
				SuContainer.fromKVPairs(
					"a",
					SuContainer.fromKVPairs(
						0, SuContainer.fromKVPairs("c", "1")
					),
					"c", "1"
				),
				"c", // StringStruct2 * c
				SuContainer.fromKVPairs(
					"a",
					SuContainer.fromKVPairs(
						0, SuContainer.fromKVPairs("c", "4444"),
						1, SuContainer.fromKVPairs("c", "666666")
					)
				)
		);
		assertEquals(15, countVariableIndirect("StringStruct3", c));
	}
}
