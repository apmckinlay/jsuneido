/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.util.Dnum;
import suneido.util.DnumTest;

import com.google.common.primitives.UnsignedLongs;

public class PackDnumTest {

	@Test
	public void test_nbytes() {
		assertThat(PackDnum.nbytes(0), equalTo(1));
		assertThat(PackDnum.nbytes(0x1f), equalTo(1));
		assertThat(PackDnum.nbytes(0x3f), equalTo(2));
		assertThat(PackDnum.nbytes(0x1fff), equalTo(2));
		assertThat(PackDnum.nbytes(0x3fff), equalTo(3));
		assertThat(PackDnum.nbytes(UnsignedLongs.MAX_VALUE), equalTo(9));
	}

	@Test
	public void test_pack() {
		test_pack(0, 0, 0, 		3);
		test_pack(+1, 1, 0, 	3, 128, 1);
		test_pack(+1, 100, -2, 	3, 128, 1);
		test_pack(+1, 123, 0, 	3, 128, 1 << 5, 123);
		test_pack(+1, 0xfff, 0, 3, 128, (1 << 5) | 0xf, 0xff);
		test_pack(+1, 0xfffff, 0, 3, 128, (2 << 5) | 0xf, 0xff, 0xff);
		test_pack(-1, 1, 0, 	2, 127, 1);
	}

	private static void test_pack(int sign, int coef, int exp, int... bs) {
		ByteBuffer buf = ByteBuffer.allocate(20);
		PackDnum.pack(sign, coef, exp, buf);
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
		pack_unpack_test("12345678901234567890");
	}

	private static void pack_unpack_test(String s) {
		Dnum dn = DnumTest.parse(s);
		ByteBuffer buf = ByteBuffer.allocate(20);
		PackDnum.pack(dn, buf);
		buf.flip();
		buf.get(); // tag
		Object x = PackDnum.unpack(buf);
		if (x instanceof Integer)
			assertThat(x.toString(), equalTo(dn.toString()));
		else
			assertThat(((Dnum) x), equalTo(dn));
	}

}
