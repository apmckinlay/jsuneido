package suneido.language;

import static org.junit.Assert.assertEquals;
import static suneido.language.Pack.pack;
import static suneido.language.Pack.unpack;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;

import org.junit.Test;

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
		test("");
		test("abc");
		test(new Date());
	}

	private static void test(Object x) {
		test(x, x);
	}

	private static void test(Object expected, Object x) {
		ByteBuffer buf = pack(x);
		buf.position(0);
		Object y = unpack(buf);
		assertEquals(expected, y);
	}

}
