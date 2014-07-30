/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi._64BitIssue;
import suneido.jsdi.type.Structure;
import suneido.jsdi.type.StructureTest;
import suneido.util.testing.Assumption;

/**
 * x86-specific tests for {@link Structure}.
 *
 * @author Victor Schappert
 * @since 20140719
 * @see suneido.jsdi.type.StructureTest
 */
@DllInterface
public class StructureTestX86 {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
	}

	// THIS IS WHAT ThreeTierStruct's MARSHALLED REPRESENTATION LOOKS LIKE on
	// x86:
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

	private static MarshallPlanX86 getMarshallPlan(String name) {
		Structure struct = StructureTest.getAndResolve(name);
		MarshallPlanBuilderX86 builder = new MarshallPlanBuilderX86(
				struct.getVariableIndirectCount(), false);
		struct.addToPlan(builder, false);
		return (MarshallPlanX86)builder.makeMarshallPlan();
	}

	public static Object eval(String src) {
		return StructureTest.eval(src);
	}

	@Test
	@_64BitIssue // TODO: Make AMD64 test for this
	public void testSize() {
		assertEquals(16, eval("RECT.Size()"));
		assertEquals( 8, eval("POINT.Size()"));
		assertEquals(64, eval("TwoTierStruct.Size()"));
		assertEquals(12, eval("StringStruct1.Size()"));
		assertEquals(36, eval("StringStruct2.Size()"));
		assertEquals(44, eval("StringStruct3.Size()"));
	}


	@Test
	@_64BitIssue // TODO: Make AMD64 test for this
	public void testCallOnObject() {
		assertEquals(new Buffer(4 * 4, ""), eval("RECT(Object())"));
		assertEquals(new Buffer(new byte[] { (byte) 0xff, (byte) 0xee,
				(byte) 0xdd, (byte) 0x0c, (byte) 0xbb, (byte) 0xaa,
				(byte) 0x99, 0x08 }, 0, 8),
				eval("POINT(#(x: 0x0cddeeff, y: 0x0899aabb))"));
		assertEquals(
				new Buffer(new byte[] { (byte)'x', 0, (byte)'y', 0, 1, 0, 0, 0, 0, 0, 0, 0 }, 0, 12),
				eval("(struct { buffer[2] a; string[2] b; Packed_CharCharShortLong c })(#(a: x, b: y, c: #(a: 1)))")
		);
	}

	@Test
	@_64BitIssue // TODO: Make AMD64 test for this
	public void testCallOnBuffer() {
		assertEquals(eval("#(left:0,top:0,right:0,bottom:0)"),
				eval("RECT(Buffer(RECT.Size(), ''))"));
		assertEquals(
				eval("#(x: 1, y: 1)"),
				eval("POINT(Buffer(POINT.Size(), '\\x01\\x00\\x00\\x00\\x01'))"));
		assertEquals(
				eval("#(a:1, b:-1, c:2, d:-2)"),
				eval("Packed_CharCharShortLong(Packed_CharCharShortLong(Object(a:1, b:-1, c:2, d:-2)))")
		);
	}

	@Test
	@_64BitIssue // TODO: Make AMD64 test for this
	public void testMarshallPlan() {
		assertEquals("MarshallPlanX86[ 16, 16, { }, { 0, 4, 8, 12 }, #vi:0 ]",
				getMarshallPlan("RECT").toString());
		assertEquals("MarshallPlanX86[ 8, 8, { }, { 0, 4 }, #vi:0 ]",
				getMarshallPlan("POINT").toString());
		assertEquals(
				"MarshallPlanX86[ 64, 88, { 16:64, 60:80 }, " +
					"{ 0, 4, 8, 12, 16, 64, 68, 72, 76, 20, 24, 28, 32, 36, " +
					"40, 44, 48, 52, 56, 60, 80 }, #vi:0 ]",
				getMarshallPlan("TwoTierStruct").toString());
		assertEquals(
				"MarshallPlanX86[ 260, 424, " +
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
		assertEquals("MarshallPlanX86[ 12, 16, { 4:16 }, { 0, 2, 4, 8 }, #vi:1 ]",
				getMarshallPlan("StringStruct1").toString());
		assertEquals(
				"MarshallPlanX86[ 36, 40, { 4:40, 16:41, 32:42 }, " +
				"{ 0, 2, 4, 8, 12, 14, 16, 20, 24, 32 }, #vi:3 ]",
				getMarshallPlan("StringStruct2").toString());
		assertEquals(
				"MarshallPlanX86[ 44, 80, { 8:80, 20:81, 36:82, 40:44, 48:83, 60:84, 76:85 }, " +
					"{ 0, 4, 6, 8, 12, 16, 18, 20, 24, 28, 36, 40, " +
					"44, 46, 48, 52, 56, 58, 60, 64, 68, 76 }, #vi:6 ]",
				getMarshallPlan("StringStruct3").toString());
		assertEquals(
				"MarshallPlanX86[ 8, 8, { }, { 0, 1, 2, 4 }, #vi:0 ]",
				getMarshallPlan("Packed_CharCharShortLong").toString());
		assertEquals(
				"MarshallPlanX86[ 32, 64, { 16:64, 20:65, 28:32, 48:66, 52:67 }, " +
					"{ 0, 1, 2, 4, 8, 9, 10, 12, 16, 20, 24, 28, " +
						"32, 33, 34, 36, 40, 41, 42, 44, 48, 52, 56, 60 }, #vi:4 ]",
				getMarshallPlan("Recursive_StringSum1").toString());
	}

}
