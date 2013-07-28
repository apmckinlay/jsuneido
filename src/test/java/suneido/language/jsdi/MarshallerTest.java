package suneido.language.jsdi;

import static org.junit.Assert.*;
import static suneido.language.jsdi.MarshallTestUtil.*;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

import suneido.language.jsdi.type.PrimitiveSize;

/**
 * Test for {@link Marshaller}.
 *
 * @author Victor Schappert
 * @since 20130717
 */
@DllInterface
public class MarshallerTest {

	private static byte[] ba(String input) {
		return DatatypeConverter.parseHexBinary(input);
	}

	private static int[] ia(Integer... input) {
		final int N = input.length;
		final int[] output = new int[N];
		for (int i = 0; i < N; ++i) {
			output[i] = input[i].intValue();
		}
		return output;
	}

	private static void simulateNativeSidePutStringInViArray(
			Marshaller marshaller, String string) {
		marshaller.getViArray()[0] = string;
	}

	@Test
	public void testNullMarshaller() {
		MarshallPlan NULL_PLAN = nullPlan();
		final Marshaller NULL_MARSHALLER = NULL_PLAN.makeMarshaller();
		assertThrew(new Runnable() {
			public void run() {
				NULL_MARSHALLER.putChar((byte)'X');
			}
		}, ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testMarshallBool() {
		MarshallPlan mp = directPlan(PrimitiveSize.BOOL);
		Marshaller mr = mp.makeMarshaller();
		mr.putBool(true);
		assertArrayEquals(ba("01000000"), mr.getData()); // little-endian
		mr.rewind();
		assertTrue(mr.getBool());
		mr = mp.makeMarshaller();
		mr.putBool(false);
		assertArrayEquals(ba("00000000"), mr.getData());
		mr.rewind();
		assertFalse(mr.getBool());
	}

	@Test
	public void testMarshallChar() {
		MarshallPlan mp = directPlan(PrimitiveSize.CHAR);
		Marshaller mr = mp.makeMarshaller();
		mr.putChar((byte)0x21);
		assertArrayEquals(ba("21"), mr.getData());
		mr.rewind();
		assertEquals((byte)0x21, mr.getChar());
	}

	@Test
	public void testMarshallShort() {
		MarshallPlan mp = directPlan(PrimitiveSize.SHORT);
		Marshaller mr = mp.makeMarshaller();
		mr.putShort((short)0x1982);
		assertArrayEquals(ba("8219"), mr.getData()); // little-endian
		mr.rewind();
		assertEquals((short)0x1982, mr.getShort());
	}

	@Test
	public void testMarshallLong() {
		MarshallPlan mp = directPlan(PrimitiveSize.LONG);
		Marshaller mr = mp.makeMarshaller();
		mr.putLong(0x19820207);
		assertArrayEquals(ba("07028219"), mr.getData()); // little-endian
		mr.rewind();
		assertEquals(0x19820207, mr.getLong());
	}

	@Test
	public void testMarshallInt64() {
		MarshallPlan mp = directPlan(PrimitiveSize.INT64);
		Marshaller mr = mp.makeMarshaller();
		mr.putInt64(0x0123456789abcdefL);
		assertArrayEquals(ba("efcdab8967452301"), mr.getData()); // little-endian
		mr.rewind();
		assertEquals(0x0123456789abcdefL, mr.getInt64());
	}

	@Test
	public void testMarshallFloat() {
		MarshallPlan mp = directPlan(PrimitiveSize.FLOAT);
		Marshaller mr = mp.makeMarshaller();
		mr.putFloat(1.0f); // IEEE 32-bit float binary rep => 0x3f800000
		assertArrayEquals(ba("0000803f"), mr.getData()); // little-endian
		mr.rewind();
		assertTrue(1.0f == mr.getFloat());
	}

	@Test
	public void testMarshallDouble() {
		MarshallPlan mp = directPlan(PrimitiveSize.DOUBLE);
		Marshaller mr = mp.makeMarshaller();
		mr.putDouble(19820207.0); // IEEE 64-bit double binary rep => 0x4172e6eaf0000000
		assertArrayEquals(ba("000000f0eae67241"), mr.getData()); // little-endian
		mr.rewind();
		assertTrue(19820207.0 == mr.getDouble());
	}

	@Test
	public void testMarshallPtrNotNull() {
		// Plan for a "C" char *
		MarshallPlan mp = pointerPlan(PrimitiveSize.CHAR);
		Marshaller mr = mp.makeMarshaller();
		mr.putPtr();
		mr.putChar((byte)0xff);
		assertArrayEquals(ba("00000000ff"), mr.getData());
		assertArrayEquals(ia(0, PrimitiveSize.POINTER), mr.getPtrArray());
		mr.rewind();
		// At the moment, this looks like a NULL pointer because we judge
		// NULLness coming *out* of the Marshaller based on whether the pointer
		// is set to zero.
		assertTrue(mr.isPtrNull());
		// Simulate the native side setting the pointer to a non-null value.
		mr.rewind();
		mr.putChar((byte)1);
		mr.rewind();
		assertFalse(mr.isPtrNull());
		assertEquals((byte)0xff, mr.getChar());
	}

	@Test
	public void testMarshallPtrNull() {
		MarshallPlan mp = pointerPlan(PrimitiveSize.INT64);
		Marshaller mr = mp.makeMarshaller();
		mr.putNullPtr();
		mr.putInt64(0x1982020719900606L);
		assertArrayEquals(ba("000000000606901907028219"), mr.getData());
		assertArrayEquals(ia(0, -1), mr.getPtrArray());
		mr.rewind();
		assertTrue(mr.isPtrNull());
		assertEquals(0x1982020719900606L, mr.getInt64());
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testMarshallStringDirectZeroTerminated_OverflowString() {
		MarshallPlan mp = directPlan(PrimitiveSize.CHAR);
		Marshaller mr = mp.makeMarshaller();
		// The reason the character count has to be 3 is that the marshaller
		// copies the character count - 1, assuming that the character at count
		// is already 0. So a count of 2 doesn't overflow because only one
		// character gets copied, it just gets copied into the spot which should
		// be occupied by the zero-terminator...
		mr.putZeroTerminatedStringDirect("ab", 3);
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testMarshallStringDirectZeroTerminated_OverflowBuffer() {
		MarshallPlan mp = directPlan(PrimitiveSize.CHAR);
		Marshaller mr = mp.makeMarshaller();
		Buffer b = new Buffer(2, "ab");
		mr.putZeroTerminatedStringDirect(b , 3);
	}

	@Test
	public void testMarshallStringDirectZeroTerminated() {
		final int LEN = 3;
		//
		// With String
		//
		MarshallPlan mp = directPlan(LEN * PrimitiveSize.CHAR);
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect("", LEN);
			assertArrayEquals(ba("000000"), mr.getData());
			mr.rewind();
			assertEquals("", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect("a", LEN);
			assertArrayEquals(ba("610000"), mr.getData());
			mr.rewind();
			assertEquals("a", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect("01", LEN);
			assertArrayEquals(ba("303100"), mr.getData());
			mr.rewind();
			assertEquals("01", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect("ABC", LEN);
			assertArrayEquals(ba("414200"), mr.getData());
			mr.rewind();
			assertEquals("AB", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			// This is just to test the top-level branch in the method when
			// numChars is 0
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect("@@@", LEN);
			assertArrayEquals(ba("404000"), mr.getData());
			mr.rewind();
			assertEquals("", mr.getZeroTerminatedStringDirect(0));
		}
		//
		// With Buffer
		//
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect(new Buffer(1, ""), LEN);
			assertArrayEquals(ba("000000"), mr.getData());
			mr.rewind();
			assertEquals("", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect(new Buffer(1, "a"), LEN);
			assertArrayEquals(ba("610000"), mr.getData());
			mr.rewind();
			assertEquals("a", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect(new Buffer(2, "01"), LEN);
			assertArrayEquals(ba("303100"), mr.getData());
			mr.rewind();
			assertEquals("01", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putZeroTerminatedStringDirect(new Buffer(3, "ABC"), LEN);
			assertArrayEquals(ba("414200"), mr.getData());
			mr.rewind();
			assertEquals("AB", mr.getZeroTerminatedStringDirect(LEN));
		}
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testMarshallStringDirectNonZeroTerminated_OverflowString() {
		MarshallPlan mp = directPlan(PrimitiveSize.CHAR);
		Marshaller mr = mp.makeMarshaller();
		mr.putNonZeroTerminatedStringDirect("ab", 2);
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testMarshallStringDirectNonZeroTerminated_OverflowBuffer() {
		MarshallPlan mp = directPlan(PrimitiveSize.CHAR);
		Marshaller mr = mp.makeMarshaller();
		Buffer b = new Buffer(2, "ab");
		mr.putNonZeroTerminatedStringDirect(b, 2);
	}

	@Test
	public void testMarshallStringDirectNonZeroTerminated() {
		final int LEN = 2;
		//
		// With String Input
		//
		MarshallPlan mp = directPlan(LEN * PrimitiveSize.CHAR);
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putNonZeroTerminatedStringDirect("", LEN);
			assertArrayEquals(ba("0000"), mr.getData());
			mr.rewind();
			Buffer b1 = mr.getNonZeroTerminatedStringDirect(LEN, null);
			// Have to compare Buffer on lhs, String on rhs or comparison fails.
			// Note this technically breaks the symmetry requirement of
			// Object.equals(), but it is how Ops.is_() is implemented.
			assertEquals(b1, "\u0000\u0000");
			assertEquals(LEN, b1.size());
			Buffer b2 = new Buffer(100, "abc");
			mr.rewind();
			Buffer b3 = mr.getNonZeroTerminatedStringDirect(LEN, b2);
			assertSame(b2, b3);
			assertEquals(b1, b3);
			Buffer b4 = new Buffer(1, "x");
			mr.rewind();
			Buffer b5 = mr.getNonZeroTerminatedStringDirect(LEN, b4);
			assertNotSame(b4, b5);
			assertEquals(b1, b5);
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putNonZeroTerminatedStringDirect("a", LEN);
			assertArrayEquals(ba("6100"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "a\u0000");
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putNonZeroTerminatedStringDirect("01", LEN);
			assertArrayEquals(ba("3031"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "01");
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putNonZeroTerminatedStringDirect("ABC", LEN);
			assertArrayEquals(ba("4142"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "AB");
		}
		//
		// With Buffer Input
		//
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putNonZeroTerminatedStringDirect(new Buffer(1, ""), LEN);
			assertArrayEquals(ba("0000"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "\u0000\u0000");
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putNonZeroTerminatedStringDirect(new Buffer(1, "a"), LEN);
			assertArrayEquals(ba("6100"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "a\u0000");
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putNonZeroTerminatedStringDirect(new Buffer(2, "01"), LEN);
			assertArrayEquals(ba("3031"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "01");
		}
		{
			Marshaller mr = mp.makeMarshaller();
			mr.putNonZeroTerminatedStringDirect(new Buffer(3, "ABC"), LEN);
			assertArrayEquals(ba("4142"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "AB");
		}
	}

	@Test
	public void testMarshallStringIndirectPtrNull_ExpectString() {
		MarshallPlan mp = variableIndirectPlan();
		Marshaller mr = mp.makeMarshaller();
		mr.putNullStringPtr(true);
		assertTrue(Arrays.equals(new boolean[] { true }, mr.getViInstArray()));
		mr.rewind();
		assertEquals(mr.getStringPtr(), Boolean.FALSE);
		mr.rewind();
		assertEquals(mr.getStringPtrMaybeByteArray(new Buffer(0, "")), Boolean.FALSE);
		mr.rewind();
	}

	@Test
	public void testMarshallStringIndirectPtrNull_ExpectByteArray() {
		MarshallPlan mp = variableIndirectPlan();
		Marshaller mr = mp.makeMarshaller();
		mr.putNullStringPtr(false);
		mr.rewind();
		assertEquals(mr.getStringPtr(), Boolean.FALSE);
		mr.rewind();
		assertEquals(mr.getStringPtrMaybeByteArray(new Buffer(0, "")), Boolean.FALSE);
		mr.rewind();
		assertEquals(mr.getStringPtrAlwaysByteArray(new Buffer(0, "")), Boolean.FALSE);
	}

	@Test
	public void testMarshallStringIndirectStringBackToString() {
		final String IN = "Quis custodiet ipsos custodes?";
		final String OUT = "Cauta est et ab illis incipit uxor.";
		MarshallPlan mp = variableIndirectPlan();
		Marshaller mr = mp.makeMarshaller();
		mr.putStringPtr(IN, true);
		mr.rewind();
		simulateNativeSidePutStringInViArray(mr, OUT);
		String string = (String)mr.getStringPtr();
		assertEquals(OUT, string);
		mr.rewind();
		string = (String)mr.getStringPtrMaybeByteArray(new Buffer(0, ""));
		assertEquals(OUT, string);
	}

	@Test
	public void testMarshallStringIndirectByteArrayBackToByteArray() {
		final String IN =  "And the silken sad uncertain rustling";
		final String OUT = "of each purple curtainXXXXXXXXXXXXXXX".replace('X', '\u0000');
		final Buffer IN_ = new Buffer(IN.length(), IN);
		final Buffer EXPECT = new Buffer(IN.length(), OUT);
		MarshallPlan mp = variableIndirectPlan();
		Buffer OUT_;
		Marshaller mr = mp.makeMarshaller();
		mr.putStringPtr(IN_, false);
		// Simulate native side changing the contents of the byte buffer without
		// changing the size.
		Buffer.copyStr(OUT, IN_.getInternalData(), 0, OUT.length());
		mr.rewind();
		Object C = mr.getStringPtrMaybeByteArray(IN_);
		assertSame(IN_, C);
		assertEquals(EXPECT, C);
		mr.rewind();
		Object D = mr.getStringPtrAlwaysByteArray(IN_);
		assertSame(IN_, D);
		assertEquals(EXPECT, D);
		// Cases 'E' and 'F' should never happen unless the Suneido programmer
		// is doing silly things in concurrent threads...
		OUT_ = new Buffer(IN.length(), "");
		mr.rewind();
		Object E = mr.getStringPtrMaybeByteArray(OUT_);
		assertSame(OUT_, E);
		assertFalse(EXPECT.equals(E));
		assertEquals(new Buffer(IN.length(), ""), OUT_);
		mr.rewind();
		Object F = mr.getStringPtrAlwaysByteArray(OUT_);
		assertSame(OUT_, F);
		assertFalse(EXPECT.equals(F));
		assertEquals(new Buffer(IN.length(), ""), OUT_);
		// Cases 'G' and 'H' should also never happen. The rationale for
		// returning a new Buffer is just to make sure the marshaller returns a
		// non-null value
		mr.rewind();
		Object G = mr.getStringPtrAlwaysByteArray(null);
		assertNotSame(IN_, G);
		assertEquals(EXPECT, G);
		mr.rewind();
		Object H = mr.getStringPtr();
		assertTrue(H instanceof Buffer);
		assertEquals(EXPECT, H);
	}

	@Test
	public void testSkipBasicArrayElements_Single() {
		MarshallPlan mp = directPlan(PrimitiveSize.CHAR);
		final Marshaller mr = mp.makeMarshaller();
		mr.skipBasicArrayElements(1);
		assertThrew(new Runnable() {
			public void run() {
				mr.putChar((byte) 'a');
			}
		}, ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testSkipBasicArrayElements_Multiple() {
		final int NUM_ELEMS = 3;
		MarshallPlan mp = arrayPlan(PrimitiveSize.LONG, NUM_ELEMS);
		final Marshaller mr = mp.makeMarshaller();
		for (int k = 0; k < NUM_ELEMS; ++k) {
			mr.skipBasicArrayElements(1);
		}
		assertThrew(new Runnable() {
			public void run() {
				mr.putLong(1);
			}
		}, ArrayIndexOutOfBoundsException.class);
		mr.rewind();
		mr.skipBasicArrayElements(NUM_ELEMS);
		assertThrew(new Runnable() {
			public void run() {
				mr.putLong(1);
			}
		}, ArrayIndexOutOfBoundsException.class);
		mr.rewind();
		mr.putLong(9);
		mr.skipBasicArrayElements(1);
		mr.putLong(19);
		assertThrew(new Runnable() {
			public void run() {
				mr.putLong(6);
			}
		}, ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testSkipComplexArrayElements_Single() {
		MarshallPlan cp = compoundPlan(1, PrimitiveSize.FLOAT, PrimitiveSize.BOOL);
		ElementSkipper skipper = new ElementSkipper(2, 0);
		final Marshaller mr = cp.makeMarshaller();
		mr.skipComplexArrayElements(skipper);
		assertThrew(new Runnable() {
			public void run() {
				mr.putChar((byte) 'a');
			}
		}, ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testSkipComplexArrayElements_Multiple() {
		final int NUM_ELEMS = 3;
		MarshallPlan cp = compoundPlan(3, PrimitiveSize.FLOAT,
				PrimitiveSize.BOOL);
		final Marshaller mr = cp.makeMarshaller();
		ElementSkipper skipper_1 = new ElementSkipper(2, 0);
		ElementSkipper skipper_3 = new ElementSkipper(6, 0);
		for (int k = 0; k < NUM_ELEMS; ++k) {
			mr.skipComplexArrayElements(skipper_1);
		}
		assertThrew(new Runnable() {
			public void run() {
				mr.putFloat(1.0f);
			}
		}, ArrayIndexOutOfBoundsException.class);
		mr.rewind();
		mr.skipComplexArrayElements(skipper_3);
		assertThrew(new Runnable() {
			public void run() {
				mr.putFloat(1.0f);
			}
		}, ArrayIndexOutOfBoundsException.class);
		mr.rewind();
		mr.putFloat(091906.0f);
		mr.skipBasicArrayElements(1);
		mr.putFloat(890714.0f);
		mr.putBool(true);
		mr.skipComplexArrayElements(skipper_1);
		assertThrew(new Runnable() {
			public void run() {
				mr.putChar((byte)'a');
			}
		}, ArrayIndexOutOfBoundsException.class);
	}
}
