package suneido.language.jsdi.dll;

import static org.junit.Assert.assertEquals;
import static suneido.util.testing.Throwing.assertThrew;

import java.math.BigDecimal;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.SuException;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.Numbers;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.SimpleContext;
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
		"Recursive_StringSum1",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tRecursive_StringSum2 * inner\n" +
			"\t}",
		"Recursive_StringSum2",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tlong inner\n" +
			"\t}",
		"StringWrapper", "struct { string x }",
		"BufferWrapper", "struct { buffer x }",
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
		"TestHelloWorldOutParam", "dll void jsdi:_TestHelloWorldOutParam(StringWrapper * ptr)",
		"TestHelloWorldOutBuffer", "dll void jsdi:_TestHelloWorldOutBuffer(BufferWrapper * ptr)",
		"TestReturnPtrPtrPtrDoubleAsUInt64", "dll int64 jsdi:_TestReturnPtrPtrPtrDoubleAsUInt64@4(PtrPtrDouble * ptr)",
		"TestSumString", "dll long jsdi:_TestSumString@4(Recursive_StringSum1 * rss)",
		"TestSumResource", "dll long jsdi:_TestSumResource@8(resource res, ResourceWrapper * pres)"
	};

	private static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
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
			new Runnable() { public void run() { eval("TestStrLen(Buffer(0))"); } },
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
		//assertSame(Boolean.FALSE, eval("TestHelloWorldReturn(false)"));
	}
}
