/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import static org.junit.Assert.assertEquals;
import static suneido.util.testing.Throwing.assertThrew;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.jsdi.*;
import suneido.util.testing.Assumption;

/**
 * Test for {@link Structure}.
 *
 * @author Victor Schappert
 * @since 20130703
 * @see suneido.language.ParseAndCompileStructTest
 */
@DllInterface
public class StructureTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
		CONTEXT = new SimpleContext(NAMED_TYPES);
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_TYPES = {
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
		"Packed_CharCharShortLong", "struct { char a; char b; short c; long d; }",
		"Recursive_StringSum1",
			"struct { Packed_CharCharShortLong[2] x; string str; buffer buf; long len; Recursive_StringSum0 * inner }",
		"Recursive_StringSum0",
			"struct { Packed_CharCharShortLong[2] x; string str; buffer buf; long len; long setToZero }",
		"CycleA", "struct { CycleB cycleB }",
		"CycleB", "struct { CycleA cycleA }",
		"SelfRef", "struct { SelfRef selfRef }",
		"SelfRef2", "struct { SelfRef2 * selfRef }",
		"SelfRef3", "struct { SelfRef3 * selfRef }",
		"StringStruct1", "struct { short a; short b; string c; string[4] d; }",
		"StringStruct2", "struct { StringStruct1[2] a; buffer[8] b; buffer c; }",
		"StringStruct3", "struct { long a; StringStruct2 b; StringStruct2 * c }",
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
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
			struct.getSizeDirectWholeWords(),
			struct.getSizeIndirect(),
			struct.getVariableIndirectCount(),
			false
		);
		struct.addToPlan(builder, false);
		return builder.makeMarshallPlan();
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
		assertEquals("MarshallPlan[ 12, 0, { 4:12 }, { 0, 2, 4, 8 }, #vi:1 ]",
				getMarshallPlan("StringStruct1").toString());
		assertEquals(
				"MarshallPlan[ 36, 0, { 4:36, 16:37, 32:38 }, " +
				"{ 0, 2, 4, 8, 12, 14, 16, 20, 24, 32 }, #vi:3 ]",
				getMarshallPlan("StringStruct2").toString());
		assertEquals(
				"MarshallPlan[ 44, 36, { 8:80, 20:81, 36:82, 40:44, 48:83, 60:84, 76:85 }, " +
					"{ 0, 4, 6, 8, 12, 16, 18, 20, 24, 28, 36, 40, " +
					"44, 46, 48, 52, 56, 58, 60, 64, 68, 76 }, #vi:6 ]",
				getMarshallPlan("StringStruct3").toString());
		assertEquals(
				"MarshallPlan[ 8, 0, { }, { 0, 1, 2, 4 }, #vi:0 ]",
				getMarshallPlan("Packed_CharCharShortLong").toString());
		assertEquals(
				"MarshallPlan[ 32, 32, { 16:64, 20:65, 28:32, 48:66, 52:67 }, " +
					"{ 0, 1, 2, 4, 8, 9, 10, 12, 16, 20, 24, 28, " +
						"32, 33, 34, 36, 40, 41, 42, 44, 48, 52, 56, 60 }, #vi:4 ]",
				getMarshallPlan("Recursive_StringSum1").toString());
	}

	@Test
	public void testCycle() {
		assertThrew(
				new Runnable() { @Override
				public void run() { eval("CycleA.Size()"); } },
				JSDIException.class,
				".*cycle.*"
		);
	}

	@Test
	public void testSelfRef() {
		for (final String selfRef : new String[] { "SelfRef", "SelfRef2", "SelfRef3" }) {
			assertThrew(
					new Runnable() { @Override
					public void run() { eval(selfRef + ".Size()"); } },
					JSDIException.class,
					".*cycle.*"
			);
		}
	}

	@Test
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
	public void testCallOnObject_NoIndirect() {
		assertThrew(new Runnable() {
			@Override
			public void run() {
				eval("ThreeTierStruct(Object())");
			}
		}, JSDIException.class, "does not support.*pointers");
	}

	@Test
	public void testCallOnBuffer_NoIndirect() {
		assertThrew(new Runnable() {
			@Override
			public void run() {
				eval("ThreeTierStruct(Buffer(100, ''))");
			}
		}, JSDIException.class, "does not support.*pointers");
	}

	@Test
	public void testCallOnObject_NoVariableIndirect() {
		for (String s : new String[] { "string", "buffer", "resource" } ) {
			final String code = String.format("(struct { %s x })(Object())", s);
			assertThrew(new Runnable() {
				@Override
				public void run() {
					eval(code);
				}
			}, JSDIException.class, "does not support.*pointers");
		}
		assertThrew(new Runnable() {
			@Override
			public void run() {
				eval("StringStruct2(Object(a: Object()))");
			}
		}, JSDIException.class, "does not support.*pointers");
	}

	@Test
	public void testCallOnBuffer_NoVariableIndirect() {
		for (String s : new String[] { "string", "buffer", "resource" } ) {
			final String code = String.format("(struct { %s x })(Buffer(100, ''))", s);
			assertThrew(new Runnable() {
				@Override
				public void run() {
					eval(code);
				}
			}, JSDIException.class, "does not support.*pointers");
		}
		assertThrew(new Runnable() {
			@Override
			public void run() {
				eval("StringStruct2(Buffer(100, ''))");
			}
		}, JSDIException.class, "does not support.*pointers");
	}

	@Test
	public void testCallOnString() {
		for (final String s : new String[] { "(struct { short a })", "RECT",
				"Recursive_StringSum1" }) {
			assertThrew(new Runnable() {
				@Override
				public void run() {
					eval(String.format("%s('\\x00'.Repeat(200))", s));
				}
			}, JSDIException.class, "does not support Struct\\(string\\)");
		}
	}

	// NOTE: tests for Structure() -- i.e. Structure.call1(Object) -- are in
	//       DllTest
	// TODO: tests for Modify()
}
