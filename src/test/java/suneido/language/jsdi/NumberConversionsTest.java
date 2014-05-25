/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import static org.junit.Assert.assertEquals;
import static suneido.language.jsdi.NumberConversions.toLong;
import static suneido.language.jsdi.NumberConversions.toFloat;
import static suneido.language.jsdi.NumberConversions.toDouble;
import static suneido.language.jsdi.NumberConversions.toPointer32;
import static suneido.language.jsdi.NumberConversions.toPointer64;
import static suneido.util.testing.Throwing.assertThrew;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Test;

import suneido.SuContainer;
import suneido.language.Concats;
import suneido.language.Numbers;

/**
 * Test for {@link NumberConversions}.
 *
 * @author Victor Schappert
 * @since 20130812
 *
 */
public class NumberConversionsTest {

	@Test
	public void testToLong() {
		assertEquals(0L, toLong(Boolean.FALSE));
		assertEquals(1L, toLong(Boolean.TRUE));
		assertEquals(0L, toLong(0));
		assertEquals(1L, toLong(1));
		assertEquals(-1L, toLong(-1));
		assertEquals(Long.MAX_VALUE, toLong(Long.MAX_VALUE));
		assertEquals(Long.MIN_VALUE, toLong(Long.MIN_VALUE));
		assertEquals(Long.MAX_VALUE, toLong(Numbers.BD_LONG_MAX));
		assertEquals(Long.MIN_VALUE, toLong(Numbers.BD_LONG_MIN));
		assertEquals(0L, toLong(""));
		assertEquals(0L, toLong("0"));
		assertEquals(1L, toLong("1"));
		assertEquals(-1L, toLong("-1"));
		assertEquals(8L, toLong("010"));
		for (String prefix : new String[] { "0x", "0X" }) {
			for (int k = 0; k < Integer.SIZE >>> 2; ++k) {
				long expected = 0xfL << (k << 2);
				char[] chars = new char[k + 1];
				Arrays.fill(chars, '0');
				chars[0] = 'f';
				CharSequence value = new Concats(prefix, new String(chars));
				assertEquals(expected, toLong(value));
			}
		}
		assertEquals(0L, toLong(new Buffer(1, "0")));
		assertEquals(1L, toLong(new Buffer(1, "1")));
		assertEquals(-1L, toLong(new Buffer(2, "-1")));
		assertEquals(286L, toLong(new Buffer(3, "286")));
		for (final Object bad : new Object[] { "hello", new SuContainer() }) {
			assertThrew(
					new Runnable() { @Override
					public void run() { toLong(bad); } },
					JSDIException.class, "can't convert"
				);
		}
		// Ensure numbers outside range of Long truncate correctly
		assertEquals(1, toLong(1.55555));
		assertEquals(-123, toLong(-123.89898989898));
		assertEquals(Long.MAX_VALUE, toLong(Double.MAX_VALUE));
		assertEquals(0, toLong(Double.MIN_VALUE));
		assertEquals(Long.MIN_VALUE, toLong(Numbers.BD_LONG_MAX.add(BigDecimal.ONE)));
		assertEquals(Long.MAX_VALUE, toLong(Numbers.BD_LONG_MIN.subtract(BigDecimal.ONE)));
	}

