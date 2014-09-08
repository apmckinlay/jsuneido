/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static suneido.jsdi.type.BasicType.BOOL;
import static suneido.jsdi.type.BasicType.DOUBLE;
import static suneido.jsdi.type.BasicType.FLOAT;
import static suneido.jsdi.type.BasicType.GDIOBJ;
import static suneido.jsdi.type.BasicType.HANDLE;
import static suneido.jsdi.type.BasicType.INT16;
import static suneido.jsdi.type.BasicType.INT32;
import static suneido.jsdi.type.BasicType.INT64;
import static suneido.jsdi.type.BasicType.INT8;
import static suneido.jsdi.type.BasicType.OPAQUE_POINTER;
import static suneido.util.testing.Throwing.assertThrew;

import java.math.BigDecimal;

import org.junit.Test;

import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.marshall.PrimitiveSize;
import suneido.runtime.Numbers;
import suneido.util.testing.Assumption;

/**
 * Test for {@link BasicValue}.
 *
 * @author Victor Schappert
 * @since 20130718
 * @see suneido.jsdi.abi.x86.BasicValueTestX86
 */
@DllInterface
public class BasicValueTest {

	public static BasicValue bv(BasicType basicType) {
		return new BasicValue(basicType);
	}

	private static final Object bd(float f) {
		return Numbers.toBigDecimal(f);
	}

	private static final Object bd(double d) {
		return Numbers.toBigDecimal(d);
	}

	public static final class BasicTypeSet {
		public final BasicType type;
		public final Object[] values;
		public BasicTypeSet(BasicType type, Object... values) {
			this.type = type;
			this.values = values;
		}
	}

	public static final BasicTypeSet[] SETS = new BasicTypeSet[] {
		new BasicTypeSet(BOOL, Boolean.TRUE, Boolean.FALSE),
		new BasicTypeSet(INT8, (int)Byte.MIN_VALUE, -1, 0, 1, (int)Byte.MAX_VALUE),
		new BasicTypeSet(INT16, (int)Short.MIN_VALUE, -1, 0, 1, 0x01ff, (int)Short.MAX_VALUE),
		new BasicTypeSet(INT32, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE),
		new BasicTypeSet(INT64, Long.MIN_VALUE, -1L, 0L, 1L, 0x01ffffffffL, Long.MAX_VALUE),
		new BasicTypeSet(OPAQUE_POINTER, (long)Integer.MIN_VALUE, (long)-1, (long)0, (long)1, (long)Integer.MAX_VALUE),
		// VS@20130808: I took out the tests for infinity/NaN because BigDecimal
		//              doesn't handle them, but this does create an issue: we
		//              can still get NaN's, if only due to the dll interface,
		//              and they will result in the Suneido programmer getting
		//              odd error messages.
		new BasicTypeSet(FLOAT, bd(Float.MIN_VALUE), bd(Float.MIN_NORMAL),
				bd(-1.0f), bd(0.0f), bd(1.0f)),
		new BasicTypeSet(DOUBLE, bd(Double.MIN_VALUE), bd(Double.MIN_NORMAL),
				bd(-1.0), bd(0.0), bd(1.0)),
		new BasicTypeSet(HANDLE, (long)Integer.MIN_VALUE, (long)-1, (long)0, (long)1, (long)Integer.MAX_VALUE),
		new BasicTypeSet(GDIOBJ, (long)Integer.MIN_VALUE, (long)-1, (long)0, (long)1, (long)Integer.MAX_VALUE)
	};

	private static final BasicType[] PTR_TYPES = { BasicType.OPAQUE_POINTER, BasicType.GDIOBJ, BasicType.HANDLE };

	//
	// TESTS for Marshalling basic values OUT from native return values
	//

