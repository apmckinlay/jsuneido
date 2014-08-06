/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import suneido.SuContainer;
import suneido.SuException;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.JSDIException;
import suneido.jsdi.SimpleContext;
import suneido.jsdi.ThunkManager;
import suneido.jsdi.type.Callback;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.util.testing.Assumption;

/**
 * Test for {@link Callback}.
 *
 * @author Victor Schappert
 * @since 20130807
 * @see suneido.language.ParseAndCompileCallbackTest
 */
@DllInterface
@RunWith(Parameterized.class)
public class CallbackTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIsOnWindows();
		CONTEXT = new SimpleContext(NAMED_TYPES);
	}

	@Parameters
	public static Collection<Object[]> isFast() {
		return Arrays.asList(new Object[][] { { Boolean.FALSE }, { Boolean.TRUE } }); 
	}

	public CallbackTest(boolean isFast) {
		JSDI.getInstance().setFastMode(isFast);
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_TYPES = {
		"TestCallback_Int32_1", "callback(int32 a)",
		"TestCallback_Int32_2", "callback(int32 a, int32 b)",
		"TestCallback_Mixed_6", "callback(double a, int8 b, float c, int16 d, float e, int64 f)",
		"TestCallback_Packed_Int8Int8Int16Int32", "callback(Packed_Int8Int8Int16Int32 a)",
		"TestCallback_Recursive_StringSum", "callback(Recursive_StringSum1_Callback * ptr)",
		"TestCallback_Recursive_StringSum_Bad", "callback(Recursive_StringSum1_Dll * ptr)",
		"Packed_Int8Int8Int16Int32", "struct { int8 a; int8 b; int16 c; int32 d; }",
		"Recursive_StringSum1_Dll",
			"struct\n" +
			"\t{\n" +
			"\tPacked_Int8Int8Int16Int32[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tint32 len\n" +
			"\tRecursive_StringSum0_Dll * inner\n" +
			"\t}",
		"Recursive_StringSum0_Dll",
			"struct\n" +
			"\t{\n" +
			"\tPacked_Int8Int8Int16Int32[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tint32 len\n" +
			"\tpointer inner\n" +
			"\t}",
		"Recursive_StringSum1_Callback",
			"struct\n" +
			"\t{\n" +
			"\tPacked_Int8Int8Int16Int32[2] x\n" +
			"\tstring str\n" +
			"\tstring buffer_\n" +
			"\tint32 len\n" +
			"\tRecursive_StringSum0_Callback * inner\n" +
			"\t}",
		"Recursive_StringSum0_Callback",
			"struct\n" +
			"\t{\n" +
			"\tPacked_Int8Int8Int16Int32[2] x\n" +
			"\tstring str\n" +
			"\tstring buffer_\n" +
			"\tint32 len\n" +
			"\tpointer inner\n" +
			"\t}",
		"TestInvokeCallback_Int32_1",
			"dll int32 jsdi:TestInvokeCallback_Int32_1(TestCallback_Int32_1 f, int32 a)",
		"TestInvokeCallback_Int32_1_2",
			"dll int32 jsdi:TestInvokeCallback_Int32_1_2(" +
					"TestCallback_Int32_1 f, int32 a, TestCallback_Int32_1 g, int32 b)",
		"TestInvokeCallback_Int32_2",
			"dll int32 jsdi:TestInvokeCallback_Int32_2(TestCallback_Int32_2 f, int32 a, int32 b)",
		"TestInvokeCallback_Mixed_6",
			"dll int32 jsdi:TestInvokeCallback_Mixed_6(TestCallback_Mixed_6 g, double a, int8 b, float c, int16 d, float e, int64 f)",
		"TestInvokeCallback_Packed_Int8Int8Int16Int32",
			"dll int32 jsdi:TestInvokeCallback_Packed_Int8Int8Int16Int32(" +
				"TestCallback_Packed_Int8Int8Int16Int32 f, Packed_Int8Int8Int16Int32 a)",
		"TestInvokeCallback_Recursive_StringSum",
			"dll int32 jsdi:TestInvokeCallback_Recursive_StringSum(" +
				"TestCallback_Recursive_StringSum f, Recursive_StringSum1_Dll * ptr)",
		"TestInvokeCallback_Recursive_StringSum_Bad",
			"dll int32 jsdi:TestInvokeCallback_Recursive_StringSum(" +
				"TestCallback_Recursive_StringSum_Bad f, Recursive_StringSum1_Dll * ptr)",
		"PCCSL", "function(a = 0, b = 0, c = 0, d = 0) { return Object(a: a, b: b, c: c, d: d) }",
		"RSS",
			"function(x0, x1, str = false, buffer_ = false, len = 0, inner = false)\n" +
			"\t{\n" +
			"\treturn Object(x: Object(x0, x1), str: str, buffer_: buffer_, len: len, inner: inner)\n" +
			"\t}",
		"SuTestSumString",
			"function(rss)\n" +
			"\t{\n" +
			"\tsum = Object(0)\n" +
			"\tvalueOf = { |x| Object?(x) ? x.a + x.b + x.c + x.d : x }\n" +
			"\tadd = { |x| if (false isnt rss[x]) sum[0] += valueOf(rss[x]) }\n" +
			"\tsum[0] += valueOf(rss.x[0])\n" +
			"\tsum[0] += valueOf(rss.x[1])\n" +
			"\tadd('str')\n" +
			"\tadd('buffer_')\n" +
			"\tif (Object?(rss.inner)) sum[0] += SuTestSumString(rss.inner)\n" +
			"\treturn sum[0]\n" +
			"\t}"
	};

	private static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	//
	// CLEAR ANY REMAINING CALLBACKS BEFORE EACH TEST
	//

	@Before
	public void clearCallbacks()
	{
		for (Object o : ThunkManager.Callbacks.Callbacks()) {
			ThunkManager.ClearCallback.ClearCallback(o);
		}
	}

	//
	// BASIC FUNCTIONALITY TESTS
	//

	@Test
	public void testCallbacksEmpty() {
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1Function() {
		assertEquals(
			eval("#(11, true)"),
			eval("Object(TestInvokeCallback_Int32_1( f = function(a) { return a }, 11), ClearCallback(f))")
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1Method() {
		assertEquals(
			eval("#(21, true)"),
			eval(
				"x = class { New(.value) { } Method(a, b = -1) { return .value + a - b } };" +
				"Object(TestInvokeCallback_Int32_1((y = new x(30)).Method, -10), ClearCallback(y.Method))"
			)
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1Call() {
		assertEquals(
			eval("Object(0x1 << 18, true)"),
			eval(
				"x = class { Call(y) { return y * y } } ; " +
				"Object(TestInvokeCallback_Int32_1(z = x(), 0x1 << 9), ClearCallback(z))"
			)
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1CallClass() {
		assertEquals(
			eval("Object(0x1 << 16, true, false)"),
			eval(
				"x = class { CallClass(y) { return y / 2 } } ; " +
				"Object(TestInvokeCallback_Int32_1(x, 0x1 << 17), ClearCallback(x), ClearCallback(x))"
			)
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1Block() {
		assertEquals(
			eval("#(31, true)"),
			eval("Object(TestInvokeCallback_Int32_1(x = { |a| a }, 31), ClearCallback(x))")
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1Closure() {
		assertEquals(
			eval("#(601, true)"),
			eval(
				"a = Object(f: function() { return 300 })\n" +
				"Object(TestInvokeCallback_Int32_1(x = { |b| (a.f)() + b }, 301), ClearCallback(x))"
			)
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue2Function() {
		assertEquals(-1, eval("TestInvokeCallback_Int32_2( function(a, b) { return a + b }, 1000, -1001)"));
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testBasicValue2Method() {
		assertEquals(
			12,
			eval(
				"x = class { Method(a = 1, b = 2) { return a * b } } \n" +
				"TestInvokeCallback_Int32_2(x().Method, -3, -4)"
			)
		);
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testBasicValue2Call() {
		assertEquals(
			312,
			eval(
				"x = class { Call(a, b = 1, c = 2) { return a * b * c } } ; " +
				"TestInvokeCallback_Int32_2(x(), 12, 13)"
			)
		);
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testBasicValue2CallClass() {
		assertEquals(
			999,
			eval(
				"x = class { CallClass(@aa) { sum = 0; for (a in aa) { sum += a }; return sum } } ; " +
				"TestInvokeCallback_Int32_2(x, -1, 1000)"
			)
		);
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testBasicValue2Block() {
		assertEquals(
			0x19820207,
			eval("TestInvokeCallback_Int32_2( { |a,b| a | b }, 0x19820000, 0x207)")
		);
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testBasicValue2Closure() {
		assertEquals(
			11,
			eval(
				"x = Object(f: function(a) { return a * a * a * a })\n" +
				"y = { |a,b| (x.f)(a) + b }\n" +
				"x.f = function(a) { return a * a * a }\n" +
				"TestInvokeCallback_Int32_2(y, 2, 3)"
			)
		);
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testBasicValue6Function() {
		assertEquals(
			38,
			eval(
				"TestInvokeCallback_Mixed_6(" +
						"function(a, b, c, d, e, f) { return a + b + c + d + e + f + 1 }," +
						"97, 2, -60, -9, 10, -3)"
			)
		);
	}

	@Test
	public void testStructValueFunction() {
		// The reason I modified this test to compare objects rather than just
		// the return value of TestInvokeCallback_Packed_Int8Int8Int16Int32(...)
		// is that there was a long-standing bug in Callback.marshallOut(...)
		// where it failed to advance the marshaller position so that any
		// values or parameters subsequently marshalled out would be corrupt.
		// This test now also regression-tests the fix for that bug, since the
		// value 'x' marshalled out should equal the value marshalled in.
		assertEquals(
			eval("#(-10, #(a: -1, b: -2, c: -3, d: -4))"),
			eval(
				"Object(" +
					"TestInvokeCallback_Packed_Int8Int8Int16Int32(" +
						"function(x) { return x.a + x.b + x.c + x.d }," +
						"x = PCCSL(-1, -2, -3, -4)" +
					"), x" +
				")"
			)
		);
	}

	@Test
	public void testStructValueMethod() {
		assertEquals(
			Integer.MIN_VALUE + 7,
			eval(
				"c = class { DoStuff(a) { return a.a - a.b - a.c - a.d } }\n" +
				"TestInvokeCallback_Packed_Int8Int8Int16Int32(" +
					"(new c).DoStuff," +
					"PCCSL(1, -2, -3, " + -(Integer.MIN_VALUE + 1) + ")" +
				")"
			)
		);
	}

	@Test
	public void testViFunction_NoStrings() {
		assertEquals(
			26,
			eval("TestInvokeCallback_Recursive_StringSum(" +
					"SuTestSumString," +
					"RSS(PCCSL(), PCCSL(5, 6, 7, 8))" +
				")"
			)
		);
	}

	@Test
	public void testViMethod_OneString() {
		assertEquals(
			26,
			eval(
				"x = class { Method(y) { return SuTestSumString(y) } }\n" +
				"TestInvokeCallback_Recursive_StringSum(" +
					"(new x()).Method," +
					"RSS(PCCSL(4, 3, 2, 1), PCCSL(-1, 0, 1, 2), '14')" +
				")"
			)
		);
	}

	@Test
	public void testViCall_TwoStrings() {
		// NOTE: You need to send a buffer with one more item of space than the
		//       string stored in it so that it is zero-terminated, because it
		//       is being out-marshalled as the 'string' type.
		assertEquals(
			26,
			eval(
				"x = class { Call(y) { return SuTestSumString(y) } }\n" +
				"TestInvokeCallback_Recursive_StringSum(" +
					"new x()," +
					"RSS(PCCSL(4, 3, 2, 1), PCCSL(-1, 0, 1, 2), '1414', Buffer(6, '-1400'), 6)" +
				")"
			)
		);
	}

	@Test
	public void testAtParam() {
		assertEquals(
			3,
			eval("TestInvokeCallback_Int32_2(function(@a) { s = 0; for (b in a) s += b; return s; }, 1, 2)")
		);
	}

	//
	// TYPES THAT CAN'T BE MARSHALLED OUT OF CALLBACK
	//

	@Test
	public void testCantUnmarshallBuffer() {
		assertThrew(() -> {
				eval("TestInvokeCallback_Recursive_StringSum_Bad(" +
						"SuTestSumString," +
						"RSS(PCCSL(), PCCSL(5, 6, 7, 8))" +
						")");
				},
			JSDIException.class,
			"buffer may not directly or indirectly be passed to a callback"
		);
	}

	//
	// PARAMETER MISMATCHES
	//

	@Test
	public void testExceptionParamMismatch() {
		assertThrew(() -> {
			eval("TestInvokeCallback_Int32_1(function(x) { x.Add('y') }, 2)");
			}, SuException.class, "method not found:.*Add");
	}

	//
	// BAD RETURN VALUES
	//

	@Test
	public void testExceptionReturnValueNone_Basic() {
		assertEquals(0, eval("TestInvokeCallback_Int32_1(function(@x) { }, 1)"));
	}

	@Test
	public void testExceptionReturnValueNone_Vi() {
		assertEquals(0, eval("TestInvokeCallback_Recursive_StringSum(function(x) { return }, Object())"));
	}

	@Test
	public void testExceptionReturnValueNotConvertible() {
		assertThrew(() -> {
			eval("TestInvokeCallback_Int32_1(function(a) { return 'Michalek/Spezza/Ryan' }, 2)");
			}, JSDIException.class, "can't convert");
	}

	//
	// EXCEPTION PROPAGATION
	//

	@Test
	public void testExceptionThrowBasic() {
		// Straightforward test -- make sure it works as a one-off
		assertThrew(() -> {
			eval("TestInvokeCallback_Int32_1(function(a) { throw a }, 444)");
			}, SuException.class, "444");
		// In case exception throwing causes subtle problems which might be
		// detected later, run a stress test...
		for (int k = 0; k < 100; ++k) {
			int x = k;
			int y = k + 1;
			String expected = String.format("#\\(%d, %d\\)", x, y); // escape parentheses for regex
			final String code = String
					.format("TestInvokeCallback_Int32_2(function(@a) { throw Display(a) }, %d, %d)",
							x, y);
			assertThrew(() -> {
				eval(code);
			}, SuException.class, expected);
		}
	}

	@Test
	public void testExceptionThrowVi() {
		assertThrew(() -> {
			eval("TestInvokeCallback_Recursive_StringSum(" +
					"{ |x| throw SuTestSumString(x) }, " +
					"RSS(PCCSL(102, -1, 0, -1), PCCSL(1, 1, 1, 97), '55', inner: RSS(PCCSL(50, 50, 50, 50), PCCSL(0, 0, 25, 25), '50'))" +
					")");
			},
			SuException.class, "555");
	}

	@Test
	public void testExceptionReEntrantBasic() {
		final String code =
			"f = function(n)\n" +
			"\t{\n" +
			"\tif 0 < n\n" +
			"\t\t{\n" +
			"\t\ttry\n" +
			"\t\t\tTestInvokeCallback_Int32_1(this, n - 1)\n" +
			"\t\tcatch (x)\n" +
			"\t\t\tthrow x + 1\n" +
			"\t\t}\n" +
			"\telse throw 0\n" +
			"\t}\n" +
			"f(20)";
		assertThrew(() -> { eval(code); }, SuException.class, "20");
	}

	@Test
	public void testExceptionDouble() {
		// The purpose of this test is to ensure that there are no JNI warnings
		// when the following sequence of calls is done:
		//
		//     call a 'dll', F()
		//         F() calls callback x()
		//             x() throws (in Suneido code, or Java/JNI code)
		//         F() doesn't know about the exception thrown by x() and wants
		//             to call a second callback, y(), before F() returns back
		//             to Suneido.
		assertThrew(() -> {
			eval("TestInvokeCallback_Int32_1_2({|x| throw 'one' $ x }, 1, {|y| }, 2)");
			}, SuException.class, "one1");
		assertThrew(() -> {
			eval("TestInvokeCallback_Int32_1_2({|x| throw 'one' $ x }, 1, {|y| throw 'two' $ y }, 2)");
			}, SuException.class, "one1");
	}

	@Test
	public void testExceptionReEntrantVi() {
		final String code =
			"g = function(rss)\n" +
			"\t{\n" +
			"\tif 0 < rss.str\n" +
			"\t\t{\n" +
			"\t\ttry\n" +
			"\t\t\tTestInvokeCallback_Recursive_StringSum(this, RSS(Object(), Object(), str: rss.str - 1))\n" +
			"\t\tcatch (x)\n" +
			"\t\t\tthrow x + 1\n" +
			"\t\t}\n" +
			"\telse throw 0\n" +
			"\t}\n" +
			"g(RSS(Object(), Object(), str: '15'))";
		assertThrew(() -> { eval(code); }, SuException.class, "15");
	}

	//
	// RE-ENTRANT CALLBACKS
	//

	@Test
	public void testReEntrance() {
		// This test runs a recursive loop causing several levels of callbacks
		// (dll --> callback --> Java --> dll --> callback --> Java ...) to
		// ensure that this aspect of the program behaves as expected.
		final String code =
			"f = function(n) " +
			"{ return 0 < n ? TestInvokeCallback_Int32_1(this, n - 1) + 1: n }\n" +
			"f(10)";
		assertEquals(10, eval(code));
	}

	//
	// SELF-CLEARING CALLBACK
	//

	@Test
	public void testSelfClearingCallback() {
		// Callbacks must be able to clear themselves because this is supported
		// in cSuneido, e.g. by WndProc.NCDESTROY(), so the below operation
		// should work just fine.
		final String code =
			"f = function(n) " +
			"{ ClearCallback(this); return 2 * n; }\n" +
			"TestInvokeCallback_Int32_1(f, 3)";
		assertEquals(6, eval(code));
		assertEquals(eval("#()"), eval("Callbacks()"));
	}
}