	@Test
	public void testToFloat() {
		assertEquals(0f, toFloat(Boolean.FALSE), 0f);
		assertEquals(1f, toFloat(Boolean.TRUE), 0f);
		assertEquals(0f, toFloat(0f), 0f);
		assertEquals(0f, toFloat(0), 0f);
		assertEquals(0f, toFloat(0L), 0f);
		assertEquals(0f, toFloat(0.0), 0f);
		assertEquals(1f, toFloat(1f), 0f);
		assertEquals(1f, toFloat(1), 0f);
		assertEquals(1f, toFloat(1L), 0f);
		assertEquals(1f, toFloat(1.0), 0f);
		assertEquals(-1f, toFloat(-1f), 0f);
		assertEquals(-1f, toFloat(-1), 0f);
		assertEquals(-1f, toFloat(-1L), 0f);
		assertEquals(-1f, toFloat(-1.0), 0f);
		assertEquals(Float.MIN_VALUE, toFloat(Float.MIN_VALUE), 0f);
		assertEquals(Float.MIN_VALUE, toFloat((double)Float.MIN_VALUE), 0f);
		assertEquals(Float.MAX_VALUE, toFloat(Float.MAX_VALUE), 0f);
		assertEquals(Float.MAX_VALUE, toFloat((double)Float.MAX_VALUE), 0f);
		assertEquals(0.0f, toFloat(BigInteger.ZERO), 0.0f);
		assertEquals(1.0f, toFloat(BigInteger.ONE), 0.0f);
		assertEquals(0.0f, toFloat(BigDecimal.ZERO), 0.0f);
		assertEquals(1.0f, toFloat(BigDecimal.ONE), 0.0f);
		assertEquals(0f, toFloat(""), 0f);
		assertEquals(0f, toFloat("0"), 0f);
		assertEquals(0f, toFloat("0.0"), 0f);
		assertEquals(0f, toFloat("0.000000"), 0f);
		assertEquals(1f, toFloat("1"), 0f);
		assertEquals(1f, toFloat("1.0"), 0f);
		assertEquals(1f, toFloat("1.000000"), 0f);
		assertEquals(-1f, toFloat("-1"), 0f);
		assertEquals(-1f, toFloat("-1.0"), 0f);
		assertEquals(-1f, toFloat("-1.000000"), 0f);
		assertEquals(Float.MIN_VALUE, toFloat(Float.toString(Float.MIN_VALUE)), 0f);
		assertEquals(Float.MAX_VALUE, toFloat(Float.toString(Float.MAX_VALUE)), 0f);
		// Ensure numbers outside range of float truncate appropriately
		assertEquals(Float.POSITIVE_INFINITY, toFloat(Double.MAX_VALUE), 0f);
		assertEquals(0f, toFloat(Double.MIN_VALUE), 0f);
		assertEquals(Float.POSITIVE_INFINITY, toFloat("16777216E" + Float.MAX_EXPONENT), 0f);
	}

	@Test
	public void testToDouble() {
		assertEquals(0.0, toDouble(Boolean.FALSE), 0.0);
		assertEquals(1.0, toDouble(Boolean.TRUE), 0.0);
		assertEquals(0.0, toDouble(0f), 0.0);
		assertEquals(0.0, toDouble(0), 0.0);
		assertEquals(0.0, toDouble(0L), 0.0);
		assertEquals(0.0, toDouble(0.0), 0.0);
		assertEquals(1.0, toDouble(1f), 0.0);
		assertEquals(1.0, toDouble(1), 0.0);
		assertEquals(1.0, toDouble(1L), 0.0);
		assertEquals(1.0, toDouble(1.0), 0.0);
		assertEquals(-1.0, toDouble(-1f), 0.0);
		assertEquals(-1.0, toDouble(-1), 0.0);
		assertEquals(-1.0, toDouble(-1L), 0.0);
		assertEquals(-1.0, toDouble(-1.0), 0.0);
		assertEquals(Float.MIN_VALUE, toDouble(Float.MIN_VALUE), 0.0);
		assertEquals(Float.MIN_VALUE, toDouble((double)Float.MIN_VALUE), 0.0);
		assertEquals(Float.MAX_VALUE, toDouble(Float.MAX_VALUE), 0.0);
		assertEquals(Float.MAX_VALUE, toDouble((double)Float.MAX_VALUE), 0.0);
		assertEquals(Double.MIN_VALUE, toDouble(Double.MIN_VALUE), 0.0);
		assertEquals(Double.MAX_VALUE, toDouble(Double.MAX_VALUE), 0.0);
		assertEquals(0.0, toDouble(BigInteger.ZERO), 0.0);
		assertEquals(1.0, toDouble(BigInteger.ONE), 0.0);
		assertEquals(0.0, toDouble(BigDecimal.ZERO), 0.0);
		assertEquals(1.0, toDouble(BigDecimal.ONE), 0.0);
		assertEquals(0.0, toDouble(""), 0.0);
		assertEquals(0.0, toDouble("0"), 0.0);
		assertEquals(0.0, toDouble("0.0"), 0.0);
		assertEquals(0.0, toDouble("0.000000"), 0.0);
		assertEquals(1.0, toDouble("1"), 0.0);
		assertEquals(1.0, toDouble("1.0"), 0.0);
		assertEquals(1.0, toDouble("1.000000"), 0.0);
		assertEquals(-1.0, toDouble("-1"), 0.0);
		assertEquals(-1.0, toDouble("-1.0"), 0.0);
		assertEquals(-1.0, toDouble("-1.000000"), 0.0);
		assertEquals((double)Float.MIN_VALUE, toDouble(Double.toString(Float.MIN_VALUE)), 0.0);
		assertEquals((double)Float.MAX_VALUE, toDouble(Double.toString(Float.MAX_VALUE)), 0.0);
		assertEquals(Double.MIN_VALUE, toDouble(new Buffer(Double.toString(Double.MIN_VALUE))), 0.0);
		assertEquals(Double.MAX_VALUE, toDouble(new Buffer(Double.toString(Double.MAX_VALUE))), 0.0);
		// Ensure numbers outside range of float truncate appropriately
		assertEquals(Double.POSITIVE_INFINITY, toDouble(new BigDecimal(
				Double.MAX_VALUE).multiply(new BigDecimal(2))), 0.0);
		assertEquals(-1.0,
				toDouble(new BigDecimal(Double.MIN_VALUE)
						.subtract(BigDecimal.ONE)), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, toFloat("4503599627370497E"
				+ Double.MAX_EXPONENT), 0f);
	}

