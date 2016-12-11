/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.jsdi.DllInterface;
import suneido.jsdi.type.Structure;
import suneido.jsdi.type.StructureTest;
import suneido.util.testing.Assumption;

/**
 * AMD64-specific tests for {@link Structure}.
 *
 * @author Victor Schappert
 * @since 20140803
 * @see suneido.jsdi.type.StructureTest
 */
@DllInterface
public class StructureTest64 {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs64BitOnWindows();
	}

	// THIS IS WHAT ThreeTierStruct's MARSHALLED REPRESENTATION LOOKS LIKE on
	// amd64:
	//
	//     area   byte   size   desc
	//      D       0     72    tts1
	//        d       16     8    tts1.prcOptional
	//        d       64     8    tts1.pLong
	//      D      72      4    ttp
	//      D      80     64    tts2
	//        d       96   4    tts2.prcOptional
	//        d      144     4    tts2.pLong
	//      D     152     64    tts3[0]
	//        d      168     4     tts3[0].prcOptional
	//        d      216     4     tts3[0].pLong
	//      D     224     64    tts3[1]
	//        d      240     4     tts3[1].prcOptional
	//        d      288     4     tts3[1].prcOptional
	//      I     296     16    *tts1.prcOptional
	//      I     312      4    *tts1.pLong
	//      I     320     72    *ttp
	//        i     336      4  ttp->prcOptional
	//        i     384      4  ttp->pLong
	//          i     392     16  *ttp->prcOptional
	//          i     408      4  *ttp->pLong
	//      I     412     16    *tts2.prcOptional
	//      I     428      4    *tts2.pLong
	//      I     432     16    *tts3[0].prcOptional
	//      I     448      4    *tts3[0].pLong
	//      I     452     16    *tts3[1].prcOptional
	//      I     468      4    *tts3[1].pLong
	//

	private static MarshallPlan64 getMarshallPlan(String name) {
		Structure struct = StructureTest.getAndResolve(name);
		MarshallPlanBuilder64 builder = new MarshallPlanBuilder64(
				struct.getVariableIndirectCount(), false);
		struct.addToPlan(builder, false);
		return (MarshallPlan64)builder.makeMarshallPlan();
	}

	public static Object eval(String src) {
		return StructureTest.eval(src);
	}

	@Test
	public void testSize() {
		assertEquals(16, eval("RECT.Size()"));
		assertEquals( 8, eval("POINT.Size()"));
		assertEquals(72, eval("TwoTierStruct.Size()"));
		assertEquals(24, eval("StringStruct1.Size()"));
		assertEquals(64, eval("StringStruct2.Size()"));
		assertEquals(80, eval("StringStruct3.Size()"));
	}

	@Test
	public void testMarshallPlan() {
		assertEquals("MarshallPlan64[ 16, 16, { }, { 0, 4, 8, 12 }, #vi:0 ]",
				getMarshallPlan("RECT").toString());
		assertEquals("MarshallPlan64[ 8, 8, { }, { 0, 4 }, #vi:0 ]",
				getMarshallPlan("POINT").toString());
		assertEquals(
				"MarshallPlan64[ 72, 96, { 16:72, 64:88 }, " +
					"{ 0, 4, 8, 12, 16, 72, 76, 80, 84, 24, 28, 32, 36, 40, " +
					"44, 48, 52, 56, 60, 64, 88 }, #vi:0 ]",
				getMarshallPlan("TwoTierStruct").toString());
		assertEquals(
				"MarshallPlan64[ 296, 472, " +
					"{ 16:296, 64:312, 72:320, 336:392, 384:408, 96:412, 144:428, 168:432, " +
					"216:448, 240:452, 288:468 }, " +
						// tts1
					"{ 0, 4, 8, 12, 16, 296, 300, 304, 308, 24, 28, 32, 36, 40, " +
						"44, 48, 52, 56, 60, 64, 312, " +
						// ttp
					"72, 320, 324, 328, 332, 336, 392, 396, 400, 404, 344, 348, "+
						"352, 356, 360, 364, 368, 372, 376, 380, 384, 408, " +
						// tts2
					"80, 84, 88, 92, 96, 412, 416, 420, 424, 104, 108, 112, 116, 120, " +
						"124, 128, 132, 136, 140, 144, 428, " +
						// tts3[0]
					"152, 156, 160, 164, 168, 432, 436, 440, 444, 176, 180, 184, " +
						"188, 192, 196, 200, 204, 208, 212, 216, 448, " +
						// tts3[1]
					"224, 228, 232, 236, 240, 452, 456, 460, 464, 248, 252, 256, " +
						"260, 264, 268, 272, 276, 280, 284, 288, 468 " +
					"}, #vi:0 ]",
				getMarshallPlan("ThreeTierStruct").toString());
		// StringStruct1 and StringStruct2 are very useful structures for
		// testing 64-bit alignment because they make such inefficient use of
		// space on x64 (lots of padding).
		assertEquals("MarshallPlan64[ 24, 24, { 8:24 }, { 0, 2, 8, 16 }, #vi:1 ]",
				getMarshallPlan("StringStruct1").toString());
		assertEquals(
				"MarshallPlan64[ 64, 64, { 8:64, 32:65, 56:66 }, " +
				"{ 0, 2, 8, 16, 24, 26, 32, 40, 48, 56 }, #vi:3 ]",
				getMarshallPlan("StringStruct2").toString());
		assertEquals(
				"MarshallPlan64[ 80, 144, { 16:144, 40:145, 64:146, 72:80, 88:147, 112:148, 136:149 }, " +
					"{ 0, 8, 10, 16, 24, 32, 34, 40, 48, 56, 64, 72, " +
					"80, 82, 88, 96, 104, 106, 112, 120, 128, 136 }, #vi:6 ]",
				getMarshallPlan("StringStruct3").toString());
		assertEquals(
				"MarshallPlan64[ 8, 8, { }, { 0, 1, 2, 4 }, #vi:0 ]",
				getMarshallPlan("Packed_Int8Int8Int16Int32").toString());
		assertEquals(
				"MarshallPlan64[ 48, 96, { 16:96, 24:97, 40:48, 64:98, 72:99 }, " +
					"{ 0, 1, 2, 4, 8, 9, 10, 12, 16, 24, 32, 40, " +
						"48, 49, 50, 52, 56, 57, 58, 60, 64, 72, 80, 88 }, #vi:4 ]",
				getMarshallPlan("Recursive_StringSum1").toString());
	}

}
