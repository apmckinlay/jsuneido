/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

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
		test_pack(-1, 1, 0, 	2, 127, 1);
	}

	private static void test_pack(int sign, int coef, int exp, int... bs) {
		ByteBuffer buf = ByteBuffer.allocate(20);
		PackDnum.pack(sign, coef, exp, buf);
		assertThat(buf.position(), equalTo(bs.length));
		for (int i = 0; i < bs.length; ++i)
			assertThat(buf.get(i), equalTo((byte) bs[i]));
	}

}
