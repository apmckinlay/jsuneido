/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static suneido.compiler.Compiler.compile;
import static suneido.runtime.Pack.pack;
import static suneido.runtime.Pack.packLong;
import static suneido.runtime.Pack.unpack;
import static suneido.util.ByteBuffers.bufferUcompare;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.PortTests;
import suneido.SuObject;
import suneido.SuDate;
import suneido.SuRecord;
import suneido.util.Dnum;

public class PackTest {

	@Test
	public void test_pack() {
		test(false);
		test(true);
		test(0);
		test(Dnum.Zero);
		test(Dnum.Inf);
		test(Dnum.MinusInf);
		test(Dnum.One, 1);
		test(1);
		test(Dnum.parse("4.945573770491"));
		test("");
		test("abc");
		test(SuDate.now());
		test(new SuObject());
		test(new SuRecord());
		test(10000);
		test(10001);
		test(1234);
		test(12345678);
		test(1234567890);
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
	public void pack_int_bug() {
		assertEquals(4, PackDnum.packSizeLong(1234));

		ByteBuffer buf = packLong(1234);
		assertEquals(4, buf.remaining());
		assertEquals(0x03, buf.get(0));
		assertEquals((byte) 0x84, buf.get(1));
		assertEquals(12, buf.get(2));
		assertEquals(34, buf.get(3));
	}

	@Test
	public void pack_int_vs_dnum() {
		t(0);
		t(1);
		t(10000);
		t(10001);
	}

	private static void t(long n) {
		assertEquals(packLong(n), pack(Dnum.from(n)));
	}

	@Test
	public void pack_number_size() {
		assertThat(PackDnum.packSizeLong(1234), equalTo(4));
		assertThat(PackDnum.packSizeLong(12345), equalTo(5));
		assertThat(PackDnum.packSizeLong(1234567), equalTo(6));
		assertThat(PackDnum.packSizeLong(Integer.MAX_VALUE), equalTo(7));
		assertThat(PackDnum.packSize(Dnum.from(Integer.MAX_VALUE)), equalTo(7));
	}

	@Test
	public void pack_dnum() {
		String[] data = { "-123e-64", "0", "123", "-123", "inf", "-inf", ".001",
				"1234567890123456", "-123e-99", "456e99" };
		for (String s : data) {
			Dnum dn = Dnum.parse(s);
			ByteBuffer bufdn = Pack.pack(dn);
			Object x = Pack.unpack(bufdn);
			if (x instanceof Dnum)
				assertThat((Dnum) x, equalTo(dn));
			else {
				long i = ((Number) x).longValue();
				assertThat(i, equalTo(Long.parseLong(s)));
			}
		}
	}

	@Test
	public void unpacklong() {
		upl(0);
		upl(123);
		upl(-123);
		upl(1230000);
		upl(-1230000);
		upl(2000000000);
		upl(-2000000000);
		upl(9999_9999_9999_9999L);
		upl(-9999_9999_9999_9999L);
		upl(9999_0000_0000_0000L);
		upl(-9999_0000_0000_0000L);
	}

	void upl(long n) {
		ByteBuffer buf = Pack.packLong(n);
		assertThat(buf, equalTo(Pack.pack(Dnum.from(n))));
		assertThat(PackDnum.unpackLong(buf), equalTo(n));
	}

	@Test
	public void porttests() {
		PortTests.addTest("compare_packed", PackTest::pt_compare_packed);
		PortTests.skipTest("lang_rangeto");
		PortTests.skipTest("lang_rangelen");
		PortTests.skipTest("compare");
		assert PortTests.runFile("lang.test");
	}

	public static boolean pt_compare_packed(String... data) {
		int n = data.length;
		for (int i = 0; i < n; ++i) {
			ByteBuffer x = Pack.pack(compile("test", data[i]));
			for (int j = i + 1; j < n; ++j) {
				ByteBuffer y = Pack.pack(compile("test", data[j]));
				if (bufferUcompare(x, y) >= 0 || bufferUcompare(y, x) <= 0) {
					System.out.println("\t" + x + " should be less than " + y);
					return false;
				}
			}
		}
		return true;
	}

}
