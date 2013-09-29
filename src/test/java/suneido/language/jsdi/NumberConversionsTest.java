package suneido.language.jsdi;

import static org.junit.Assert.assertEquals;
import static suneido.language.jsdi.NumberConversions.toLong;
import static suneido.language.jsdi.NumberConversions.toPointer32;
import static suneido.util.testing.Throwing.assertThrew;

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
					new Runnable() { public void run() { toLong(bad); } },
					JSDIException.class, "can't convert"
				);
		}
	}

	// TODO: make test for toFloat
	// TODO: make test for toDouble

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
				new Runnable() { public void run() { toPointer32((long)Integer.MAX_VALUE + 1L); }},
				JSDIException.class
		);
		assertThrew(
				new Runnable() { public void run() { toPointer32((long)Integer.MIN_VALUE - 1L); }},
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
			public void run() {
				toPointer32(Numbers.BD_INT_MAX.add(Numbers.toBigDecimal(1)));
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
			public void run() {
				toPointer32(Numbers.BD_INT_MIN.subtract(Numbers.toBigDecimal(1)));
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
			public void run() { toPointer32(1.5); }
		}, JSDIException.class);
		assertEquals(0, toPointer32(""));
		assertEquals(1, toPointer32("1"));
		assertEquals(-1, toPointer32("-1"));
		assertEquals(Integer.MIN_VALUE, toPointer32(Integer.toString(Integer.MIN_VALUE)));
		assertEquals(Integer.MAX_VALUE, toPointer32(Integer.toString(Integer.MAX_VALUE)));
		assertThrew(new Runnable() {
			public void run() {
				toPointer32(Long.toString((long)Integer.MAX_VALUE + 1L));
			}
		}, JSDIException.class);
		assertThrew(new Runnable() {
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
			public void run() {
				toPointer32(new SuContainer());
			}
		}, JSDIException.class);
	}
}
