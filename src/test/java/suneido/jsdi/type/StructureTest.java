/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.JSDIException;
import suneido.jsdi.SimpleContext;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.Numbers;
import suneido.util.testing.Assumption;

/**
 * Test for {@link Structure}.
 *
 * @author Victor Schappert
 * @since 20130703
 * @see suneido.language.ParseAndCompileStructTest
 * @see suneido.jsdi.abi.x86.StructureTestX86
 * @see suneido.jsdi.abi.amd64.StructureTest64
 */
@DllInterface
@RunWith(Parameterized.class)
public class StructureTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIsOnWindows();
	}

	@Parameters
	public static Collection<Object[]> isFast() {
		return Arrays.asList(new Object[][] { { Boolean.FALSE }, { Boolean.TRUE } }); 
	}

	public StructureTest(boolean isFast) {
		JSDI.getInstance().setFastMode(isFast);
	}

	private static final String[] NAMED_TYPES = {
		"INT32", "struct { int32 value }",
		"RECT", "struct { int32 left; int32 top; int32 right; int32 bottom; }",
		"POINT", "struct { int32 x; int32 y; }",
		"TwoTierStruct",
			"struct\n" +
				"\t{\n" +
				"\tPOINT[2]  pts ;\n" +
				"\tRECT *    prcOptional ;\n" +
				"\tint32[10] extra ;\n" +
				"\tINT32 *   pLong ;\n" +
				"\t}",
		"ThreeTierStruct",
			"struct\n" +
				"\t{\n" +
				"\tTwoTierStruct[1] tts1\n" +
				"\tTwoTierStruct *  ttp\n" +
				"\tTwoTierStruct    tts2\n" +
				"\tTwoTierStruct[2] tts3\n" +
				"}\n",
		"Packed_Int8Int8Int16Int32", "struct { int8 a; int8 b; int16 c; int32 d; }",
		"Recursive_StringSum2",
			"struct { Packed_Int8Int8Int16Int32[2] x; string str; buffer buf; int32 len; Recursive_StringSum1 * inner }",
		"Recursive_StringSum1",
			"struct { Packed_Int8Int8Int16Int32[2] x; string str; buffer buf; int32 len; Recursive_StringSum0 * inner }",
		"Recursive_StringSum0",
			"struct { Packed_Int8Int8Int16Int32[2] x; string str; buffer buf; int32 len; pointer inner }",
		"Recursive_Int8Int8Int16Int32_2",
			"struct { Packed_Int8Int8Int16Int32 x; Recursive_Int8Int8Int16Int32_1 * inner; }",
		"Recursive_Int8Int8Int16Int32_1",
			"struct { Packed_Int8Int8Int16Int32 x; Recursive_Int8Int8Int16Int32_0 * inner; }",
		"Recursive_Int8Int8Int16Int32_0",
			"struct { Packed_Int8Int8Int16Int32 x; pointer inner; }",
		"CycleA", "struct { CycleB cycleB }",
		"CycleB", "struct { CycleA cycleA }",
		"SelfRef", "struct { SelfRef selfRef }",
		"SelfRef2", "struct { SelfRef2 * selfRef }",
		"SelfRef3", "struct { SelfRef3 * selfRef }",
		"StringStruct1", "struct { int16 a; int16 b; string c; string[4] d; }",
		"StringStruct2", "struct { StringStruct1[2] a; buffer[8] b; buffer c; }",
		"StringStruct3", "struct { int32 a; StringStruct2 b; StringStruct2 * c }",
		"TestReturnStatic_Packed_Int8Int8Int16Int32",
			"dll pointer jsdi:TestReturnStatic_Packed_Int8Int8Int16Int32(Packed_Int8Int8Int16Int32 * ptr)",
		"TestReturnStatic_Recursive_Int8Int8Int16Int32",
			"dll pointer jsdi:TestReturnStatic_Recursive_Int8Int8Int16Int32(Recursive_Int8Int8Int16Int32_2 * ptr)",
		"TestReturnStatic_Recursive_StringSum",
			"dll pointer jsdi:TestReturnStatic_Recursive_StringSum(Recursive_StringSum2 * ptr)",
	};
	private static ContextLayered CONTEXT = new SimpleContext(NAMED_TYPES);

	public static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	private static Structure struct(String src) {
		return (Structure)eval(src);
	}

	public static Structure getAndResolve(String name) {
		final Structure s = (Structure)CONTEXT.get(name);
		s.bind(0);
		return s;
	}

	@Test
	public void testCycle() {
		assertThrew(() -> { eval("CycleA.Size()"); },
				JSDIException.class,
				".*cycle.*"
		);
	}

	@Test
	public void testSelfRef() {
		for (final String selfRef : new String[] { "SelfRef", "SelfRef2", "SelfRef3" }) {
			assertThrew(() -> { eval(selfRef + ".Size()"); },
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
				eval("(struct { buffer[2] a; string[2] b; Packed_Int8Int8Int16Int32 c })(#(a: x, b: y, c: #(a: 1)))")
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
				eval("Packed_Int8Int8Int16Int32(Packed_Int8Int8Int16Int32(Object(a:1, b:-1, c:2, d:-2)))")
		);
	}

	@Test
	public void testCallOnObject_NoIndirect() {
		assertThrew(() -> { eval("ThreeTierStruct(Object())"); },
				JSDIException.class, "does not support.*pointers");
	}

	@Test
	public void testCallOnBuffer_NoIndirect() {
		assertThrew(() -> { eval("ThreeTierStruct(Buffer(100, ''))"); },
				JSDIException.class, "does not support.*pointers");
	}

	@Test
	public void testCallOnObject_NoVariableIndirect() {
		for (String s : new String[] { "string", "buffer", "resource" } ) {
			final String code = String.format("(struct { %s x })(Object())", s);
			assertThrew(() -> { eval(code); },
					JSDIException.class, "does not support.*pointers");
		}
		assertThrew(() -> { eval("StringStruct2(Object(a: Object()))"); },
				JSDIException.class, "does not support.*pointers");
	}

	@Test
	public void testCallOnBuffer_NoVariableIndirect() {
		for (String s : new String[] { "string", "buffer", "resource" } ) {
			final String code = String.format("(struct { %s x })(Buffer(100, ''))", s);
			assertThrew(() -> { eval(code); },
					JSDIException.class, "does not support.*pointers");
		}
		assertThrew(() -> { eval("StringStruct2(Buffer(100, ''))"); },
				JSDIException.class, "does not support.*pointers");
	}

	@Test
	public void testCallOnString() {
		for (final String s : new String[] { "(struct { short a })", "RECT",
				"Recursive_StringSum1" }) {
			assertThrew(() -> { eval(String.format("%s('\\x00'.Repeat(200))", s)); },
					JSDIException.class, "does not support Struct\\(string\\)");
		}
	}

	//
	// TESTS for copying out Structures
	//

	@Test
	public void testCopyOutNull() {
		for (String structName : new String[] { "Packed_Int8Int8Int16Int32",
				"Recursive_Int8Int8Int16Int32_2", "Recursive_Int8Int8Int16Int32_1",
				"Recursive_Int8Int8Int16Int32_0", "Recursive_StringSum2",
				"Recursive_StringSum1", "Recursive_StringSum0" }) {
			for (String nullValue : new String[] { "false", "0" }) {
				assertEquals(Boolean.FALSE,
						eval(String.format("%s(%s)", structName, nullValue)));
			}
		}
	}

	@Test
	public void testCopyOutDirect() {
		Object addr = eval("TestReturnStatic_Packed_Int8Int8Int16Int32(false)");
		assertFalse(Numbers.isZero(addr));
		assertEquals(addr, eval("TestReturnStatic_Packed_Int8Int8Int16Int32(Object(a: 5, d: -1))"));
		String code = String.format("Packed_Int8Int8Int16Int32(%d)", addr);
		assertEquals(eval("#(a: 5, b: 0, c: 0, d: -1)"), eval(code));
		assertEquals(addr, eval("TestReturnStatic_Packed_Int8Int8Int16Int32(Object(a: 0, b: 5, c: -1))"));
		assertEquals(
				eval("#(a: 0, b: 5, c: -1, d: 0)"),
				struct("Packed_Int8Int8Int16Int32").call1(addr));
	}

	@Test
	public void testCopyOutIndirect() {
		Object addr = eval("TestReturnStatic_Recursive_Int8Int8Int16Int32(false)");
		assertFalse(Numbers.isZero(addr));
		assertEquals(addr, eval("TestReturnStatic_Recursive_Int8Int8Int16Int32(Object(x: Object(a: 5, d: -1)))"));
		final String code0 = String.format("Recursive_Int8Int8Int16Int32_0(%d)", addr); // doesn't engage indirect copy out
		assertEquals(eval("#(x: #(a: 5, b: 0, c: 0, d: -1), inner: 0)"), eval(code0));
		final String code1 = String.format("Recursive_Int8Int8Int16Int32_1(%d)", addr); // indirect copy out
		assertEquals(eval("#(x: #(a: 5, b: 0, c: 0, d: -1), inner: false)"), eval(code1));
		final String code2 = String.format("Recursive_Int8Int8Int16Int32_2(%d)", addr); // indirect copy out
		assertEquals(eval("#(x: #(a: 5, b: 0, c: 0, d: -1), inner: false)"), eval(code2));
		assertEquals(
				addr,
				eval("TestReturnStatic_Recursive_Int8Int8Int16Int32(" +
						"Object(inner: Object(inner: Object(x: Object(c: 5555)))))"
		));
		assertEquals(
				eval("#(x: #(a:0,b:0,c:0,d:0), " +
						"inner: #(x: #(a:0,b:0,c:0,d:0), " +
							"inner: #(x: #(a:0,b:0,c:5555,d:0), " +
							"inner: 0)" +
					"))"),
				eval(code2)
		);
	}

	@Test
	public void testCopyOutVariableIndirect() {
		Object addr = eval("TestReturnStatic_Recursive_StringSum(0)");
		String array = "Object(Object(a: 10, b: 9, c: 8, d: 7), Object(a: 6, b: 5, c: 4, d: 3))";
		String array0 = "#(#(a:0,b:0,c:0,d:0),#(a:0,b:0,c:0,d:0))";
		String hello = "'Ol� mundo'";
		assertFalse(Numbers.isZero(addr));
		Object value = eval(String.format("TestReturnStatic_Recursive_StringSum(Object(x: %s, str: %s))", array, hello));
		assertEquals(addr, value);
		for (String s : new String[] { "Recursive_StringSum2", "Recursive_StringSum1" }) {
			assertEquals(
					eval(String.format("Object(x: %s, str: %s, buf: false, len: 0, inner: false)", array, hello)),
					eval(String.format("%s(%d)", s, addr))
			);
		}
		value = eval(String.format("TestReturnStatic_Recursive_StringSum(Object(x: %s, str: %s, inner: Object()))", array, hello));
		assertEquals(addr, value);
		for (String s : new String[] { "Recursive_StringSum2/false", "Recursive_StringSum1/0" }) {
			String[] split = s.split("/");
			assertEquals(
					eval(String.format("Object(x: %s, str: %s, buf: false, len: 0, " +
							"inner: #(x: %s, str: false, buf: false, len: 0, inner: %s))", array, hello, array0, split[1])),
					eval(String.format("%s(%d)", split[0], addr))
			);
		}
		value = eval(String.format(
					"TestReturnStatic_Recursive_StringSum(" +
							"Object(x: %s, str: %s, " +
							"inner: Object(str: 'inner1', " +
								"inner: Object(x: %s, str: 'inner2'))))", array, hello, array));
		assertEquals(addr, value);
		assertEquals(
				eval(String.format("Object(x: %s, str: %s, buf: false, len: 0, " +
						"inner: Object(x: %s, str: 'inner1', buf: false, len: 0, " +
						"inner: Object(x: %s, str: 'inner2', buf: false, len: 0, inner: 0)))",
						array, hello, array0, array)),
				eval(String.format("Recursive_StringSum2(%d)", addr)));
	}

	@Test
	public void testCopyOutDirectWin32Exception() {
		assertThrew(() -> {
			eval("Packed_Int8Int8Int16Int32(1)");
		}, JSDIException.class, "win32 exception: ACCESS_VIOLATION");
	}

	@Test
	public void testCopyOutIndirectWin32Exception() {
		assertThrew(() -> {
			eval("Recursive_Int8Int8Int16Int32_2(1)");
		}, JSDIException.class, "win32 exception: ACCESS_VIOLATION");
	}

	@Test
	public void testCopyOutVariableIndirectWin32Exception() {
		assertThrew(() -> {
			eval("Recursive_StringSum2(1)");
		}, JSDIException.class, "win32 exception: ACCESS_VIOLATION");
	}

}
