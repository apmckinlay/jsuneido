/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.util.ByteBuffers;
import suneido.util.Dnum;
import suneido.util.DnumTest;

import com.google.common.primitives.UnsignedLongs;

public class PackDnumTest {

	@Test
	public void test_nbytes() {
		assertThat(PackDnum.nbytes(1), equalTo(1));
		assertThat(PackDnum.nbytes(0xff), equalTo(1));
		assertThat(PackDnum.nbytes(0x100), equalTo(2));
		assertThat(PackDnum.nbytes(0xffff), equalTo(2));
		assertThat(PackDnum.nbytes(0x10000), equalTo(3));
		assertThat(PackDnum.nbytes(UnsignedLongs.MAX_VALUE), equalTo(8));
	}

	@Test
	public void test_pack() {
		test_pack(0, 0, 0, 			3);
		test_pack(+1, 1, 0, 		3, 129, 1);
		test_pack(+1, 100, -2, 		3, 129, 1);
		test_pack(+1, 123, 0, 		3, 131, 123);
		test_pack(+1, 0xffff, 0, 	3, 133, 0xff, 0xff);
		test_pack(-1, 1, 0, 		2, 126, 0xfe);
	}

	private static void test_pack(int sign, int coef, int exp, int... bs) {
		ByteBuffer buf = ByteBuffer.allocate(20);
		PackDnum.pack(sign, coef, exp, buf);
		assertThat(PackDnum.packSize(sign, coef, exp), equalTo(buf.position()));
		assertThat(buf.position(), equalTo(bs.length));
		for (int i = 0; i < bs.length; ++i)
			assertThat(buf.get(i), equalTo((byte) bs[i]));
	}

	@Test
	public void pack_unpack_test() {
		pack_unpack_test("0");
		pack_unpack_test("1");
		pack_unpack_test("-1");
		pack_unpack_test("inf");
		pack_unpack_test("-inf");
		pack_unpack_test("1048575"); // 0xfffff
		pack_unpack_test("1048575e-3");
		pack_unpack_test("-123456");
		pack_unpack_test("12345678901234567899");
		pack_unpack_test("1e99");
		pack_unpack_test("-1e-99");
	}

	private static void pack_unpack_test(String s) {
		Dnum dn = DnumTest.parse(s);
		ByteBuffer buf = pack(dn);
		buf.get(); // tag
		Object x = PackDnum.unpack(buf);
		if (x instanceof Integer)
			assertThat(x.toString(), equalTo(dn.toString()));
		else
			assertThat(((Dnum) x).check(), equalTo(dn));
	}

	private static ByteBuffer pack(Dnum dn) {
		ByteBuffer buf = ByteBuffer.allocate(20);
		PackDnum.pack(dn, buf);
		assertThat(PackDnum.packSize(dn), equalTo(buf.position()));
		buf.flip();
		return buf;
	}

	private static ByteBuffer pack(String s) {
		return pack(DnumTest.parse(s));
	}

	@Test
	public void cmp_test() {
		String data[] = {
				"-inf", "-12345678901234567899", "-1e9", "-123456", "-9", "-1", "-1e-9",
				"0", "1e-9", "1", "9", "123456", "1e9", "12345678901234567899", "inf"};
		int n = data.length;
		for (int i = 0; i < n - 1; ++i) {
			ByteBuffer x = pack(data[i]);
			for (int j = i + 1; j < n; ++j) {
				ByteBuffer y = pack(data[j]);
				assertThat(data[i] + " :: " + data[j], ByteBuffers.bufferUcompare(x, y), lessThan(0));
				assertThat(ByteBuffers.bufferUcompare(y, x), greaterThan(0));
			}
		}
	}

}
