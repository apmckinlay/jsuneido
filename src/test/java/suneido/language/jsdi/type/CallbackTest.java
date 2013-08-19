package suneido.language.jsdi.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.util.testing.Throwing.assertThrew;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import suneido.SuContainer;
import suneido.SuException;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.SimpleContext;
import suneido.language.jsdi.ThunkManager;
import suneido.util.testing.Assumption;

/**
 * Test for {@link Callback}.
 *
 * @author Victor Schappert
 * @since 20130807
 * @see suneido.language.ParseAndCompileCallbackTest
 */
public class CallbackTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
		CONTEXT = new SimpleContext(NAMED_TYPES);
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_TYPES = {
		"TestCallback_Long1", "callback(long a)",
		"TestCallback_Long2", "callback(long a, long b)",
		"TestCallback_Packed_CharCharShortLong", "callback(Packed_CharCharShortLong a)",
		"TestCallback_Recursive_StringSum", "callback(Recursive_StringSum1_Callback * ptr)",
		"TestCallback_Recursive_StringSum_Bad", "callback(Recursive_StringSum1_Dll * ptr)",
		"Packed_CharCharShortLong", "struct { char a; char b; short c; long d; }",
		"Recursive_StringSum1_Dll",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tRecursive_StringSum0_Dll * inner\n" +
			"\t}",
		"Recursive_StringSum0_Dll",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tlong inner\n" +
			"\t}",
		"Recursive_StringSum1_Callback",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tstring buffer_\n" +
			"\tlong len\n" +
			"\tRecursive_StringSum0_Callback * inner\n" +
			"\t}",
		"Recursive_StringSum0_Callback",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tstring buffer_\n" +
			"\tlong len\n" +
			"\tlong inner\n" +
			"\t}",
		"TestInvokeCallback_Long1",
			"dll long jsdi:_TestInvokeCallback_Long1@8(TestCallback_Long1 f, long a)",
		"TestInvokeCallback_Long2",
			"dll long jsdi:_TestInvokeCallback_Long2@12(TestCallback_Long2 f, long a, long b)",
		"TestInvokeCallback_Packed_CharCharShortLong",
			"dll long jsdi:_TestInvokeCallback_Packed_CharCharShortLong@12(" +
				"TestCallback_Packed_CharCharShortLong f, Packed_CharCharShortLong a)",
		"TestInvokeCallback_Recursive_StringSum",
			"dll long jsdi:_TestInvokeCallback_Recursive_StringSum@8(" +
				"TestCallback_Recursive_StringSum f, Recursive_StringSum1_Dll * ptr)",
		"TestInvokeCallback_Recursive_StringSum_Bad",
			"dll long jsdi:_TestInvokeCallback_Recursive_StringSum@8(" +
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
			eval("Object(TestInvokeCallback_Long1( f = function(a) { return a }, 11), ClearCallback(f))")
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1Method() {
		assertEquals(
			eval("#(21, true)"),
			eval(
				"x = class { New(.value) { } Method(a, b = -1) { return .value + a - b } };" +
				"Object(TestInvokeCallback_Long1((y = new x(30)).Method, -10), ClearCallback(y.Method))"
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
				"Object(TestInvokeCallback_Long1(z = x(), 0x1 << 9), ClearCallback(z))"
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
				"Object(TestInvokeCallback_Long1(x, 0x1 << 17), ClearCallback(x), ClearCallback(x))"
			)
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1Block() {
		assertEquals(
			eval("#(31, true)"),
			eval("Object(TestInvokeCallback_Long1(x = { |a| a }, 31), ClearCallback(x))")
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue1Closure() {
		assertEquals(
			eval("#(601, true)"),
			eval(
				"a = Object(f: function() { return 300 })\n" +
				"Object(TestInvokeCallback_Long1(x = { |b| (a.f)() + b }, 301), ClearCallback(x))"
			)
		);
		assertEquals(new SuContainer(), eval("Callbacks()"));
	}

	@Test
	public void testBasicValue2Function() {
		assertEquals(-1, eval("TestInvokeCallback_Long2( function(a, b) { return a + b }, 1000, -1001)"));
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testBasicValue2Method() {
		assertEquals(
			12,
			eval(
				"x = class { Method(a = 1, b = 2) { return a * b } } \n" +
				"TestInvokeCallback_Long2(x().Method, -3, -4)"
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
				"TestInvokeCallback_Long2(x(), 12, 13)"
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
				"TestInvokeCallback_Long2(x, -1, 1000)"
			)
		);
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testBasicValue2Block() {
		assertEquals(
			0x19820207,
			eval("TestInvokeCallback_Long2( { |a,b| a | b }, 0x19820000, 0x207)") 
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
				"TestInvokeCallback_Long2(y, 2, 3)"
			)
		);
		assertFalse(new Integer(0).equals(eval("Callbacks().Size()")));
	}

	@Test
	public void testStructValueFunction() {
		assertEquals(
			-10,
			eval(
				"TestInvokeCallback_Packed_CharCharShortLong(" +
					"function(x) { return x.a + x.b + x.c + x.d }," +
					"PCCSL(-1, -2, -3, -4)" +
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
				"TestInvokeCallback_Packed_CharCharShortLong(" +
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
			eval("TestInvokeCallback_Long2(function(@a) { s = 0; for (b in a) s += b; return s; }, 1, 2)")
		);
	}

	//
	// TYPES THAT CAN'T BE MARSHALLED OUT OF CALLBACK
	//

	@Test
	public void testCantUnmarshallBuffer() {
		assertThrew(
			new Runnable() {
				public void run() {
					eval("TestInvokeCallback_Recursive_StringSum_Bad(" +
							"SuTestSumString," +
							"RSS(PCCSL(), PCCSL(5, 6, 7, 8))" +
						")"
					);
				}
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
		assertThrew(new Runnable() {
			public void run() {
				eval("TestInvokeCallback_Long1(function(x) { x.Add('y') }, 2)");
			}
		}, SuException.class, "method not found:.*Add");
	}

	//
	// BAD RETURN VALUES
	//

	@Test
	public void testExceptionReturnValueNone_Basic() {
		assertEquals(0, eval("TestInvokeCallback_Long1(function(@x) { }, 1)"));
	}

	@Test
	public void testExceptionReturnValueNone_Vi() {
		assertEquals(0, eval("TestInvokeCallback_Recursive_StringSum(function(x) { return }, Object())"));
	}

	@Test
	public void testExceptionReturnValueNotConvertible() {
		assertThrew(new Runnable() {
			public void run() {
				eval("TestInvokeCallback_Long1(function(a) { return 'Michalek/Spezza/Ryan' }, 2)");
			}
		}, SuException.class, "can't convert");
	}

	//
	// EXCEPTION PROPAGATION
	//

	@Test
	public void testExceptionThrowBasic() {
		// Straightforward test -- make sure it works as a one-off
		assertThrew(new Runnable() {
			public void run() {
				eval("TestInvokeCallback_Long1(function(a) { throw a }, 444)");
			}
		}, SuException.class, "444");
		// In case exception throwing causes subtle problems which might be
		// detected later, run a stress test...
		for (int k = 0; k < 100; ++k) {
			int x = k;
			int y = k + 1;
			String expected = String.format("#\\(%d, %d\\)", x, y); // escape parentheses for regex
			final String code = String
					.format("TestInvokeCallback_Long2(function(@a) { throw Display(a) }, %d, %d)",
							x, y);
			assertThrew(new Runnable() {
				public void run() {
					eval(code);
				}
			}, SuException.class, expected);
		}
	}

	@Test
	public void testExceptionThrowVi() {
		assertThrew(new Runnable() {
			public void run() {
				eval(
						"TestInvokeCallback_Recursive_StringSum(" +
								"{ |x| throw SuTestSumString(x) }, " +
								"RSS(PCCSL(102, -1, 0, -1), PCCSL(1, 1, 1, 97), '55', inner: RSS(PCCSL(50, 50, 50, 50), PCCSL(0, 0, 25, 25), '50'))" +
						")"
				);
			}
		}, SuException.class, "555");
	}

	@Test
	public void testExceptionReEntrantBasic() {
		final String code =
			"f = function(n)\n" +
			"\t{\n" +
			"\tif 0 < n\n" +
			"\t\t{\n" +
			"\t\ttry\n" +
			"\t\t\tTestInvokeCallback_Long1(this, n - 1)\n" +
			"\t\tcatch (x)\n" +
			"\t\t\tthrow x + 1\n" +
			"\t\t}\n" +
			"\telse throw 0\n" +
			"\t}\n" +
			"f(20)";
		assertThrew(new Runnable() {
			public void run() {
				eval(code);
			}
		}, SuException.class, "20");
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
		assertThrew(new Runnable() {
			public void run() {
				eval(code);
			}
		}, SuException.class, "15");
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
			"{ return 0 < n ? TestInvokeCallback_Long1(this, n - 1) + 1: n }\n" +
			"f(10)";
		assertEquals(10, eval(code));
	}
}
