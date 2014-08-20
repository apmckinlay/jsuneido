/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static suneido.util.testing.Throwing.assertThrew;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import suneido.SuException;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.Numbers;
import suneido.util.testing.Assumption;

/**
 * Test for {@link Dll}.
 *
 * @author Victor Schappert
 * @since 20130808
 * @see suneido.language.ParseAndCompileDllTeste
 */
@DllInterface
@RunWith(Parameterized.class)
public class DllTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIsOnWindows();
		CONTEXT = new SimpleContext(NAMED_TYPES);
	}

	@Parameters
	public static Collection<Object[]> isFast() {
		return Arrays.asList(new Object[][] { { Boolean.FALSE }, { Boolean.TRUE } }); 
	}

	public DllTest(boolean isFast) {
		JSDI.getInstance().setFastMode(isFast);
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_TYPES = {
		"Packed_Int8Int8Int16Int32", "struct { int8 a; int8 b; int16 c; int32 d; }",
		"Packed_Int8x3", "struct { int8 a; int8 b; int8 c; }",
		"Recursive_StringSum1",
			"struct\n" +
			"\t{\n" +
			"\tPacked_Int8Int8Int16Int32[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tint32 len\n" +
			"\tRecursive_StringSum0 * inner\n" +
			"\t}",
		"Recursive_StringSum0",
			"struct\n" +
			"\t{\n" +
			"\tPacked_Int8Int8Int16Int32[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tint32 len\n" +
			"\tpointer inner\n" +
			"\t}",
		"Swap_StringInt32Int32",
			"struct { string str; int32 a; int32 b; }",
		"Int32Wrapper", "struct { int32 x }",
		"StringWrapper", "struct { string x }",
		"DoubleWrapper", "struct { double x }",
		"ResourceWrapper", "struct { resource x }",
		"PtrDouble", "struct { DoubleWrapper * x }",
		"PtrPtrDouble", "struct { PtrDouble * x }",
		"TestVoid", "dll void jsdi:TestVoid()",
		"TestInt8", "dll int8 jsdi:TestInt8(int8 a)",
		"TestInt16", "dll int16 jsdi:TestInt16(int16 a)",
		"TestInt32", "dll int32 jsdi:TestInt32(int32 a)",
		"TestInt64", "dll int64 jsdi:TestInt64(int64 a)",
		"TestReturn1_0Float", "dll float jsdi:TestReturn1_0Float()",
		"TestReturn1_0Double", "dll double jsdi:TestReturn1_0Double()",
		"TestFloat", "dll float jsdi:TestFloat(float a)",
		"TestDouble", "dll double jsdi:TestDouble(double a)",
		"TestCopyInt32Value", "dll void jsdi:TestCopyInt32Value(Int32Wrapper * src, Int32Wrapper * dst)",
		"TestSumTwoInt8s", "dll char jsdi:TestSumTwoInt8s(int8 a, int8 b)",
		"TestSumTwoInt16s", "dll short jsdi:TestSumTwoInt16s(int16 a, int16 b)",
		"TestSumTwoInt32s", "dll int32 jsdi:TestSumTwoInt32s(int32 a, int32 b)",
		"TestSumTwoFloats", "dll float jsdi:TestSumTwoFloats(float a, float b)",
		"TestSumTwoDoubles", "dll double jsdi:TestSumTwoDoubles(double a, double b)",
		"TestSumThreeInt32s", "dll int32 jsdi:TestSumThreeInt32s(int32 a, int32 b, int32 c)",
		"TestSumFourInt32s", "dll int32 jsdi:TestSumFourInt32s(int32 a, int32 b, int32 c, int32 d)",
		"TestSumFiveInt32s", "dll int32 jsdi:TestSumFiveInt32s(int32 a, int32 b, int32 c, int32 d, int32 e)",
		"TestSumSixInt32s", "dll int32 jsdi:TestSumSixInt32s(int32 a, int32 b, int32 c, int32 d, int32 e, int32 f)",
		"TestSumSixMixed", "dll int32 jsdi:TestSumSixMixed(double a, int8 b, float c, int16 d, float e, int64 f)",
		"TestSumSevenInt32s", "dll int32 jsdi:TestSumSevenInt32s(int32 a, int32 b, int32 c, int32 d, int32 e, int32 f, int32 g)",
		"TestSumEightInt32s", "dll int32 jsdi:TestSumEightInt32s(int32 a, int32 b, int32 c, int32 d, int32 e, int32 f, int32 g, int32 h)",
		"TestSumNineInt32s", "dll int32 jsdi:TestSumNineInt32s(int32 a, int32 b, int32 c, int32 d, int32 e, int32 f, int32 g, int32 h, int32 i)",
		"TestSumInt8PlusInt64", "dll int64 jsdi:TestSumInt8PlusInt64(char a, int64 b)",
		"TestSumPackedInt8Int8Int16Int32", "dll int32 jsdi:TestSumPackedInt8Int8Int16Int32(Packed_Int8Int8Int16Int32 x)",
		"TestSumPackedInt8x3", "dll int32 jsdi:TestSumPackedInt8x3(Packed_Int8x3 x)",
		"TestSumManyInts", "dll int64 jsdi:TestSumManyInts(int8 a, int16 b, int32 c, Swap_StringInt32Int32 d, int64 e, Packed_Int8Int8Int16Int32 f, Packed_Int8x3 g, Recursive_StringSum1 h, Recursive_StringSum0 * i)",
		"TestDivideTwoInt32s", "dll int32 jsdi:TestDivideTwoInt32s(int32 a, int32 b)",
		"TestStrLen", "dll int32 jsdi:TestStrLen([in] string str)",
		"TestHelloWorldReturn", "dll string jsdi:TestHelloWorldReturn(bool flag)",
		"TestHelloWorldOutParam", "dll void jsdi:TestHelloWorldOutParam(StringWrapper * ptr)",
		"TestHelloWorldOutBuffer", "dll void jsdi:TestHelloWorldOutBuffer(buffer buffer_, int32 size)",
		"TestHelloWorldOutBufferAsStr", "dll void jsdi:TestHelloWorldOutBuffer(string buffer_, int32 size)",
		"TestReturnPtrPtrPtrDoubleAsUInt64", "dll int64 jsdi:TestReturnPtrPtrPtrDoubleAsUInt64(PtrPtrDouble * ptr)",
		"TestSumString", "dll int32 jsdi:TestSumString(Recursive_StringSum1 * rss)",
		"TestSumResource", "dll int32 jsdi:TestSumResource(resource res, ResourceWrapper * pres)",
		"TestSwap", "dll int32 jsdi:TestSwap(Swap_StringInt32Int32 * ptr)",
		"TestReturnString", "dll string jsdi:TestReturnString([in] string str)",
		"TestReturnPtrString", "dll string jsdi:TestReturnPtrString(StringWrapper * ptr)",
		"TestReturnStringOutBuffer", "dll string jsdi:TestReturnStringOutBuffer(string str, buffer buffer_, int32 size)",
		"TestReturnStatic_Packed_Int8Int8Int16Int32",
			"dll pointer jsdi:TestReturnStatic_Packed_Int8Int8Int16Int32(Packed_Int8Int8Int16Int32 * ptr)",
	};

	private static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	private static final String[] NOT_AN_OBJECT = { "50", "true", "false",
			"-123.456", "#19900606.020700000" };

	private static void assertNeedObject(final String code,
			final Class<? extends Throwable> exception, final String pattern) {
		for (final String value : NOT_AN_OBJECT) {
			assertThrew(() -> { eval(String.format(code, value)); },
					exception, pattern);
		}
	}

	private static BigDecimal bd(double d) {
		return Numbers.toBigDecimal(d);
	}

	//
	// TESTS
	//

	@Test
	public void testVoid() {
		eval("TestVoid()");
	}

	@Test
	public void testInt8() {
		assertEquals((int)'$', eval("TestInt8('$'.Asc())"));
	}

	@Test
	public void testInt16() {
		assertEquals(0xfff, eval("TestInt16(0xfff)"));
	}

	@Test
	public void testInt32() {
		assertEquals(0xcafebabe, eval("TestInt32(0xcafebabe)"));
	}

	@Test
	public void testInt64() {
		assertEquals(0x100000000L, eval("TestInt64(4 * (1 << 30))"));
	}

	@Test
	public void testReturn1_0Float() {
		assertEquals(bd(1.0), eval("TestReturn1_0Float()"));
	}

	@Test
	public void testReturn1_0Double() {
		assertEquals(bd(1.0), eval("TestReturn1_0Double()"));
	}

	@Test
	public void testFloat() {
		assertEquals(bd(-888888.75), eval("TestFloat(-888888.75)"));
	}

	@Test
	public void testDouble() {
		assertEquals(bd(-7777777777.25), eval("TestDouble(-7777777777.25)"));
	}

	@Test
	public void testCopyInt32Value() {
		assertEquals(17, eval("TestCopyInt32Value(Object(x: 17), y = Object()); y.x"));
	}

	@Test
	public void testSumTwoInt8s() {
		assertEquals(3, eval("TestSumTwoInt8s(1, 2)"));
	}

	@Test
	public void testSumTwoInt16s() {
		assertEquals(0x7feb, eval("TestSumTwoInt16s(0x7f00, 0xeb)"));
	}

	@Test
	public void testSumTwoInt32s() {
		assertEquals(0x7feb1982, eval("TestSumTwoInt32s(0x70e01080, 0x0f0b0902)"));
	}

	@Test
	public void testSumTwoFloats() {
		assertEquals(bd(3 * 987654), eval("TestSumTwoFloats(987654, 2 * 987654)"));
	}

	@Test
	public void testSumTwoDoubles() {
		assertEquals(bd(5 * 1234567), eval("TestSumTwoDoubles(3 * 1234567, 2 * 1234567)"));
	}

	@Test
	public void testSumThreeInt32s() {
		assertEquals(0x1812feb7, eval("TestSumThreeInt32s(0x18000007, 0x00120000, 0x0000feb0)"));
	}

	@Test
	public void testSumFourInt32s() {
		assertEquals(-10, eval("TestSumFourInt32s(-1, -2, -3, -4)"));
	}

	@Test
	public void testSumFiveInt32s() {
		assertEquals(1, eval("TestSumFiveInt32s(-2, 3, -3, 1, 2)"));
	}

	@Test
	public void testSumSixInt32s() {
		assertEquals(2, eval("TestSumSixInt32s(-2, 3, -3, 1, 2, 1)"));
	}

	@Test
	public void testSumSixMixed() {
		assertEquals(-37, eval("TestSumSixMixed(-97, -2, 60, 9, -10, 3)"));
	}

	@Test
	public void testSumSevenInt32s() {
		assertEquals(12, eval("TestSumSevenInt32s(-2, 3, -3, 1, 2, 1, 10)"));
	}

	@Test
	public void testSumEightInt32s() {
		assertEquals(100, eval("TestSumEightInt32s(-2, 3, -3, 1, 2, 1, 10, 88)"));
	}

	@Test
	public void testSumNineInt32s() {
		assertEquals(-250, eval("TestSumNineInt32s(-2, 3, -3, 1, 2, 1, 10, 88, -350)"));
	}

	@Test
	public void testSumInt8PlusInt64() {
		assertEquals(0x00000000ffffffffL, eval("TestSumInt8PlusInt64(-1, 4 * (1 << 30))"));
	}

	@Test
	public void testSumPackedInt8Int8Int16Int32() {
		assertEquals(0, eval("TestSumPackedInt8Int8Int16Int32(Object())"));
		assertEquals(
				100,
				eval("TestSumPackedInt8Int8Int16Int32(Object(b: 50, c: 1000, d: -950))"));
		assertNeedObject("TestSumPackedInt8Int8Int16Int32(%s)",
				JSDIException.class, "can't convert .* to object");
	}

	@Test
	public void testSumPackedInt8x3() {
		assertEquals(0, eval("TestSumPackedInt8x3(Object())"));
		assertEquals(-120, eval("TestSumPackedInt8x3(Object(a: -20, c: -100))"));
	}

    @Test
    public void testSumManyInts() {
        assertEquals(
            (int)(long)eval("TestSumManyInts(-2, 1, -2, " +
                 "Object() /* note this contributes 1 via TestSwap() because a == b */, " +
                 "-2," +
                 "Object(a: -2, b: -2, c: -3, d: 8) /* contributes 1 */," +
                 "Object(a: 1, b: -2, c: -1) /* contributes -2 */," +
                 "Object(x: Object(1: Object(a: -2, b: 1, c: 1, d: -2)), inner: Object(x: Object(a: -5, b: 5, c: -3, d: 5))) /* contributes 1 */," +
                 "Object(str: '1') /* contributes 1 */)"
            ), -2 + 1 - 2 + 1 - 2 + 1 - 2 + 1 - 2
        );
    }

	@Test
	public void testStrLen() {
		assertEquals(0, eval("TestStrLen(false)")); // equivalent to NULL
		assertEquals(0, eval("TestStrLen(0)"));     // equivalent to NULL
		assertEquals(0, eval("TestStrLen('')"));
		assertEquals(1, eval("TestStrLen(1)"));     // gets converted to "1"
		assertEquals(1, eval("TestStrLen(2)"));     // gets converted to "2"
		assertEquals(2, eval("TestStrLen(20)"));    // gets converted to "20"
		assertEquals(4, eval("TestStrLen(true)"));  // gets converted to "true"
		assertEquals(34, eval("TestStrLen('supercalifragilisticexpialidocious')"));
		assertThrew(() -> { eval("TestStrLen(Buffer(1, 'a'))"); },
			JSDIException.class, "cannot safely be marshalled");
		assertEquals(0, eval("TestStrLen(Buffer(1))"));
		assertEquals(0, eval("TestStrLen(Buffer(1000))"));
		assertThrew(() -> { eval("TestStrLen(Buffer(1, 'a'))"); },
				JSDIException.class, "cannot safely be marshalled");
		assertEquals(1, eval("TestStrLen(Buffer(2, 'a'))"));
		assertEquals(1, eval("TestStrLen(Buffer(20, 'a'))"));
		assertEquals(11, eval("TestStrLen(Buffer(20, 'hello world'))"));
	}

	@Test
	public void testHelloWorldReturn() {
		assertSame(Boolean.FALSE, eval("TestHelloWorldReturn(false)"));
		assertEquals("hello world", eval("TestHelloWorldReturn(true)"));
	}

	@Test
	public void testHelloWorldOutParam() {
		assertEquals("hello world", eval("TestHelloWorldOutParam(ptr = Object()); ptr.x"));
	}

	@Test
	public void testHelloWorldOutBuffer() {
		eval("TestHelloWorldOutBuffer(false, 0)");
		final int N = "hello world".length();
		for (int k = 1; k <= N + 1; ++k) {
			String expected = "hello world\u0000\u0000".substring(0, k);
			String code = String.format(
				"TestHelloWorldOutBuffer(b = Buffer(%d), b.Size()); b", k);
			assertEquals(new Buffer(k, expected), eval(code));
		}
	}

	@Test
	public void testHelloWorldOutBufferAsStr() {
		// This tests that when you set the type to 'string' but pass in a
		// Buffer, the Buffer gets truncated at the first NUL so that it looks
		// like a string.
		final int N = "hello world".length();
		for (int k = 1; k <= N + 5; ++k) {
			String expected = "hello world".substring(0, Math.min(k, N));
			String code = String.format(
					"TestHelloWorldOutBufferAsStr(b = Buffer(%d), b.Size()); b", k);
			Buffer x = new Buffer(expected.length(), expected);
			Object y = eval(code);
			assertEquals(x, y);
			assertEquals(x, expected);
			assertEquals(y, expected);
		}
	}

	@Test
	public void testReturnPtrPtrPtrDoubleAsUInt64() {
		assertEquals(0L, eval("TestReturnPtrPtrPtrDoubleAsUInt64(Object())"));
		assertEquals(0L, eval("TestReturnPtrPtrPtrDoubleAsUInt64(Object(x: Object()))"));
		assertEquals(0L, eval("TestReturnPtrPtrPtrDoubleAsUInt64(Object(x: Object()))"));
		assertEquals(0L, eval("TestReturnPtrPtrPtrDoubleAsUInt64(Object(x: Object(x: Object())))"));
		assertEquals(0L, eval("TestReturnPtrPtrPtrDoubleAsUInt64(Object(x: Object(x: Object(x: 0.0))))"));
		Object x = eval("TestReturnPtrPtrPtrDoubleAsUInt64(Object(x: Object(x: Object(x: 0.5))))");
		Number n = (Number)x;
		assertEquals(0.5, Double.longBitsToDouble(n.longValue()), 0.0);
	}

	@Test
	public void testSumString() {
		assertEquals(0, eval("TestSumString(Object())"));
		assertEquals(
				100,
				eval(
					"rss = Object(" +
							"x: Object(Object(a: 1, b: 2, c: 3, d: 4))," +
							"str: '90'" +
						")\n" +
					"TestSumString(rss)"
				)
			);
		assertEquals(
				eval("Object(-100, Buffer(50, '-100'))"),
				eval(
					"rss = Object(" +
							"x: Object(" +
								"Object(a: 1, b: 2, c: 3, d: 4), " +
								"Object(a: -5, b: -4, c: -3, d: -2)" +
							")," +
							"str: '-121'," +
							"buffer_: Buffer(50)," +
							"len: 50," +
							"inner: Object(" +
								"x: Object(Object(), Object(a: 22, b: 1, c: 1, d: 1))," +
								"str: '0'" +
							")" +
						")\n" +
					"Object(TestSumString(rss), rss.buffer_)"
				)
			);
	}

	@Test
	public void testSumResource() {
		assertEquals(0, eval("TestSumResource(0, Object())"));
		assertEquals(5, eval("TestSumResource(5, Object())"));
		assertEquals(6, eval("TestSumResource('6', Object())"));
		assertEquals(eval("#(37, 37)"), eval("Object(TestSumResource(37, x = Object()), x.x)"));
		assertEquals(eval("#(99, 99)"), eval("Object(TestSumResource(66, x = Object(x: 33)), x.x)"));
		assertEquals(
			eval("#(9999999, 'sum is not an INTRESOURCE')"),
			eval("Object(TestSumResource(1, x = Object(x: '9999998')), x.x)")
		);
	}

	@Test
	public void testSwap() {
		assertEquals(
				eval("#(1, #(str: '=', a: 5, b: 5))"),
				eval("Object(TestSwap(x = Object(a: 5, b: 5)), x)")
			);
		assertEquals(
				eval("#(0, #(str: '!=', a: 1982, b: 207))"),
				eval("Object(TestSwap(x = Object(a: 207, b: 1982)), x)")
			);
	}

	@Test
	public void testReturnString() {
		assertEquals(Boolean.FALSE, eval("TestReturnString(0)"));
		assertEquals(Boolean.FALSE, eval("TestReturnString(false)"));
		for (String s : new String[] { "", "a", "ab", "a somewhat lengthier string" } ) {
			assertEquals(s, eval(String.format("TestReturnString('%s')", s)));
		}
	}

	@Test
	public void testReturnPtrString() {
		assertEquals(Boolean.FALSE, eval("TestReturnPtrString(Object())"));
		assertEquals(Boolean.FALSE, eval("TestReturnPtrString(Object(x: 0))"));
		assertEquals(Boolean.FALSE, eval("TestReturnPtrString(Object(x: false))"));
		for (String s : new String[] { "", "x", "yy" }) {
			assertEquals(s, eval(String.format("TestReturnPtrString(Object(x: '%s'))", s)));
		}
	}


	@Test
	public void testReturnStringOutBuffer() {
		assertEquals(Boolean.FALSE, eval("TestReturnStringOutBuffer(0, 0, 0)"));
		assertEquals(Boolean.FALSE, eval("TestReturnStringOutBuffer(false, 0, 0)"));
		assertEquals(Boolean.FALSE, eval("TestReturnStringOutBuffer(false, false, 0)"));
		assertEquals(Boolean.FALSE, eval("TestReturnStringOutBuffer('abcdefg', false, 0)"));
		assertEquals(Boolean.FALSE, eval("TestReturnStringOutBuffer(false, new Buffer(10), 10)"));
		for (String s : new String[] { "", "A", "AA", "so...very...hungry!" })
		{
			for (int len : new int[] { 1, 2, 10, 50 })
			{
				String codeTest = String
						.format("Object(TestReturnStringOutBuffer('%s', b = Buffer(%2$d), %2$d), b)",
								s, len);
				Object output = eval(codeTest);
				String s2;
				if (len < s.length() + 1)
					s2 = len < 2 ? "" : s.substring(0, len - 1);
				else
					s2 = s;
				String codeExpect = String.format(
						"Object('%s', Buffer(%d, '%1$s'))", s2, len);
				Object expected = eval(codeExpect);
				assertEquals(expected, output);
			}
		}
	}

	@Test
	public void testDllAsClassMember() {
		assertEquals(12,
			eval("class { CallClass(@x) { .mydll(@x) } mydll: dll int32 jsdi:TestStrLen([in] string str) }('hello, world')")
		);
	}

	@Test
	public void testDllAsInstanceMember() {
		assertEquals(
			-19800725L,
			eval("class { Call(@x) { .mydll(@x) } mydll: dll int64 jsdi:TestInt64(int64 a) }()(-19800725)")
		);
	}

	@Test
	public void testNoModifyReadOnly() {
		// In cSuneido, you can get away with passing read-only objects to the
		// dll marshaller, which happily modifies them. However, it seems like
		// more consistent behaviour is to throw. Why should the marshaller be
		// exempt from the read-only object rule?
		assertThrew(() -> {
			eval("TestHelloWorldOutParam(#())");
		}, SuException.class, "readonly");
		assertThrew(
				() -> {
					eval("TestReturnStatic_Packed_Int8Int8Int16Int32(#(b: 25, c: 1050, d: -875))");
				}, SuException.class, "readonly");
	}

	//
	// TESTS for Win32 exceptions
	//

	@Test
	public void testWin32ExceptionDirect() {
		assertThrew(() -> {
			eval("TestDivideTwoInt32s(10, 0)");
		}, JSDIException.class, "win32 exception: INT_DIVIDE_BY_ZERO");
	}

	@Test
	public void testWin32ExceptionIndirect() {
		assertThrew(() -> {
			eval("(dll void jsdi:TestCopyInt32Value(Int32Wrapper * src, pointer dst))(Object(x: 1), 1)");
		}, JSDIException.class, "win32 exception: ACCESS_VIOLATION");
	}

	@Test
	public void testWin32ExceptionVariableIndirect() {
		assertThrew(() -> {
			eval("TestSumString(Object(inner: Object(inner: 1)))");
		}, JSDIException.class, "win32 exception: ACCESS_VIOLATION");
	}

	//
	// TESTS for regressions
	//

	@Test
	public void testRegression64BitNumber() {
		// This is a test to prevent reappearance of the following bug fixed on
		// 20140820: Object.Add(value, at: index) was silently failing to add
		// the value under the following conditions: index is an integer
		// returned by a 'dll' function returning type 'int64'; index is 0; and
		// the Object in question already has an element at index 0.
		for (String funcName : new String[] { "TestInt64", "TestInt32",
				"TestInt16", "TestInt8" }) {
			String code = String.format(
				"x = Object()\n" +
				"for (k = 10; 1 <= k; --k)\n" +
				"    x.Add(k, at: %s(0))\n" +
				"x",
				funcName
			);
			assertEquals(eval("#(1 2 3 4 5 6 7 8 9 10)"), eval(code));
		}
	}
}
