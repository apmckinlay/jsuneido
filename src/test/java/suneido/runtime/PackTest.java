/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static suneido.runtime.Numbers.INF;
import static suneido.runtime.Numbers.MC;
import static suneido.runtime.Numbers.MINUS_INF;
import static suneido.runtime.Pack.*;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.SuContainer;
import suneido.SuDate;
import suneido.SuRecord;
import suneido.runtime.Ops;

public class PackTest {

	@Test
	public void test_pack() {
		test(false);
		test(true);
		test(0);
		test(BigDecimal.ZERO, 0);
		test(1);
		test(BigDecimal.ONE, 1);
		test(new BigDecimal("4.94557377049180"), new BigDecimal("4.945573770491"));
		test("");
		test("abc");
		test(SuDate.now());
		test(new SuContainer());
		test(new SuRecord());
		test(10000);
		test(10001);
		test(1234);
		test(12345678);
		test(1234567890);
		test(INF);
		test(MINUS_INF);
		test(BigDecimal.valueOf(Long.MAX_VALUE, -5).round(MC));
	}

	private static void test(Object x) {
		test(x, x);
		if (x instanceof Integer) {
			int n = (Integer) x;
			test(-n, -n);
		}
	}

	private static void test(Object x, Object expected) {
		ByteBuffer buf = pack(x);
		buf.position(0);
		Object y = unpack(buf);
		assertTrue("expected <" + expected + "> but was <" + y + ">",
				Ops.is_(expected, y));
		assertEquals(Ops.typeName(expected), Ops.typeName(y));
	}

	@Test
	public void pack_number_bug() {
		assertEquals(4, packSizeLong(10000));

		ByteBuffer buf = packLong(10000);
		assertEquals(4, buf.remaining());
		assertEquals(0x03, buf.get(0));
		assertEquals((byte) 0x82, buf.get(1));
		assertEquals(0x00, buf.get(2));
		assertEquals(0x01, buf.get(3));
	}

	@Test
	public void pack_int_vs_bd() {
		t(0);
		t(1);
		t(10000);
		t(10001);
	}

	private static void t(int n) {
		assertEquals(packLong(n), pack(BigDecimal.valueOf(n)));
	}

	@Test
	public void pack_number_size() {
		assertThat(packSizeLong(1000), is(4));
		assertThat(packSizeLong(10000), is(4));
		assertThat(packSizeLong(10001), is(6));
		assertThat(packSizeLong(9999999999999999L), is(10));
		assertThat(packSizeLong(Long.MAX_VALUE), is(10));
		assertThat(packSize(BigDecimal.valueOf(Long.MAX_VALUE)), is(10));
	}

	@Test
	public void big_decimal() {
		assertThat(BigDecimal.valueOf(1200, 0).precision(), is(4));

		BigDecimal n = BigDecimal.valueOf(12345678);
		assert n != n.stripTrailingZeros();
	}

	@Test
	public void inf() {
		assertThat(packSize(INF), is(2));
		ByteBuffer buf = pack(INF);
		assertThat(buf.capacity(), is(2));
		assertThat(buf.get(0) & 0xff, is(3));
		assertThat(buf.get(1) & 0xff, is(255));
	}

	@Test
	public void minus_inf() {
		assertThat(packSize(MINUS_INF), is(2));
		ByteBuffer buf = pack(MINUS_INF);
		assertThat(buf.capacity(), is(2));
		assertThat(buf.get(0) & 0xff, is(2));
		assertThat(buf.get(1) & 0xff, is(0));
	}

}