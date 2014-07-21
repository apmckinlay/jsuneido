/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static suneido.jsdi.VariableIndirectInstruction.NO_ACTION;
import static suneido.jsdi.VariableIndirectInstruction.RETURN_JAVA_STRING;
import static suneido.jsdi.VariableIndirectInstruction.RETURN_RESOURCE;
import static suneido.jsdi.abi.x86.MarshallTestUtilX86.arrayPlan;
import static suneido.jsdi.abi.x86.MarshallTestUtilX86.compoundPlan;
import static suneido.jsdi.abi.x86.MarshallTestUtilX86.directPlan;
import static suneido.jsdi.abi.x86.MarshallTestUtilX86.nullPlan;
import static suneido.jsdi.abi.x86.MarshallTestUtilX86.pointerPlan;
import static suneido.jsdi.abi.x86.MarshallTestUtilX86.variableIndirectPlan;
import static suneido.jsdi.abi.x86.MarshallTestUtilX86.variableIndirectPlan2;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.SuInternalError;
import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.ElementSkipper;
import suneido.jsdi.JSDIException;
import suneido.jsdi.PrimitiveSize;
import suneido.jsdi.VariableIndirectInstruction;
import suneido.jsdi.abi.x86.MarshallPlanX86;
import suneido.jsdi.abi.x86.MarshallerX86;
import suneido.util.testing.Assumption;

/**
 * Test for {@link MarshallerX86}.
 *
 * @author Victor Schappert
 * @since 20130717
 */
