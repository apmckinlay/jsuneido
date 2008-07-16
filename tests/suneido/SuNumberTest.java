package suneido;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class SuNumberTest {
	@Test
	public void pack() {
		String[] values = {
				"1", "10", "123", "1000", "9999", "10000", "10002", "100020000", "100020003","1000200030004",
				".12", ".1", ".01", ".001", ".0001", ".00010002", ".00001"
				};
		for (String s : values) {
			SuValue x = new SuDecimal(s);
			ByteBuffer buf = ByteBuffer.allocate(x.packSize());
			x.pack(buf);
			SuValue y = SuValue.unpack(buf);
			assertEquals(x, y);
		}
	}

	@Test
	public void test() {
		SuInteger num = SuInteger.valueOf(34);
		ByteBuffer buf = ByteBuffer.allocate(num.packSize());
		num.pack(buf);
		assertEquals(34, SuNumber.unpackLong(buf));
	}

	@Test
	public void valueOf() {
		check("0", SuInteger.valueOf(0));
		check("-12", SuInteger.valueOf(-12));
		check("0x100", SuInteger.valueOf(256));
		check("0377", SuInteger.valueOf(255));
		check("123456", SuInteger.valueOf(123456));
		check("1234567890123");
		check(".1");
		check("1e6");
		check("1E3");
	}

	private void check(String s, SuNumber x) {
		SuNumber y = SuNumber.valueOf(s);
		assertEquals(x.getClass(), y.getClass());
		assertEquals(x, y);
	}
	private void check(String s) {
		check(s, new SuDecimal(s));
	}
}
