package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static suneido.language.Pack.pack;
import static suneido.language.Pack.unpack;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;

import org.junit.Test;

import suneido.SuContainer;
import suneido.SuRecord;

public class PackTest {

	@Test
	public void test_pack() {
		test(false);
		test(true);
		test(0);
		test(0, BigDecimal.ZERO);
		test(1);
		test(1, BigDecimal.ONE);
		test(new BigDecimal("4.94557377049180"));
		test("");
		test("abc");
		test(new Date());
		test(new SuContainer());
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

	private static void test(Object expected, Object x) {
		ByteBuffer buf = pack(x);
		test2(expected, buf);
		if (x instanceof Integer) {
			buf = ByteBuffer.allocate(Pack.INT32SIZE);
			Pack.packInt32(buf, (Integer) x);
			test2(expected, buf);
		}

	}

	private static void test2(Object expected, ByteBuffer buf) {
		buf.position(0);
		Object y = unpack(buf);
		assertTrue("expected <" + expected + "> but was <" + y + ">",
				Ops.is_(expected, y));
		assertEquals(Ops.typeName(expected), Ops.typeName(y));
	}

	@Test
	public void pack_number_bug() {
		assertEquals(4, Pack.packSize(10000));

		ByteBuffer buf = pack(10000);
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

	private void t(int n) {
		assertEquals(pack(n), pack(BigDecimal.valueOf(n)));
	}

}
