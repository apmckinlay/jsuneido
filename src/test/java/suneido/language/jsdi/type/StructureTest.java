/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import static suneido.util.testing.Throwing.assertThrew;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.SimpleContext;
import suneido.language.jsdi._64BitIssue;
import suneido.util.testing.Assumption;

/**
 * Test for {@link Structure}.
 *
 * @author Victor Schappert
 * @since 20130703
 * @see suneido.language.ParseAndCompileStructTest
 * @see suneido.language.jsdi.abi.x86.StructureX86Test
 */
@DllInterface
public class StructureTest {

	@BeforeClass
	@_64BitIssue // This should be relaxed to jvmIsOnWindows()
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
	}

	private static final String[] NAMED_TYPES = {
		"RECT", "struct { int32 left; int32 top; int32 right; int32 bottom; }",
		"POINT", "struct { int32 x; int32 y; }",
		"TwoTierStruct",
			"struct\n" +
				"\t{\n" +
				"\tPOINT[2]  pts ;\n" +
				"\tRECT *    prcOptional ;\n" +
				"\tint32[10] extra ;\n" +
				"\tint32 *   pLong ;\n" +
				"\t}",
		"ThreeTierStruct",
			"struct\n" +
				"\t{\n" +
				"\tTwoTierStruct[1] tts1\n" +
				"\tTwoTierStruct *  ttp\n" +
				"\tTwoTierStruct    tts2\n" +
				"\tTwoTierStruct[2] tts3\n" +
				"}\n",
		"Packed_CharCharShortLong", "struct { int8 a; int8 b; int16 c; int32 d; }",
		"Recursive_StringSum1",
			"struct { Packed_CharCharShortLong[2] x; string str; buffer buf; int32 len; Recursive_StringSum0 * inner }",
		"Recursive_StringSum0",
			"struct { Packed_CharCharShortLong[2] x; string str; buffer buf; int32 len; pointer setToZero }",
		"CycleA", "struct { CycleB cycleB }",
		"CycleB", "struct { CycleA cycleA }",
		"SelfRef", "struct { SelfRef selfRef }",
		"SelfRef2", "struct { SelfRef2 * selfRef }",
		"SelfRef3", "struct { SelfRef3 * selfRef }",
		"StringStruct1", "struct { int16 a; int16 b; string c; string[4] d; }",
		"StringStruct2", "struct { StringStruct1[2] a; buffer[8] b; buffer c; }",
		"StringStruct3", "struct { int32 a; StringStruct2 b; StringStruct2 * c }",
	};
	private static ContextLayered CONTEXT = new SimpleContext(NAMED_TYPES);

	public static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	public static Structure getAndResolve(String name) {
		final Structure s = (Structure)CONTEXT.get(name);
		s.resolve(0);
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

	// NOTE: tests for Structure() -- i.e. Structure.call1(Object) -- are in
	//       DllTest
}
