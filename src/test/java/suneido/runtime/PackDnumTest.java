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

import suneido.util.Dnum;

public class PackDnumTest {

	@Test
	public void test_coefBytes() {
		ck("99", (byte) 99);
		ck("9876", (byte) 98, (byte) 76);
		ck("1111", (byte) 11, (byte) 11);
		ck("1234560000654321", (byte) 12, (byte) 34, (byte) 56, (byte) 0,
				(byte) 0, (byte) 65, (byte) 43, (byte) 21);

		long n = 11;
		for (int i = 1; i < 8; ++i) {
			assertThat(coefBytes(Dnum.from(1, n, 0).coef()), equalTo(i));
			n = n * 100 + 11;
		}
	}
	private static void ck(String s, byte... expected) {
		assertThat(coefBytes(Dnum.parse(s).coef()), equalTo(expected.length));
		byte[] bytes = new byte[8];
		assertThat(coefBytes(Dnum.parse(s).coef(), bytes),
				equalTo(expected.length));
		for (int i = 0; i < expected.length; ++i)
			assertThat(bytes[i], equalTo(expected[i]));
		for (int i = expected.length; i < bytes.length; ++i)
			assertThat(bytes[i], equalTo((byte) 0));
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
			Dnum d2 = (Dnum) PackDnum.unpack(buf);
			assertThat(d2, equalTo(dn));
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