	@Test
	public void testMarshallOutReturnValueNoPointer() {
		assertEquals(Boolean.FALSE, bv(BOOL).marshallOutReturnValue(0L, null));
		assertEquals(Boolean.TRUE, bv(BOOL).marshallOutReturnValue(1L, null));
		assertEquals(0, bv(INT8).marshallOutReturnValue(0L, null));
		assertEquals(0, bv(INT8).marshallOutReturnValue(0xf00L, null)); // truncated
		assertEquals(-1, bv(INT8).marshallOutReturnValue(0xffL, null));
		assertEquals(-1, bv(INT8).marshallOutReturnValue(-1L, null));
		assertEquals(0, bv(INT16).marshallOutReturnValue(0L, null));
		assertEquals(0, bv(INT16).marshallOutReturnValue(0xf0000L, null)); // truncated
		assertEquals(-1, bv(INT16).marshallOutReturnValue(0xffffL, null));
		assertEquals(-1, bv(INT16).marshallOutReturnValue(-1L, null));
		assertEquals(0, bv(INT32).marshallOutReturnValue(0L, null));
		assertEquals(0, bv(INT32).marshallOutReturnValue(0xf00000000L, null)); // truncated
		assertEquals(-1, bv(INT32).marshallOutReturnValue(0x0ffffffffL, null));
		assertEquals(-1, bv(INT32).marshallOutReturnValue(-1L, null));
		assertEquals(0L, bv(INT64).marshallOutReturnValue(0L, null));
		assertEquals(-1L, bv(INT64).marshallOutReturnValue(-1L, null));
		assertEquals(Long.MAX_VALUE, bv(INT64).marshallOutReturnValue(Long.MAX_VALUE, null));
		assertEquals(Numbers.toBigDecimal(0.0), bv(FLOAT)
				.marshallOutReturnValue((long) Float.floatToRawIntBits(0.0f), null));
		assertEquals(
				Numbers.toBigDecimal(Float.MIN_VALUE),
				bv(FLOAT).marshallOutReturnValue(
						Double.doubleToRawLongBits((double) Float.MIN_VALUE), null));
		assertEquals(Numbers.toBigDecimal(0.0), bv(DOUBLE)
				.marshallOutReturnValue(Double.doubleToRawLongBits(0.0), null));
		assertEquals(
				Numbers.toBigDecimal(Double.MIN_VALUE),
				bv(DOUBLE).marshallOutReturnValue(
						Double.doubleToRawLongBits(Double.MIN_VALUE), null));
	}

	@Test
	public void testMarshallOutReturnValuePointer() {
		Assumption.jsdiIsAvailable(); // Prevent failure on Mac OS, Linux, etc.
		if (Integer.BYTES == PrimitiveSize.POINTER) {
			for (BasicType type : PTR_TYPES) {
				assertEquals(0, bv(type).marshallOutReturnValue(0L, null));
				// Test truncation
				assertEquals(0,
						bv(type).marshallOutReturnValue(0xf00000000L, null));
				assertEquals(-1,
						bv(type).marshallOutReturnValue(0x0ffffffffL, null));
				assertEquals(-1, bv(type).marshallOutReturnValue(-1L, null));
			}
		} else if (Long.BYTES == PrimitiveSize.POINTER) {
			for (BasicType type : PTR_TYPES) {
				assertEquals(0L, bv(type).marshallOutReturnValue(0L, null));
				assertEquals(0xf00000000L,
						bv(type).marshallOutReturnValue(0xf00000000L, null));
				assertEquals(-1L, bv(type).marshallOutReturnValue(-1L, null));
			}
		} else {
			throw new RuntimeException("pointer size not supported: "
					+ PrimitiveSize.POINTER);
		}
	}

	//
	// TESTS for Marshalling basic values in and out of Java long values
	//

	private static final class LongMarshallHelper {
		final BasicValue type;
		final Object obj;
		final long j;

		LongMarshallHelper(BasicType type, Object obj, long j) {
			this.type = bv(type);
			this.obj = obj;
			this.j = j;
		}

		BigDecimal bd() {
			return Numbers.toBigDecimal((Number) obj);
		}
	}

	private static final LongMarshallHelper lmh(BasicType type, Object fromObj,
			long fromLong) {
		return new LongMarshallHelper(type, fromObj, fromLong);
	}

	@Test
	public void marshallToFromLong_Bool() {
		// General tests
		LongMarshallHelper[] arr = { lmh(BOOL, Boolean.FALSE, 0L),
				lmh(BOOL, Boolean.TRUE, 1L) };
		for (LongMarshallHelper x : arr) {
			assertEquals(x.j, x.type.marshallInToLong(x.obj));
			assertEquals(x.obj, x.type.marshallOutFromLong(x.j, null));
		}
		// Truncation tests: since the value is only 32 bits wide, any info in
		// the high-order 32 bits ought to be ignored.
		assertEquals(Boolean.FALSE, bv(BOOL).marshallOutFromLong(0xf00000000L, null));
		assertEquals(Boolean.TRUE, bv(BOOL).marshallOutFromLong(0xf000008888L, null));
	}