@DllInterface
public class MarshallerX86Test {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
	}

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
			MarshallerX86 marshaller, String string) {
		marshaller.getViArray()[0] = string;
	}

	@Test
	public void testNullMarshaller() {
		MarshallPlanX86 NULL_PLAN = nullPlan();
		final MarshallerX86 NULL_MARSHALLER = NULL_PLAN.makeMarshallerX86();
		assertThrew(() -> { NULL_MARSHALLER.putInt8((byte)'X'); },
				ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testMarshallBool() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.BOOL);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putBool(true);
		assertArrayEquals(ba("01000000"), mr.getData()); // little-endian
		mr.rewind();
		assertTrue(mr.getBool());
		mr = mp.makeMarshallerX86();
		mr.putBool(false);
		assertArrayEquals(ba("00000000"), mr.getData());
		mr.rewind();
		assertFalse(mr.getBool());
	}

	@Test
	public void testMarshallChar() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT8);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putInt8((byte)0x21);
		assertArrayEquals(ba("21"), mr.getData());
		mr.rewind();
		assertEquals((byte)0x21, mr.getInt8());
	}

	@Test
	public void testMarshallShort() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT16);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putInt16((short)0x1982);
		assertArrayEquals(ba("8219"), mr.getData()); // little-endian
		mr.rewind();
		assertEquals((short)0x1982, mr.getInt16());
	}

	@Test
	public void testMarshallLong() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT32);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putInt32(0x19820207);
		assertArrayEquals(ba("07028219"), mr.getData()); // little-endian
		mr.rewind();
		assertEquals(0x19820207, mr.getInt32());
	}

	@Test
	public void testMarshallInt64() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT64);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putInt64(0x0123456789abcdefL);
		assertArrayEquals(ba("efcdab8967452301"), mr.getData()); // little-endian
		mr.rewind();
		assertEquals(0x0123456789abcdefL, mr.getInt64());
	}

	@Test
	public void testMarshallFloat() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.FLOAT);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putFloat(1.0f); // IEEE 32-bit float binary rep => 0x3f800000
		assertArrayEquals(ba("0000803f"), mr.getData()); // little-endian
		mr.rewind();
		assertTrue(1.0f == mr.getFloat());
	}

	@Test
	public void testMarshallDouble() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.DOUBLE);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putDouble(19820207.0); // IEEE 64-bit double binary rep => 0x4172e6eaf0000000
		assertArrayEquals(ba("000000f0eae67241"), mr.getData()); // little-endian
		mr.rewind();
		assertTrue(19820207.0 == mr.getDouble());
	}

	@Test
	public void testMarshallPtrNotNull() {
		// Plan for a "C" char *
		MarshallPlanX86 mp = pointerPlan(PrimitiveSize.INT8);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putPtr();
		mr.putInt8((byte)0xff);
		assertArrayEquals(ba("00000000ff"), mr.getData());
		assertArrayEquals(ia(0, PrimitiveSize.POINTER), mr.getPtrArray());
		mr.rewind();
		// At the moment, this looks like a NULL pointer because we judge
		// NULLness coming *out* of the Marshaller based on whether the pointer
		// is set to zero.
		assertTrue(mr.isPtrNull());
		// Simulate the native side setting the pointer to a non-null value.
		mr.rewind();
		mr.putInt8((byte)1);
		mr.rewind();
		assertFalse(mr.isPtrNull());
		assertEquals((byte)0xff, mr.getInt8());
	}

	@Test
	public void testMarshallPtrNull() {
		MarshallPlanX86 mp = pointerPlan(PrimitiveSize.INT64);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putNullPtr();
		mr.putInt64(0x1982020719900606L);
		assertArrayEquals(ba("000000000606901907028219"), mr.getData());
		assertArrayEquals(ia(0, -1), mr.getPtrArray());
		mr.rewind();
		assertTrue(mr.isPtrNull());
		assertEquals(0x1982020719900606L, mr.getInt64());
	}

	@Test
	public void testMarshallPtr_CopyPtrArray() {
		MarshallPlanX86 mp = pointerPlan(PrimitiveSize.INT8, PrimitiveSize.INT16,
				PrimitiveSize.INT32, PrimitiveSize.INT64);
		MarshallerX86 mr = mp.makeMarshallerX86();
		int[] ptrArray = mr.getPtrArray();
		int[] ptrArrayCopy = Arrays.copyOf(ptrArray, ptrArray.length);
		mr.putPtr();
		assertSame(ptrArray, mr.getPtrArray());
		assertArrayEquals(ptrArrayCopy, mr.getPtrArray());
		mr.putPtr();
		assertSame(ptrArray, mr.getPtrArray());
		assertArrayEquals(ptrArrayCopy, mr.getPtrArray());
		mr.putNullPtr();
		assertNotSame(ptrArray, mr.getPtrArray());
		assertArrayEquals(ptrArrayCopy, ptrArray);
		assertFalse(Arrays.equals(ptrArrayCopy, mr.getPtrArray()));
		mr.putNullPtr();
		assertNotSame(ptrArray, mr.getPtrArray());
		assertArrayEquals(ptrArrayCopy, ptrArray);
		assertFalse(Arrays.equals(ptrArrayCopy, mr.getPtrArray()));
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testMarshallStringDirectZeroTerminated_OverflowString() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT8);
		MarshallerX86 mr = mp.makeMarshallerX86();
		// The reason the character count has to be 3 is that the marshaller
		// copies the character count - 1, assuming that the character at count
		// is already 0. So a count of 2 doesn't overflow because only one
		// character gets copied, it just gets copied into the spot which should
		// be occupied by the zero-terminator...
		mr.putZeroTerminatedStringDirect("ab", 3);
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testMarshallStringDirectZeroTerminated_OverflowBuffer() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT8);
		MarshallerX86 mr = mp.makeMarshallerX86();
		Buffer b = new Buffer(2, "ab");
		mr.putZeroTerminatedStringDirect(b , 3);
	}

	@Test
	public void testMarshallStringDirectZeroTerminated() {
		final int LEN = 3;
		//
		// With String
		//
		MarshallPlanX86 mp = directPlan(LEN * PrimitiveSize.INT8);
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect("", LEN);
			assertArrayEquals(ba("000000"), mr.getData());
			mr.rewind();
			assertEquals("", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect("a", LEN);
			assertArrayEquals(ba("610000"), mr.getData());
			mr.rewind();
			assertEquals("a", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect("01", LEN);
			assertArrayEquals(ba("303100"), mr.getData());
			mr.rewind();
			assertEquals("01", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect("ABC", LEN);
			assertArrayEquals(ba("414200"), mr.getData());
			mr.rewind();
			assertEquals("AB", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			// This is just to test the top-level branch in the method when
			// numChars is 0
			final MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect("@@@", LEN);
			assertArrayEquals(ba("404000"), mr.getData());
			mr.rewind();
			assertThrew(() -> {	mr.getZeroTerminatedStringDirect(0); },
					JSDIException.class);
		}
		//
		// With Buffer
		//
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect(new Buffer(1, ""), LEN);
			assertArrayEquals(ba("000000"), mr.getData());
			mr.rewind();
			assertEquals("", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect(new Buffer(1, "a"), LEN);
			assertArrayEquals(ba("610000"), mr.getData());
			mr.rewind();
			assertEquals("a", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect(new Buffer(2, "01"), LEN);
			assertArrayEquals(ba("303100"), mr.getData());
			mr.rewind();
			assertEquals("01", mr.getZeroTerminatedStringDirect(LEN));
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putZeroTerminatedStringDirect(new Buffer(3, "ABC"), LEN);
			assertArrayEquals(ba("414200"), mr.getData());
			mr.rewind();
			assertEquals("AB", mr.getZeroTerminatedStringDirect(LEN));
		}
	}

	@Test
	public void testMarshallStringDirectZeroTerminated_NoZero() {
		final int LEN = 3;
		MarshallPlanX86 mp = directPlan(LEN);
		final MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putNonZeroTerminatedStringDirect("abc", 3);
		mr.rewind();
		assertThrew(() -> { mr.getZeroTerminatedStringDirect(LEN); },
				JSDIException.class);
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testMarshallStringDirectNonZeroTerminated_OverflowString() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT8);
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putNonZeroTerminatedStringDirect("ab", 2);
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testMarshallStringDirectNonZeroTerminated_OverflowBuffer() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT8);
		MarshallerX86 mr = mp.makeMarshallerX86();
		Buffer b = new Buffer(2, "ab");
		mr.putNonZeroTerminatedStringDirect(b, 2);
	}

	@Test
	public void testMarshallStringDirectNonZeroTerminated() {
		final int LEN = 2;
		//
		// With String Input
		//
		MarshallPlanX86 mp = directPlan(LEN * PrimitiveSize.INT8);
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNonZeroTerminatedStringDirect("", LEN);
			assertArrayEquals(ba("0000"), mr.getData());
			mr.rewind();
			Buffer b1 = mr.getNonZeroTerminatedStringDirect(LEN, null);
			// Have to compare Buffer on lhs, String on rhs or comparison fails.
			// Note this technically breaks the symmetry requirement of
			// Object.equals(), but it is how Ops.is_() is implemented.
			assertEquals(b1, "\u0000\u0000");
			assertEquals(LEN, b1.length());
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
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNonZeroTerminatedStringDirect("a", LEN);
			assertArrayEquals(ba("6100"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "a\u0000");
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNonZeroTerminatedStringDirect("01", LEN);
			assertArrayEquals(ba("3031"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "01");
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNonZeroTerminatedStringDirect("ABC", LEN);
			assertArrayEquals(ba("4142"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "AB");
		}
		//
		// With Buffer Input
		//
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNonZeroTerminatedStringDirect(new Buffer(1, ""), LEN);
			assertArrayEquals(ba("0000"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "\u0000\u0000");
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNonZeroTerminatedStringDirect(new Buffer(1, "a"), LEN);
			assertArrayEquals(ba("6100"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "a\u0000");
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNonZeroTerminatedStringDirect(new Buffer(2, "01"), LEN);
			assertArrayEquals(ba("3031"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "01");
		}
		{
			MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNonZeroTerminatedStringDirect(new Buffer(3, "ABC"), LEN);
			assertArrayEquals(ba("4142"), mr.getData());
			mr.rewind();
			assertEquals(mr.getNonZeroTerminatedStringDirect(LEN, null), "AB");
		}
	}

	@Test
	public void testMarshallStringIndirectPtrNull_ExpectString() {
		MarshallPlanX86 mp = variableIndirectPlan();
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putNullStringPtr(RETURN_JAVA_STRING);
		assertArrayEquals(
				new int[] { RETURN_JAVA_STRING.ordinal() },
				mr.getViInstArray());
		mr.rewind();
		assertEquals(mr.getStringPtr(), Boolean.FALSE);
		mr.rewind();
		assertEquals(mr.getStringPtrMaybeByteArray(new Buffer(0, "")), Boolean.FALSE);
		mr.rewind();
	}

	@Test
	public void testMarshallStringIndirectPtrNull_ExpectByteArray() {
		MarshallPlanX86 mp = variableIndirectPlan();
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putNullStringPtr(NO_ACTION);
		assertArrayEquals(
				new int[] { NO_ACTION.ordinal() },
				mr.getViInstArray());
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
		MarshallPlanX86 mp = variableIndirectPlan();
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putStringPtr(IN, RETURN_JAVA_STRING);
		mr.rewind();
		simulateNativeSidePutStringInViArray(mr, OUT);
		String string = (String)mr.getStringPtr();
		assertEquals(OUT, string);
		mr.rewind();
		string = (String)mr.getStringPtrMaybeByteArray(new Buffer(0, ""));
		assertEquals(OUT, string);
	}

	@Test
	public void testMarshallStringIndirectPtrNull_PosAdvance() {
		// Test of bug found 20130730 in which inserting a null variable
		// indirect pointer into the marshaller wasn't advancing the position.
		MarshallPlanX86 mp = variableIndirectPlan();
		for (VariableIndirectInstruction i : VariableIndirectInstruction.values()) {
			final MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putNullStringPtr(i);
			assertThrew(() -> { mr.putInt8((byte)0); },
				ArrayIndexOutOfBoundsException.class);
		}
	}

	@Test
	public void testMarshallStringIndirectByteArrayBackToByteArray() {
		final String IN =  "And the silken sad uncertain rustling";
		final String OUT = "of each purple curtainXXXXXXXXXXXXXXX".replace('X', '\u0000');
		final Buffer IN_ = new Buffer(IN.length(), IN);
		final Buffer EXPECT = new Buffer(IN.length(), OUT);
		MarshallPlanX86 mp = variableIndirectPlan();
		Buffer OUT_;
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putStringPtr(IN_, NO_ACTION);
		// Simulate native side changing the contents of the byte buffer without
		// changing the size.
		Buffer.copyStr(OUT, IN_.getInternalData(), 0, OUT.length());
		mr.rewind();
		Object C = mr.getStringPtrMaybeByteArray(IN_);
		assertSame(IN_, C);
		assertFalse(EXPECT.equals(C)); // because C was truncated at first NUL
		assertEquals(C, "of each purple curtain");
		mr.rewind();
		Object D = mr.getStringPtrAlwaysByteArray(IN_);
		assertSame(IN_, D);
		// Cases 'E' and 'F' should never happen unless the Suneido programmer
		// is doing silly things in concurrent threads...
		OUT_ = new Buffer(IN.length(), "");
		mr.rewind();
		Object E = mr.getStringPtrMaybeByteArray(OUT_);
		assertSame(OUT_, E);
		assertFalse(EXPECT.equals(E));
		assertEquals(0, OUT_.length());
		assertFalse(new Buffer(IN.length(), "").equals(OUT_));
		mr.rewind();
		Object F = mr.getStringPtrAlwaysByteArray(OUT_);
		assertSame(OUT_, F);
		assertFalse(EXPECT.equals(F));
		assertEquals(0, OUT_.length());
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
	public void testMarshallIntResourceToIntResource() {
		MarshallPlanX86 mp = variableIndirectPlan();
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putINTRESOURCE((short) 17);
		mr.rewind();
		// Simulate native side inserting the integer equivalent of the
		// INTRESOURCE into the variable indirect array
		mr.getViArray()[0] = new Integer(17);
		assertEquals(17, mr.getResource());
	}

	@Test
	public void testMarshallIntResourceToString() {
		MarshallPlanX86 mp = variableIndirectPlan();
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putINTRESOURCE((short) 0xffff);
		mr.rewind();
		final String str = "simulation of native side replacing INTRESOURCE with String";
		mr.getViArray()[0] = str;
		assertEquals(str, mr.getResource());
	}

	@Test
	public void testMarshallStringResourceToIntResource() {
		MarshallPlanX86 mp = variableIndirectPlan();
		MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putStringPtr("string resource", RETURN_RESOURCE);
		mr.rewind();
		// Simulate native side inserting an INTRESOURCE into the variable
		// indirect array
		mr.getViArray()[0] = new Integer(64000);
		assertEquals(64000, mr.getResource());
	}

	@Test
	public void testMarshallStringResourceToStringResource() {
		MarshallPlanX86 mp = variableIndirectPlan();
		final MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putStringPtr("res", RETURN_RESOURCE);
		mr.rewind();
		// This should throw, because outgoing variable indirect array was a
		// byte[]. The native side is supposed to replace it with a String, but
		// since we haven't invoked the native side, that didn't happen.
		assertThrew(mr::getResource, SuInternalError.class);
		// Simulate native side re-converting to String
		mr.getViArray()[0] = "res";
		mr.rewind();
		assertEquals("res", mr.getResource());
	}

	@Test
	public void testMarshallResourceNullOutputException() {
		MarshallPlanX86 mp = variableIndirectPlan();
		final MarshallerX86 mr = mp.makeMarshallerX86();
		mr.putStringPtr("anything", RETURN_RESOURCE);
		mr.rewind();
		// Simulate a NULL somehow getting into the variable indirect array.
		mr.getViArray()[0] = null;
		assertThrew(mr::getResource, SuInternalError.class);
	}

	@Test
	public void testSkipBasicArrayElements_Single() {
		MarshallPlanX86 mp = directPlan(PrimitiveSize.INT8);
		final MarshallerX86 mr = mp.makeMarshallerX86();
		mr.skipBasicArrayElements(1);
		assertThrew(() -> { mr.putInt8((byte) 'a'); },
				ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testSkipBasicArrayElements_Multiple() {
		final int NUM_ELEMS = 3;
		MarshallPlanX86 mp = arrayPlan(PrimitiveSize.INT32, NUM_ELEMS);
		final MarshallerX86 mr = mp.makeMarshallerX86();
		for (int k = 0; k < NUM_ELEMS; ++k) {
			mr.skipBasicArrayElements(1);
		}
		assertThrew(() -> { mr.putInt32(1); },
				ArrayIndexOutOfBoundsException.class);
		mr.rewind();
		mr.skipBasicArrayElements(NUM_ELEMS);
		assertThrew(() -> { mr.putInt32(1); },
				ArrayIndexOutOfBoundsException.class);
		mr.rewind();
		mr.putInt32(9);
		mr.skipBasicArrayElements(1);
		mr.putInt32(19);
		assertThrew(() -> { mr.putInt32(6); },
				ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testSkipComplexArrayElements_Single() {
		MarshallPlanX86 cp = compoundPlan(1, PrimitiveSize.FLOAT, PrimitiveSize.BOOL);
		ElementSkipper skipper = new ElementSkipper(2, 0);
		final MarshallerX86 mr = cp.makeMarshallerX86();
		mr.skipComplexElement(skipper);
		assertThrew(() -> { mr.putInt8((byte) 'a'); },
				ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testSkipComplexArrayElements_Multiple() {
		final int NUM_ELEMS = 3;
		MarshallPlanX86 cp = compoundPlan(3, PrimitiveSize.FLOAT,
				PrimitiveSize.BOOL);
		final MarshallerX86 mr = cp.makeMarshallerX86();
		ElementSkipper skipper_1 = new ElementSkipper(2, 0);
		ElementSkipper skipper_3 = new ElementSkipper(6, 0);
		for (int k = 0; k < NUM_ELEMS; ++k) {
			mr.skipComplexElement(skipper_1);
		}
		assertThrew(() -> { mr.putFloat(1.0f); },
				ArrayIndexOutOfBoundsException.class);
		mr.rewind();
		mr.skipComplexElement(skipper_3);
		assertThrew(() -> { mr.putFloat(1.0f); },
				ArrayIndexOutOfBoundsException.class);
		mr.rewind();
		mr.putFloat(091906.0f);
		mr.skipBasicArrayElements(1);
		mr.putFloat(890714.0f);
		mr.putBool(true);
		mr.skipComplexElement(skipper_1);
		assertThrew(() -> { mr.putInt8((byte)'a'); },
				ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testSkipStringPtr_Single() {
		MarshallPlanX86 mp = variableIndirectPlan();
		final MarshallerX86 mr = mp.makeMarshallerX86();
		mr.skipStringPtr();
		assertThrew(() -> { mr.putInt8((byte) 'a'); },
				ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testSkipStringPtr_Multiple() {
		MarshallPlanX86 mp = variableIndirectPlan2();
		final MarshallerX86 mr = mp.makeMarshallerX86();
		mr.skipStringPtr();
		mr.putStringPtr("x", RETURN_JAVA_STRING);
		assertArrayEquals(new Object[] { null, new byte[] { (byte) 'x', 0 } },
				mr.getViArray());
		assertArrayEquals(new int[] { 0, RETURN_JAVA_STRING.ordinal() },
				mr.getViInstArray());
		assertThrew(() -> { mr.putNullStringPtr(RETURN_RESOURCE); },
				ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void testPutBool() {
		// This is a regression test for a bug found 20130809: when you
		// putBool(false), it wasn't advancing the posIndex.
		for (boolean b : new boolean[] { false, true }) {
			MarshallPlanX86 mp = directPlan(PrimitiveSize.BOOL);
			final MarshallerX86 mr = mp.makeMarshallerX86();
			mr.putBool(b);
			assertThrew(mr::getBool,
				ArrayIndexOutOfBoundsException.class);
			mr.rewind();
			assertEquals(b, mr.getBool());
		}
	}
}
