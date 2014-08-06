/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.com;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static suneido.util.testing.Throwing.assertThrew;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import suneido.SuValue;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.SimpleContext;
import suneido.jsdi.com.COMException;
import suneido.jsdi.com.COMobject;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.util.testing.Assumption;

/**
 * Test for {@link COMobject}.
 *
 * @author Victor Schappert
 * @since 20130928
 */
@DllInterface
@RunWith(Parameterized.class)
public class COMobjectTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIsOnWindows();
		CONTEXT = new SimpleContext(NAMED_TYPES);
	}

	@Parameters
	public static Collection<Object[]> isFast() {
		return Arrays.asList(new Object[][] { { Boolean.FALSE }, { Boolean.TRUE } }); 
	}

	public COMobjectTest(boolean isFast) {
		JSDI.getInstance().setFastMode(isFast);
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_TYPES = {
		"TestCreateComObject", "dll long jsdi:TestCreateComObject()",
		"MakeTestObject", "function() { COMobject(TestCreateComObject()) }"
	};

	private static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	//
	// TESTS
	//

	@Test
	public void testType() {
		assertEquals("COMobject", eval("Type(MakeTestObject())"));
	}

	@Test
	public void testCreationFunction() {
		assertFalse((boolean) eval("0 is TestCreateComObject()"));
		assertNotEquals(false, eval("MakeTestObject()"));
	}

	@Test
	public void testCreateComObject() {
		assertTrue((boolean) eval("MakeTestObject().Dispatch?()"));
	}

	@Test
	public void testGetProgID() {
		// Test the framework's ability to query the ProgID of an IDispatch.
		// We know that the ProgID for the test object should be "ITestJSDICom".
		assertTrue(eval("Display(MakeTestObject())").toString()
				.contains("ITestJSDICom"));
	}

	@Test
	public void testInitialRefCount() {
		// One for IUnknown, one because we have to QueryInterface() an
		// IDispatch.
		assertTrue((boolean) eval("1 is MakeTestObject().RefCount"));
	}

	@Test
	public void testRelease() {
		assertTrue((boolean) eval("0 is MakeTestObject().Release()"));
	}

	@Test
	public void testReleaseCheck() {
		final String[] suffixes = { "Dispatch?()", "RefCount",
				"BoolValue = true", "Sum2Ints(1, 2)" };
		for (final String suffix : suffixes) {
			final String code = "x = MakeTestObject(); x.Release(); x."
					+ suffix;
			assertThrew(() -> { eval(code); },
					COMException.class, "already released");
		}
	}

	@Test
	public void testPropBool() {
		final String code = "x = 0                                  \n"
				+ "y = MakeTestObject()                       \n"
				+ "if (false isnt y.BoolValue) throw 'fail1'  \n"
				+ "if (y.BoolValue) ++x                       \n"
				+ "y.BoolValue = true                         \n"
				+ "if (true isnt y.BoolValue) throw 'fail2'   \n"
				+ "if (y.BoolValue) ++x                       \n"
				+ "if (y.BoolValue) ++x                       \n"
				+ "y.BoolValue = false                        \n"
				+ "if (y.BoolValue) ++x                       \n"
				+ "y.BoolValue = true                         \n"
				+ "if (y.BoolValue) ++x                       \n" + "x is 3";
		;
		assertTrue((boolean) eval(code));
	}

	@Test
	public void testPropInt32() {
		// Check we can set it to 0x7fffffff
		{
			final String code = "y = MakeTestObject()                  \n"
					+ "if (0 isnt y.Int32Value) throw 'fail1'          \n"
					+ "y.Int32Value = 0x7fffffff                       \n"
					+ "if (0x7fffffff isnt y.Int32Value) throw 'fail2' \n"
					+ "y.Int32Value";
			assertEquals(0x7fffffff, ((Number) eval(code)).intValue());
		}
		// Check we can set it to -1
		{
			final String code = "y = MakeTestObject()                  \n"
					+ "if (0 isnt y.Int32Value) throw 'fail1'          \n"
					+ "y.Int32Value = -1                               \n"
					+ "if (-1 isnt y.Int32Value) throw 'fail2'         \n"
					+ "y.Int32Value";
			assertEquals(-1, ((Number) eval(code)).intValue());
		}
		// Check we can set it to 0x80000000
		{
			final String code = "y = MakeTestObject()                  \n"
					+ "if (0 isnt y.Int32Value) throw 'fail1'          \n"
					+ "y.Int32Value = 0x80000000                       \n"
					+ "if (0x80000000 isnt y.Int32Value) throw 'fail2' \n"
					+ "y.Int32Value";
			assertEquals(Integer.MIN_VALUE, ((Number) eval(code)).intValue());
		}
		// Check overflow
		{
			final String code = "MakeTestObject().Int32Value = 4 * 0x70000000";
			assertThrew(() -> { eval(code); },
					COMException.class, "overflow");
		}
	}

	@Test
	public void testPropInt64() {
		final long BIG_NUMBER = Integer.MAX_VALUE * 10L;
		final long SMALL_NUMBER = Integer.MIN_VALUE * 10L;
		// Check we can set it to a positive number outside the 32-bit range
		{
			final String code = String.format(
					"y = MakeTestObject()                              \n"
					+ "if (0 isnt y.Int64Value) throw 'fail1'          \n"
					+ "y.Int64Value = %1$d                             \n"
					+ "if (%1$d isnt y.Int64Value) throw 'fail2'       \n"
					+ "y.Int64Value", BIG_NUMBER);
			assertEquals(BIG_NUMBER, ((Number) eval(code)).longValue());
		}
		// Check we can set it to a negative number outside the 32-bit range
		{
			final String code = String.format(
					"y = MakeTestObject()                              \n"
					+ "if (0 isnt y.Int64Value) throw 'fail1'          \n"
					+ "y.Int64Value = %1$d                             \n"
					+ "if (%1$d isnt y.Int64Value) throw 'fail2'       \n"
					+ "y.Int64Value", SMALL_NUMBER);
			assertEquals(SMALL_NUMBER, ((Number) eval(code)).longValue());
		}
	}

	@Test
	public void testPropDouble() {
		final double BIG_NUMBER = 20131102.5;
		final double SMALL_NUMBER = -0.5;
		// Check we can set it to a positive double value
		{
			final String code = String.format(
					"y = MakeTestObject()                               \n"
					+ "if (0.0 isnt y.DoubleValue) throw 'fail1'        \n"
					+ "y.DoubleValue = %1$f                             \n"
					+ "if (%1$f isnt y.DoubleValue) throw 'fail2'       \n"
					+ "y.DoubleValue", BIG_NUMBER);
			assertEquals(BIG_NUMBER, ((Number) eval(code)).doubleValue(), 0.0);
		}
		// Check we can set it to a negative double value
		{
			final String code = String.format(
					"y = MakeTestObject()                               \n"
					+ "if (0.0 isnt y.DoubleValue) throw 'fail1'        \n"
					+ "y.DoubleValue = %1$f                             \n"
					+ "if (%1$f isnt y.DoubleValue) throw 'fail2'       \n"
					+ "y.DoubleValue", SMALL_NUMBER);
			assertEquals(SMALL_NUMBER, ((Number) eval(code)).doubleValue(), 0.0);
		}
	}

	@Test
	public void testPropString() {
		assertEquals(eval("MakeTestObject().StringValue"), "");
		assertEquals(
				eval("x = MakeTestObject(); x.StringValue = 'abc'; x.StringValue"),
				"abc");
	}

	@Test
	public void testPropStringNoLeak() {
		// If there's a leak in the string handling code, this should run the
		// JVM out of memory.
		eval(
			"comobject = MakeTestObject()               \n" +
			"str = 'hello, world'.Repeat(1024 * 24)     \n" +
			"for (i = 0; i < 1024; ++i)                 \n" +
			"    {                                      \n" +
			"    str_i = i $ str                        \n" +
			"    comobject.StringValue = str_i          \n" +
			"    if str_i isnt comobject.StringValue    \n" +
			"        throw 'fail ' $ i                  \n" +
			"    }"
		);
	}

	@Test
	public void testPropStringEmbeddedNull() {
		assertEquals(
				eval("x = MakeTestObject(); x.StringValue = 'a\\x00b\\x00c\\x00d'; x.StringValue"),
				"a\u0000b\u0000c\u0000d");
	}

	@Test
	public void testPropStringBuffer() {
		assertEquals(
				eval("x = MakeTestObject(); x.StringValue = Buffer(7, 'a\\x00b\\x00c\\x00d'); x.StringValue"),
				"a\u0000b\u0000c\u0000d");
	}

	@Test
	public void testPropDate() {
// FIXME: Re-enable this test. It is going to take some doing, however, because
//        jSuneido dates currently contain implicit timezones whereas cSuneido
//        dates are in some kind of strange timezone-divorced format.
//        In other words, in jSuneido if two computers in different timezones
//        simultaneously do 'x = Date()', the actual JVM representation of 'x'
//        will represent the SAME UTC time value whereas in cSuneido if the same
//        computers simultaneously do 'x = Date()', the internal representation
//        will be different because it's a different "clock time".
//		assertEquals(true, eval("MakeTestObject().DateValue is #18991230"));
//		final String DATE = "#20131102.190148500";
//		final String CODE = String
//				.format("x = MakeTestObject(); x.DateValue = %1$s; %1$s is x.DateValue",
//						DATE);
//		assertEquals(true, eval(CODE));
	}

	@Test
	public void testIUnknownGet() {
		// The IUnkValue property just returns another COMobject instance
		// wrapping the same underlying test COM object. Each time it is
		// accessed, the reference count on the underlying object should go up
		// by one.
		assertEquals(
			"ok!",
			eval(
				"a = MakeTestObject()                                    \n" +
				"if (1 isnt a.RefCount)                                  \n" +
				"    throw '1) initial RefCount wrong'                   \n" +
				"b = a.IUnkValue                                         \n" +
				"if (Type(b) isnt 'COMobject')                           \n" +
				"    throw '2) b is not a COMobject'                     \n" +
				"if (2 isnt a.RefCount)                                  \n" +
				"    throw '3) a.RefCount should be 2, is ' $ a.RefCount \n" +
				"if (b.Dispatch?())                                      \n" +
				"    throw '4) b should be just a plain IUnknown'        \n" +
				"c = a.IUnkValue                                         \n" +
				"if (Type(c) isnt 'COMobject')                           \n" +
				"    throw '5) c is not a COMobject'                     \n" +
				"if (3 isnt a.RefCount)                                  \n" +
				"    throw '6) a.RefCount should be 3, is ' $ a.RefCount \n" +
				"b.Release()                                             \n" +
				"if (2 isnt a.RefCount)                                  \n" +
				"    throw '7) a.RefCount should be down to 2 now'       \n" +
				"c.Release()                                             \n" +
				"if (1 isnt a.RefCount)                                  \n" +
				"    throw '8) a.RefCount should be down to 1 now'       \n" +
				"a.Release()                                             \n" +
				"'ok!'"
			)
		);
	}

	@Test
	public void testIDispatchGet() {
		// The IDispValue property just returns another COMobject instance
		// wrapping the same underlying test COM object. Each time it is
		// accessed, the reference count on the underlying object should go up
		// by one.
		assertEquals(
			"ok!",
			eval(
				"a = MakeTestObject()                                    \n" +
				"if (1 isnt a.RefCount)                                  \n" +
				"    throw '1) initial RefCount wrong'                   \n" +
				"b = a.IDispValue                                        \n" +
				"if (Type(b) isnt 'COMobject')                           \n" +
				"    throw '2) b is not a COMobject'                     \n" +
				"if (2 isnt a.RefCount)                                  \n" +
				"    throw '3) a.RefCount should be 2, is ' $ a.RefCount \n" +
				"if (not b.Dispatch?())                                  \n" +
				"    throw '4) b should be an IDispatch'                 \n" +
				"if (2 isnt b.RefCount)                                  \n" +
				"    throw '5) b.RefCount should be 2, is ' $ a.RefCount \n" +
				"c = b.IDispValue                                        \n" +
				"if (Type(c) isnt 'COMobject')                           \n" +
				"    throw '6) c is not a COMobject'                     \n" +
				"if (3 isnt a.RefCount)                                  \n" +
				"    throw '7) a.RefCount should be 3, is ' $ a.RefCount \n" +
				"if (not c.Dispatch?())                                  \n" +
				"    throw '8) c should an IDispatch'                    \n" +
				"if (3 isnt b.RefCount)                                  \n" +
				"    throw '9) b.RefCount should be 3, is ' $ b.RefCount \n" +
				"if (3 isnt c.RefCount)                                  \n" +
				"    throw '10) c.RefCount should be 3, is ' $ c.RefCount\n" +
				"a.Release()                                             \n" +
				"if (2 isnt b.RefCount)                                  \n" +
				"    throw '11) b.RefCount should be down to 2 now'      \n" +
				"if (2 isnt c.RefCount)                                  \n" +
				"    throw '12) c.RefCount should be down to 2 now'      \n" +
				"c.Release()                                             \n" +
				"if (1 isnt b.RefCount)                                  \n" +
				"    throw '13) b.RefCount should be down to 1 now'      \n" +
				"b.Release()                                             \n" +
				"'ok!'"
			)
		);
	}

	@Test
	public void testIUnknownPut() {
		// When the Suneido programmer passes a COMobject to an IDispatch Invoke
		// property put or method call, the jsdi COM native code converts the
		// COMobject to an IDispatch or IUnknown pointer, as appropriate, and
		// increments the reference count by one following the COM convention
		// that caller allocates, callee releases. The test method
		// 'NoopIUnk()' just calls release on the IUnknown pointer it receives,
		// thus reducing the reference count by one. The point of this test
		// is to make sure the native code is managing the reference count
		// appropriately.
		assertEquals(
				"ok!",
				eval(
					"a = MakeTestObject()                                    \n" +
					"if 1 isnt a.RefCount                                    \n" +
					"    throw '1) initial RefCount wrong'                   \n" +
					"a.NoopIUnk(a)                                           \n" +
					"if 1 isnt a.RefCount                                    \n" +
					"    throw '2) RefCount after method call wrong'         \n" +
					"a.Release()                                             \n" +
					"'ok!'"
		));
	}

	@Test
	public void testIDispatchPut() {
		// See the comment on testIUnknownPut()
		assertEquals(
				"ok!",
				eval(
					"a = MakeTestObject()                                    \n" +
					"if 1 isnt a.RefCount                                    \n" +
					"    throw '1) initial RefCount wrong'                   \n" +
					"a.NoopIDisp(a)                                          \n" +
					"if 1 isnt a.RefCount                                    \n" +
					"    throw '2) RefCount after method call wrong'         \n" +
					"a.Release()                                             \n" +
					"'ok!'"
		));
	}

	@Test
	public void testNoSuchMethod() {
		assertThrew(() -> { eval("MakeTestObject().FakeMethod(1)"); },
				COMException.class, "no member named.*FakeMethod");
	}

	@Test
	public void testNoSuchPropGet() {
		assertThrew(() -> { eval("x = MakeTestObject().FakeProperty"); },
				COMException.class, "no member named.*FakeProperty");
	}

	@Test
	public void testNoSuchPropSet() {
		assertThrew(() -> { eval("MakeTestObject().FakeProperty = 5"); },
				COMException.class, "no member named.*FakeProperty");
	}

	@Test(expected=COMException.class)
	public void testPropSetReadOnly() {
		eval("MakeTestObject().RefCount = 5");
	}

	@Test
	public void testMethodNoArgs() {
		assertEquals(true,
				eval("x = MakeTestObject()                      \n" +
					"x.Int32Value = 1                           \n" +
					"x.Int64Value = -5                          \n" +
					"x.DoubleValue = 31.5                       \n" +
					"x.SumProperties() is 27.5"));
	}

	@Test
	public void testMethodWithArgs() {
		assertEquals(true, eval("MakeTestObject().Sum2Ints(1, 2) is 3"));
		assertEquals(true, eval("MakeTestObject().Sum2Doubles(-1.0, -2.0) is -3.0"));
	}

	@Test
	public void testMethodNoReturnValue() {
		assertEquals(true,
				eval("x = MakeTestObject().IncrementProperties() is 0"));
		assertEquals(
				true,
				eval("x = MakeTestObject() ; x.IncrementProperties() ; x.SumProperties() is 3"));
	}

	@Test
	public void testErrorBadParamCount() {
		assertThrew(() -> { eval("MakeTestObject().Sum2Ints(1)"); },
				COMException.class, "bad param count");
		assertThrew(() -> { eval("MakeTestObject().SumProperties(1, 2, 3)"); },
				COMException.class, "bad param count");
	}

	@Test
	public void testErrorTypeMismatch() {
		assertThrew(() -> { eval("MakeTestObject().Sum2Ints(true, 'boron')"); },
				COMException.class, "type mismatch");
	}

	@Test
	public void testNumberNarrowing() {
		// This test just makes sure that the number narrowing code on the
		// jsdi DLL side is working.
		final COMobject comobject = (COMobject)eval("MakeTestObject()");
		final SuValue sum2ints = comobject.lookup("Sum2Ints");
		assertEquals(27L, sum2ints.eval(comobject, 29L, -2L));
		assertEquals(65L, sum2ints.eval(comobject, new BigDecimal(19),
				new BigDecimal(46))); // Spezza + Wiercoch = Karlsson?
		final SuValue sum2doubles = comobject.lookup("Sum2Doubles");
		assertEquals(new BigDecimal(100.0),
				sum2doubles.eval(comobject, 50L, new BigDecimal(50)));
	}
}