	@Test
	public void marshallToFromLong_Int8() {
		// General tests
		LongMarshallHelper[] arr = { lmh(INT8, 0, 0L), lmh(INT8, 1, 1L),
				lmh(INT8, -1, 0xffL), lmh(INT8, -128, 0x80L),
				lmh(INT8, 127, 0x7fL) };
		for (LongMarshallHelper x : arr) {
			assertEquals(x.j, x.type.marshallInToLong(x.obj));
			assertEquals(x.obj, x.type.marshallOutFromLong(x.j, null));
		}
		// Truncation tests
		assertEquals(-1, bv(INT8).marshallOutFromLong(-1L, null));
		assertEquals(31, bv(INT8).marshallOutFromLong(0xfedcba987654321fL, null));
	}

	@Test
	public void marshallToFromLong_Int16() {
		// General tests
		LongMarshallHelper[] arr = { lmh(INT16, 0, 0L), lmh(INT16, 1, 1L),
				lmh(INT16, -1, 0xffffL), lmh(INT16, -32768, 0x8000L),
				lmh(INT16, 32767, 0x7fffL) };
		for (LongMarshallHelper x : arr) {
			assertEquals(x.j, x.type.marshallInToLong(x.obj));
			assertEquals(x.obj, x.type.marshallOutFromLong(x.j, null));
		}
		// Truncation tests
		assertEquals(-1, bv(INT16).marshallOutFromLong(-1L, null));
		assertEquals(0x321f, bv(INT16).marshallOutFromLong(0xfedcba987654321fL, null));
	}

	@Test
	public void marshallToFromLong_Int32() {
		// General tests
		LongMarshallHelper[] arr = { lmh(INT32, 0, 0L), lmh(INT32, 1, 1L),
				lmh(INT32, -1, 0xffffffffL), lmh(INT32, 32768, 0x8000L),
				lmh(INT32, 32767, 0x7fffL),
				lmh(INT32, 0x12340000, 0x12340000L),
				lmh(INT32, Integer.MIN_VALUE, (long) Integer.MIN_VALUE & 0xffffffffL),
				lmh(INT32, Integer.MAX_VALUE, (long) Integer.MAX_VALUE & 0xffffffffL) };
		for (LongMarshallHelper x : arr) {
			assertEquals(x.j, x.type.marshallInToLong(x.obj));
			assertEquals(x.obj, x.type.marshallOutFromLong(x.j, null));
		}
		// Truncation tests
		assertEquals(-1, bv(INT32).marshallOutFromLong(-1L, null));
		assertEquals(0x7654321f, bv(INT32).marshallOutFromLong(0xfedcba987654321fL, null));
	}

	@Test
	public void marshallToFromLong_Int64() {
		LongMarshallHelper[] arr = { lmh(INT64, 0L, 0L), lmh(INT64, 1L, 1L),
				lmh(INT64, 0xffffffffL, 0xffffffffL), lmh(INT64, 32768L, 0x8000L),
				lmh(INT64, 32767L, 0x7fffL),
				lmh(INT64, 0x12340000L, 0x12340000L),
				lmh(INT64, Long.MIN_VALUE, Long.MIN_VALUE),
				lmh(INT64, Long.MAX_VALUE, Long.MAX_VALUE) };
		for (LongMarshallHelper x : arr) {
			assertEquals(x.j, x.type.marshallInToLong(x.obj));
			assertEquals(x.obj, x.type.marshallOutFromLong(x.j, null));
		}
	}

	@Test
	public void marshallToFromLong_Float() {
		LongMarshallHelper[] arr = { lmh(FLOAT, 1.0f, 0x3f800000L), lmh(FLOAT, 2.0f, 0x40000000L),
				lmh(FLOAT, -1.0f, 0xbf800000L), lmh(FLOAT, -128.5f, 0xc3008000L) };
		for (LongMarshallHelper x : arr) {
			assertEquals(x.j, x.type.marshallInToLong(x.obj));
			assertEquals(x.bd(), x.type.marshallOutFromLong(x.j, null));
		}
	}

	@Test
	public void marshallToFromLong_Double() {
		LongMarshallHelper[] arr = { lmh(DOUBLE, 0.0, 0L),
				lmh(DOUBLE, 1.0, 0x3ff0000000000000L),
				lmh(DOUBLE, 2.0, 0x4000000000000000L),
				lmh(DOUBLE, 3.0, 0x4008000000000000L),
				lmh(DOUBLE, -13, 0xc02a000000000000L) };
		for (LongMarshallHelper x : arr) {
			assertEquals(x.j, x.type.marshallInToLong(x.obj));
			assertEquals(x.bd(), x.type.marshallOutFromLong(x.j, null));
		}
	}

