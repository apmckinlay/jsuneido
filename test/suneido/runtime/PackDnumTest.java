/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static suneido.runtime.PackDnum.coefBytes;
import static suneido.util.ByteBuffers.bufferUcompare;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;

import suneido.runtime.Pack.Tag;
import suneido.util.Dnum;

public class PackDnumTest {

	@Test
	public void test_representation() {
		ck("0", bytes(Tag.PLUS));
		ck("inf", bytes(Tag.PLUS, 0xff, 0xff));
		ck("-inf", bytes(Tag.MINUS, 0, 0));
		ck("99", bytes(Tag.PLUS, 0x82, 99));
		ck("9876", bytes(Tag.PLUS, 0x84, 98, 76));
		ck("-1111", bytes(Tag.MINUS, 0x7b, ~11, ~11));
		ck("1234560000654321", bytes(Tag.PLUS, 0x90, 12, 34, 56, 0, 0, 65, 43, 21));

		long n = 11;
		for (int i = 1; i < 8; ++i) {
			assertThat(coefBytes(Dnum.from(1, n, 0).coef()), equalTo(i));
			n = n * 100 + 11;
		}
	}
	byte[] bytes(int... ints) {
		var bytes = new byte[ints.length];
		for (int i = 0; i < ints.length; i++)
			bytes[i] = (byte) ints[i];
		return bytes;
	}
	private static void ck(String s, byte... expected) {
		var dn = Dnum.parse(s);
		int n = PackDnum.packSize(dn);
		assert n < 20;
		var buf = ByteBuffer.allocate(n);
		PackDnum.pack(dn, buf);
		buf.flip();

		assertThat(buf.remaining(), equalTo(expected.length));
		for (var b : expected)
			assertThat(buf.get(), equalTo(b));
	}

	@Test
	public void test_packing() {
		String nums[] = { "-inf", "-1e9", "-123.45", "-123", "-100",
				"-1e-9", "0", "1e-9", ".123", "100", "123", "123.45", "98765432",
				"98765432.12345678", "1e9", "inf" };
		ArrayList<ByteBuffer> packed = new ArrayList<>();
		for (String s : nums)
			{
			Dnum dn = Dnum.parse(s);
			int n = PackDnum.packSize(dn);
			assert n < 20;
			ByteBuffer buf = ByteBuffer.allocate(n);
			PackDnum.pack(dn, buf);
			buf.flip();
			buf.get(); // tag
			var d2 = PackDnum.unpack(buf);
			assert Ops.is(dn, d2) : "expected " + dn + " got " + d2;
			buf.position(0);
			packed.add(buf);
			}
		for (int i = 0; i < packed.size(); ++i)
			for (int j = 0; j < packed.size(); ++j) {
				ByteBuffer x = packed.get(i);
				ByteBuffer y = packed.get(j);
				int cmp = bufferUcompare(x, y);
				int ijcmp = Integer.compare(i, j);
				assert Integer.signum(cmp) == Integer.signum(ijcmp)
					: "packed " + nums[i] + " <=> " + nums[j];
			}
	}
}
