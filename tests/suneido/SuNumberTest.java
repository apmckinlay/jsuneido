package suneido;

import java.nio.ByteBuffer;

import org.junit.Test;

import static org.junit.Assert.*;

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
}