	@Test
	public void marshallToFromLong_Pointer32() {
		assumeTrue(Integer.BYTES == PrimitiveSize.POINTER);
		for (final BasicType ptr_type : PTR_TYPES) {
			LongMarshallHelper[] arr = { lmh(ptr_type, 0, 0L),
					lmh(ptr_type, 1, 1L),
					lmh(ptr_type, Integer.MAX_VALUE, (long)Integer.MAX_VALUE),
					lmh(ptr_type, Integer.MIN_VALUE, (long)Integer.MIN_VALUE),
					lmh(ptr_type, 32768, 0x8000L),
					lmh(ptr_type, 32767, 0x7fffL),
					lmh(ptr_type, 0x12340000, 0x12340000L) };
			for (LongMarshallHelper x : arr) {
				LongMarshallHelper y = lmh(ptr_type, x.obj, x.j);
				assertEquals(y.j, y.type.marshallInToLong(y.obj));
				assertEquals(y.obj, y.type.marshallOutFromLong(y.j, null));
			}
			// When marshalling a 32-bit pointer out of a 64-bit number, the
			// high-order 32 bits may be garbage and are ignored.
			assertEquals(-1, bv(ptr_type).marshallOutFromLong(0xaaaaaaaaffffffffL, null));
			// Can't marshall a number requiring > 32 bits to represent into a
			// 32-bit pointer value
			assertThrew(
					() -> { bv(ptr_type).marshallInToLong((long)Integer.MAX_VALUE + 1L); }, JSDIException.class, "can't convert.*pointer"
				);
			assertThrew(
					() -> { bv(ptr_type).marshallInToLong((long)Integer.MIN_VALUE - 1L); }, JSDIException.class, "can't convert.*pointer"
				);
		}
	}

	@Test
	public void marshallToFromLong_Pointer64() {
		assumeTrue(Long.BYTES == PrimitiveSize.POINTER);
		for (BasicType ptr_type : PTR_TYPES) {
			LongMarshallHelper[] arr = { lmh(ptr_type, 0L, 0L),
					lmh(ptr_type, 1L, 1L),
					lmh(ptr_type, (long) Integer.MAX_VALUE, (long) Integer.MAX_VALUE),
					lmh(ptr_type, (long) Integer.MIN_VALUE, (long) Integer.MIN_VALUE),
					lmh(ptr_type, Long.MAX_VALUE, Long.MAX_VALUE),
					lmh(ptr_type, Long.MIN_VALUE, Long.MIN_VALUE),
					lmh(ptr_type, 32768L, 0x8000L),
					lmh(ptr_type, 32767L, 0x7fffL),
					lmh(ptr_type, 0x12340000L, 0x12340000L),
					lmh(ptr_type, 0x1234567890abcdefL, 0x1234567890abcdefL) };
			for (LongMarshallHelper x : arr) {
				LongMarshallHelper y = lmh(ptr_type, x.obj, x.j);
				assertEquals(y.j, y.type.marshallInToLong(y.obj));
				assertEquals(y.obj, y.type.marshallOutFromLong(y.j, null));
			}
		}
	}

	@Test
	public void marshallToLong_Boolean_InOpaquePointerType() {
		Assumption.jsdiIsAvailable(); // Prevent failure on Mac OS, Linux, etc. 
		for (BasicType ptr_type : PTR_TYPES) {
			LongMarshallHelper[] arr = { lmh(ptr_type, Boolean.FALSE, 0L),
					lmh(ptr_type, Boolean.TRUE, 1L) };
			for (LongMarshallHelper x : arr) {
				assertEquals(x.j, x.type.marshallInToLong(x.obj));
			}
		}
	}

	@Test
	public void marshallToLong_32bit_Overflow() {
		assumeTrue(Integer.BYTES == PrimitiveSize.POINTER);
		for (BasicType ptr_type : PTR_TYPES) {
			BasicValue b = bv(ptr_type);
			assertEquals((long) Integer.MIN_VALUE,
					b.marshallInToLong((long) Integer.MIN_VALUE));
			assertEquals((long) Integer.MAX_VALUE,
					b.marshallInToLong((long) Integer.MAX_VALUE));
			assertThrew(() -> {
				b.marshallInToLong((long) Integer.MIN_VALUE - 1L);
			}, JSDIException.class, "can't convert.*32-bit");
			assertThrew(() -> {
				b.marshallInToLong((long) Integer.MAX_VALUE + 1L);
			}, JSDIException.class, "can't convert.*32-bit");
		}
	}
}