	@Test
	public void testToPointer32() {
		assertEquals(0, toPointer32(Boolean.FALSE));
		assertEquals(0, toPointer32(0));
		assertEquals(1, toPointer32(1));
		assertEquals(-1, toPointer32(-1));
		assertEquals(Integer.MAX_VALUE, toPointer32(Integer.MAX_VALUE));
		assertEquals(Integer.MIN_VALUE, toPointer32(Integer.MIN_VALUE));
		assertEquals(0xffffffff, toPointer32(0xffffffff));
		assertEquals(0x0b501e7e, toPointer32(0x0b501e7e));
		assertEquals(0, toPointer32(0L));
		assertEquals(1, toPointer32(1L));
		assertEquals(-1, toPointer32(-1L));
		assertEquals(Integer.MAX_VALUE, toPointer32((long)Integer.MAX_VALUE));
		assertEquals(Integer.MIN_VALUE, toPointer32((long)Integer.MIN_VALUE));
		assertEquals(0xffffffff, toPointer32((long)0xffffffff));
		assertEquals(0xcafebabe, toPointer32((long)0xcafebabe));
		assertThrew(
				new Runnable() { @Override
				public void run() { toPointer32((long)Integer.MAX_VALUE + 1L); }},
				JSDIException.class
		);
		assertThrew(
				new Runnable() { @Override
				public void run() { toPointer32((long)Integer.MIN_VALUE - 1L); }},
				JSDIException.class
		);
		assertEquals(0, toPointer32(Numbers.toBigDecimal(0)));
		assertEquals(1, toPointer32(Numbers.toBigDecimal(1)));
		assertEquals(-1, toPointer32(Numbers.toBigDecimal(-1)));
		assertEquals(Integer.MAX_VALUE, toPointer32(Numbers.BD_INT_MAX));
		assertEquals(Integer.MIN_VALUE, toPointer32(Numbers.BD_INT_MIN));
		assertEquals(0xffffffff, toPointer32(Numbers.toBigDecimal(0xffffffff)));
		assertEquals(0xacc01ade, toPointer32(Numbers.toBigDecimal(0xacc01ade)));
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer32(Numbers.BD_INT_MAX.add(Numbers.toBigDecimal(1)));
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer32(Numbers.BD_INT_MIN.subtract(Numbers.toBigDecimal(1)));
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
			@Override
			public void run() { toPointer32(1.5); }
		}, JSDIException.class);
		assertEquals(0, toPointer32(""));
		assertEquals(1, toPointer32("1"));
		assertEquals(-1, toPointer32("-1"));
		assertEquals(Integer.MIN_VALUE, toPointer32(Integer.toString(Integer.MIN_VALUE)));
		assertEquals(Integer.MAX_VALUE, toPointer32(Integer.toString(Integer.MAX_VALUE)));
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer32(Long.toString((long)Integer.MAX_VALUE + 1L));
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer32(Long.toString((long)Integer.MIN_VALUE - 1L));
			}
		}, JSDIException.class);
		assertEquals(0, toPointer32(new Buffer(0, "")));
		assertEquals(0, toPointer32(new Buffer(1, "0")));
		assertEquals(16, toPointer32(new Buffer(3, "020")));
		assertEquals(0xff, toPointer32(new Buffer(4, "0xff")));
		assertEquals(-20, toPointer32(new Buffer(3, "-20")));
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer32(new SuContainer());
			}
		}, JSDIException.class);
	}

	@Test
	public void testToPointer64() {
		assertEquals(0, toPointer64(Boolean.FALSE));
		assertEquals(0, toPointer64(0));
		assertEquals(1, toPointer64(1));
		assertEquals(-1, toPointer64(-1));
		assertEquals(Integer.MAX_VALUE, toPointer64(Integer.MAX_VALUE));
		assertEquals(Integer.MIN_VALUE, toPointer64(Integer.MIN_VALUE));
		assertEquals(1L + Integer.MAX_VALUE, toPointer64(1L + Integer.MAX_VALUE));
		assertEquals(Integer.MIN_VALUE - 1L, toPointer64(Integer.MIN_VALUE - 1L));
		assertEquals(Long.MAX_VALUE, toPointer64(Long.MAX_VALUE));
		assertEquals(Long.MIN_VALUE, toPointer64(Long.MIN_VALUE));
		assertEquals(0xffffffff, toPointer64(0xffffffff));
		assertEquals(0x0b501e7e, toPointer64(0x0b501e7e));
		assertEquals(0xffffffffffffffffL, toPointer64(0xffffffffffffffffL));
		assertEquals(0x1100198507250011L, toPointer64(0x1100198507250011L));
		assertEquals(0, toPointer64(0L));
		assertEquals(1, toPointer64(1L));
		assertEquals(-1, toPointer64(-1L));
		assertEquals(Integer.MAX_VALUE, toPointer64((long)Integer.MAX_VALUE));
		assertEquals(Integer.MIN_VALUE, toPointer64((long)Integer.MIN_VALUE));
		assertEquals(0xffffffff, toPointer64((long)0xffffffff));
		assertEquals(0xcafebabe, toPointer64((long)0xcafebabe));
		assertEquals(0, toPointer64(Numbers.toBigDecimal(0)));
		assertEquals(1, toPointer64(Numbers.toBigDecimal(1)));
		assertEquals(-1, toPointer64(Numbers.toBigDecimal(-1)));
		assertEquals(Integer.MAX_VALUE, toPointer64(Numbers.BD_INT_MAX));
		assertEquals(Integer.MIN_VALUE, toPointer64(Numbers.BD_INT_MIN));
		assertEquals(0xffffffff, toPointer64(Numbers.toBigDecimal(0xffffffff)));
		assertEquals(0xacc01ade, toPointer64(Numbers.toBigDecimal(0xacc01ade)));
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer64(Numbers.BD_LONG_MAX.add(Numbers.toBigDecimal(1)));
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer64(Numbers.BD_LONG_MIN.subtract(Numbers.toBigDecimal(1)));
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
			@Override
			public void run() { toPointer64(1.5); }
		}, JSDIException.class);
		assertEquals(0, toPointer64(""));
		assertEquals(1, toPointer64("1"));
		assertEquals(-1, toPointer64("-1"));
		assertEquals(Integer.MIN_VALUE, toPointer64(Integer.toString(Integer.MIN_VALUE)));
		assertEquals(Integer.MAX_VALUE, toPointer64(Integer.toString(Integer.MAX_VALUE)));
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer64(Numbers.BI_LONG_MAX.add(BigInteger.ONE).toString());
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer64(Numbers.BI_LONG_MIN.subtract(BigInteger.ONE).toString());
			}
		}, JSDIException.class);
		assertEquals(0, toPointer64(new Buffer(0, "")));
		assertEquals(0, toPointer64(new Buffer(1, "0")));
		assertEquals(16, toPointer64(new Buffer(3, "020")));
		assertEquals(0xff, toPointer64(new Buffer(4, "0xff")));
		assertEquals(-20, toPointer64(new Buffer(3, "-20")));
		assertThrew(new Runnable() {
			@Override
			public void run() {
				toPointer64(new SuContainer());
			}
		}, JSDIException.class);
	}

}
