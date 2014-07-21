/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import static org.junit.Assert.assertEquals;
import static suneido.language.jsdi.type.BasicType.BOOL;
import static suneido.language.jsdi.type.BasicType.DOUBLE;
import static suneido.language.jsdi.type.BasicType.FLOAT;
import static suneido.language.jsdi.type.BasicType.GDIOBJ;
import static suneido.language.jsdi.type.BasicType.HANDLE;
import static suneido.language.jsdi.type.BasicType.INT16;
import static suneido.language.jsdi.type.BasicType.INT32;
import static suneido.language.jsdi.type.BasicType.INT64;
import static suneido.language.jsdi.type.BasicType.INT8;
import static suneido.language.jsdi.type.BasicType.OPAQUE_POINTER;

import org.junit.Test;

import suneido.language.Numbers;

/**
 * Test for {@link BasicValue}.
 *
 * @author Victor Schappert
 * @since 20130718
 * @see suneido.language.jsdi.abi.abix86.BasicValueTestX86
 */
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

	//
	// TESTS for Marshalling basic values OUT from native return values
	//

	@Test
	public void testMarshallOutReturnValue() {
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
		for (BasicType type : new BasicType[] { INT32, HANDLE, GDIOBJ }) {
			assertEquals(0, bv(type).marshallOutReturnValue(0L, null));
			assertEquals(0, bv(type).marshallOutReturnValue(0xf00000000L, null)); // truncated
			assertEquals(-1, bv(type).marshallOutReturnValue(0x0ffffffffL, null));
			assertEquals(-1, bv(type).marshallOutReturnValue(-1L, null));
		}
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
}
