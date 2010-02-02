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
		test(-1);
		test(new BigDecimal("4.94557377049180"));
		test("");
		test("abc");
		test(new Date());
		test(new SuContainer());
		test(new SuRecord());
	}

	private static void test(Object x) {
		test(x, x);
	}

	private static void test(Object expected, Object x) {
		ByteBuffer buf = pack(x);
		buf.position(0);
		Object y = unpack(buf);
		assertTrue("expected <" + expected + "> but was <" + y + ">",
				Ops.is_(expected, y));
		assertEquals(Ops.typeName(expected), Ops.typeName(y));
	}

	@Test
	public void unpacklong() {
		ByteBuffer buf = pack(-1);
		buf.position(0);
		Object x = Pack.unpack(buf);
		assertEquals(-1, x);
		buf.position(0);
		long n = Pack.unpackLong(buf);
		assertEquals(-1, n);
	}

}
