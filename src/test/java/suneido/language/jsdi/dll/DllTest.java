package suneido.language.jsdi.dll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static suneido.util.testing.Throwing.assertThrew;

import java.math.BigDecimal;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.SuException;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.Numbers;
import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.SimpleContext;
import suneido.language.jsdi.type.Structure;
import suneido.util.testing.Assumption;

/**
 * Test for {@link Dll}.
 *
 * @author Victor Schappert
 * @since 20130808
 * @see suneido.language.ParseAndCompileDllTeste
 */
public class DllTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
		CONTEXT = new SimpleContext(NAMED_TYPES);
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_TYPES = {
		"Packed_CharCharShortLong", "struct { char a; char b; short c; long d; }",
		"Recursive_CharCharShortLong2",
			"struct { Packed_CharCharShortLong x; Recursive_CharCharShortLong1 * inner; }",
		"Recursive_CharCharShortLong1",
			"struct { Packed_CharCharShortLong x; Recursive_CharCharShortLong0 * inner; }",
		"Recursive_CharCharShortLong0",
			"struct { Packed_CharCharShortLong x; long inner; }",
		"Recursive_StringSum2",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tRecursive_StringSum1 * inner\n" +
			"\t}",
		"Recursive_StringSum1",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tRecursive_StringSum0 * inner\n" +
			"\t}",
		"Recursive_StringSum0",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tlong inner\n" +
			"\t}",
		"Swap_StringLongLong",
			"struct { string str; long a; long b; }",
		"StringWrapper", "struct { string x }",
		"DoubleWrapper", "struct { double x }",
		"ResourceWrapper", "struct { resource x }",
		"PtrDouble", "struct { DoubleWrapper * x }",
		"PtrPtrDouble", "struct { PtrDouble * x }",
		"TestVoid", "dll void jsdi:_TestVoid@0()",
		"TestChar", "dll char jsdi:_TestChar@4(char a)",
		"TestShort", "dll short jsdi:_TestShort@4(short a)",
		"TestLong", "dll long jsdi:_TestLong@4(long a)",
		"TestInt64", "dll int64 jsdi:_TestInt64@8(int64 a)",
		"TestReturn1_0Float", "dll float jsdi:_TestReturn1_0Float@0()",
		"TestReturn1_0Double", "dll double jsdi:_TestReturn1_0Double@0()",
		"TestFloat", "dll float jsdi:_TestFloat@4(float a)",
		"TestDouble", "dll double jsdi:_TestDouble@8(double a)",
		"TestSumTwoChars", "dll char jsdi:_TestSumTwoChars@8(char a, char b)",
		"TestSumTwoShorts", "dll short jsdi:_TestSumTwoShorts@8(short a, short b)",
		"TestSumTwoLongs", "dll long jsdi:_TestSumTwoLongs@8(long a, long b)",
		"TestSumTwoFloats", "dll float jsdi:_TestSumTwoFloats@8(float a, float b)",
		"TestSumTwoDoubles", "dll double jsdi:_TestSumTwoDoubles@16(double a, double b)",
		"TestSumThreeLongs", "dll long jsdi:_TestSumThreeLongs@12(long a, long b, long c)",
		"TestSumFourLongs", "dll long jsdi:_TestSumFourLongs@16(long a, long b, long c, long d)",
		"TestSumCharPlusInt64", "dll int64 jsdi:_TestSumCharPlusInt64@12(char a, int64 b)",
		"TestSumPackedCharCharShortLong", "dll long jsdi:_TestSumPackedCharCharShortLong@8(Packed_CharCharShortLong x)",
		"TestStrLen", "dll long jsdi:_TestStrLen@4([in] string str)",
		"TestHelloWorldReturn", "dll string jsdi:_TestHelloWorldReturn@4(bool flag)",
		"TestHelloWorldOutParam", "dll void jsdi:_TestHelloWorldOutParam@4(StringWrapper * ptr)",
		"TestHelloWorldOutBuffer", "dll void jsdi:_TestHelloWorldOutBuffer@8(buffer buffer_, long size)",
		"TestHelloWorldOutBufferAsStr", "dll void jsdi:_TestHelloWorldOutBuffer@8(string buffer_, long size)",
		"TestReturnPtrPtrPtrDoubleAsUInt64", "dll int64 jsdi:_TestReturnPtrPtrPtrDoubleAsUInt64@4(PtrPtrDouble * ptr)",
		"TestSumString", "dll long jsdi:_TestSumString@4(Recursive_StringSum1 * rss)",
		"TestSumResource", "dll long jsdi:_TestSumResource@8(resource res, ResourceWrapper * pres)",
		"TestSwap", "dll long jsdi:_TestSwap@4(Swap_StringLongLong * ptr)",
		"TestReturnString", "dll string jsdi:_TestReturnString@4([in] string str)",
		"TestReturnPtrString", "dll string jsdi:_TestReturnPtrString@4(StringWrapper * ptr)",
		"TestReturnStringOutBuffer", "dll string jsdi:_TestReturnStringOutBuffer@12(string str, buffer buffer_, long size)",
		"TestReturnStatic_Packed_CharCharShortLong",
			"dll long jsdi:_TestReturnStatic_Packed_CharCharShortLong@4(Packed_CharCharShortLong * ptr)",
		"TestReturnStatic_Recursive_CharCharShortLong",
			"dll long jsdi:_TestReturnStatic_Recursive_CharCharShortLong@4(Recursive_CharCharShortLong2 * ptr)",
		"TestReturnStatic_Recursive_StringSum",
			"dll long jsdi:_TestReturnStatic_Recursive_StringSum@4(Recursive_StringSum2 * ptr)",
	};

	private static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	private static Structure struct(String src) {
		return (Structure)Compiler.eval(src, CONTEXT);
	}

	private static final String[] NOT_AN_OBJECT = { "50", "true", "false",
			"-123.456", "#19900606.020700000" };

	private static void assertNeedObject(final String code,
			final Class<? extends Throwable> exception, final String pattern) {
		for (final String value : NOT_AN_OBJECT) {
			assertThrew(new Runnable() {
				public void run() {
					eval(String.format(code, value));
				}
			}, exception, pattern);
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
	public void testChar() {
		assertEquals((int)'$', eval("TestChar('$'.Asc())"));
	}

	@Test
	public void testShort() {
		assertEquals(0xfff, eval("TestShort(0xfff)"));
	}

	@Test
	public void testLong() {
		assertEquals(0xcafebabe, eval("TestLong(0xcafebabe)"));
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
	public void testSumTwoChars() {
		assertEquals(3, eval("TestSumTwoChars(1, 2)"));
	}

	@Test
	public void testSumTwoShorts() {
		assertEquals(0x7feb, eval("TestSumTwoShorts(0x7f00, 0xeb)"));
	}

	@Test
	public void testSumTwoLongs() {
		assertEquals(0x7feb1982, eval("TestSumTwoLongs(0x70e01080, 0x0f0b0902)"));
	}

	@Test
	public void testSumTwoFloats() {
		assertEquals(bd(3 * 98765432), eval("TestSumTwoFloats(98765432, 2 * 98765432)"));
	}

	@Test
	public void testSumTwoDoubles() {
		assertEquals(bd(5 * 1234567), eval("TestSumTwoDoubles(3 * 1234567, 2 * 1234567)"));
	}

	@Test
	public void testSumThreeLongs() {
		assertEquals(0x1812feb7, eval("TestSumThreeLongs(0x18000007, 0x00120000, 0x0000feb0)"));
	}

	@Test
	public void testSumFourLongs() {
		assertEquals(-10, eval("TestSumFourLongs(-1, -2, -3, -4)"));
	}

	@Test
	public void testSumCharPlusInt64() {
		assertEquals(0x00000000ffffffffL, eval("TestSumCharPlusInt64(-1, 4 * (1 << 30))"));
	}

	@Test
	public void testSumPackedCharCharShortLong() {
		assertEquals(0, eval("TestSumPackedCharCharShortLong(Object())"));
		assertEquals(
				100,
				eval("TestSumPackedCharCharShortLong(Object(b: 50, c: 1000, d: -950))"));
		assertNeedObject("TestSumPackedCharCharShortLong(%s)",
				JSDIException.class, "can't convert .* to object");
		// In CSuneido, you can get away with passing read-only objects to the
		// dll marshaller, which happily modifies them. However, it seems like
		// more consistent behaviour is to throw. Why should the marshaller be
		// exempt from the read-only object rule?
		assertThrew(new Runnable() {
			public void run() {
				eval("TestSumPackedCharCharShortLong(#())");
			}
		}, SuException.class, "readonly");
		assertThrew(new Runnable() {
			public void run() {
				eval("TestSumPackedCharCharShortLong(#(b: 25, c: 1050, d: -875))");
			}
		}, SuException.class, "readonly");
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
		assertThrew(
			new Runnable() { public void run() { eval("TestStrLen(Buffer(1, 'a'))"); } },
			JSDIException.class, "cannot safely be marshalled"
		);
		assertEquals(0, eval("TestStrLen(Buffer(1))"));
		assertEquals(0, eval("TestStrLen(Buffer(1000))"));
		assertThrew(
				new Runnable() { public void run() { eval("TestStrLen(Buffer(1, 'a'))"); } },
				JSDIException.class, "cannot safely be marshalled"
			);
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

	//
	// TESTS for copying out Structures
	//

	@Test
	public void testStructureCopyOutNull() {
		for (String structName : new String[] { "Packed_CharCharShortLong",
				"Recursive_CharCharShortLong2", "Recursive_CharCharShortLong1",
				"Recursive_CharCharShortLong0", "Recursive_StringSum2",
				"Recursive_StringSum1", "Recursive_StringSum0" }) {
			for (String nullValue : new String[] { "false", "0" }) {
				assertEquals(Boolean.FALSE,
						eval(String.format("%s(%s)", structName, nullValue)));
			}
		}
	}

	@Test
	public void testStructureCopyOutDirect() {
		Object addr = eval("TestReturnStatic_Packed_CharCharShortLong(false)");
		assertFalse(Numbers.isZero(addr));
		assertEquals(addr, eval("TestReturnStatic_Packed_CharCharShortLong(Object(a: 5, d: -1))"));
		String code = String.format("Packed_CharCharShortLong(%d)", addr);
		assertEquals(eval("#(a: 5, b: 0, c: 0, d: -1)"), eval(code));
		assertEquals(addr, eval("TestReturnStatic_Packed_CharCharShortLong(Object(a: 0, b: 5, c: -1))"));
		assertEquals(
				eval("#(a: 0, b: 5, c: -1, d: 0)"),
				struct("Packed_CharCharShortLong").call1(addr));
	}

	@Test
	public void testStructureCopyOutIndirect() {
		Object addr = eval("TestReturnStatic_Recursive_CharCharShortLong(false)");
		assertFalse(Numbers.isZero(addr));
		assertEquals(addr, eval("TestReturnStatic_Recursive_CharCharShortLong(Object(x: Object(a: 5, d: -1)))"));
		final String code0 = String.format("Recursive_CharCharShortLong0(%d)", addr); // doesn't engage indirect copy out
		assertEquals(eval("#(x: #(a: 5, b: 0, c: 0, d: -1), inner: 0)"), eval(code0));
		final String code1 = String.format("Recursive_CharCharShortLong1(%d)", addr); // indirect copy out
		assertEquals(eval("#(x: #(a: 5, b: 0, c: 0, d: -1), inner: false)"), eval(code1));
		final String code2 = String.format("Recursive_CharCharShortLong2(%d)", addr); // indirect copy out
		assertEquals(eval("#(x: #(a: 5, b: 0, c: 0, d: -1), inner: false)"), eval(code2));
		assertEquals(
				addr,
				eval("TestReturnStatic_Recursive_CharCharShortLong(" +
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
	public void testStructureCopyOutVariableIndirect() {
		Object addr = eval("TestReturnStatic_Recursive_StringSum(0)");
		String array = "Object(Object(a: 10, b: 9, c: 8, d: 7), Object(a: 6, b: 5, c: 4, d: 3))";
		String array0 = "#(#(a:0,b:0,c:0,d:0),#(a:0,b:0,c:0,d:0))";
		String hello = "'Olá mundo'";
		assertFalse(Numbers.isZero(addr));
		Object value = eval(String.format("TestReturnStatic_Recursive_StringSum(Object(x: %s, str: %s))", array, hello));
		assertEquals(addr, value);
		for (String s : new String[] { "Recursive_StringSum2", "Recursive_StringSum1" }) {
			assertEquals(
					eval(String.format("Object(x: %s, str: %s, buffer_: false, len: 0, inner: false)", array, hello)),
					eval(String.format("%s(%d)", s, addr))
			);
		}
		value = eval(String.format("TestReturnStatic_Recursive_StringSum(Object(x: %s, str: %s, inner: Object()))", array, hello));
		assertEquals(addr, value);
		for (String s : new String[] { "Recursive_StringSum2/false", "Recursive_StringSum1/0" }) {
			String[] split = s.split("/");
			assertEquals(
					eval(String.format("Object(x: %s, str: %s, buffer_: false, len: 0, " +
							"inner: #(x: %s, str: false, buffer_: false, len: 0, inner: %s))", array, hello, array0, split[1])),
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
				eval(String.format("Object(x: %s, str: %s, buffer_: false, len: 0, " +
						"inner: Object(x: %s, str: 'inner1', buffer_: false, len: 0, " +
						"inner: Object(x: %s, str: 'inner2', buffer_: false, len: 0, inner: 0)))",
						array, hello, array0, array)),
				eval(String.format("Recursive_StringSum2(%d)", addr)));
	}
}
