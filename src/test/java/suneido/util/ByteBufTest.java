package suneido.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ByteBufTest {

	@Test
	public void test_array() {
		ByteBuf buf = ByteBuf.allocate(10);
		byte[] magic = new byte[] { 'S', 'n', 'd', 'o' };
		buf.put(0, magic);
		byte[] bytes = new byte[4];
		buf.get(0, bytes);
		assertArrayEquals(magic, bytes);
	}

	@Test
	public void test_ShortLE() {
		ByteBuf buf = ByteBuf.allocate(10);
		assertEquals(0, buf.putShortLE(0, (short) 0).getShortLE(0));
		assertEquals(66, buf.putShortLE(0, (short) 66).getShortLE(0));
		assertEquals(12345, buf.putShortLE(0, (short) 12345).getShortLE(0));
		assertEquals(-66, buf.putShortLE(0, (short) -66).getShortLE(0));
		assertEquals(-12345, buf.putShortLE(0, (short) -12345).getShortLE(0));
	}

	@Test
	public void test_IntLE() {
		ByteBuf buf = ByteBuf.allocate(10);
		assertEquals(0, buf.putIntLE(0, 0).getIntLE(0));
		assertEquals(66, buf.putIntLE(0, 66).getIntLE(0));
		assertEquals(12345, buf.putIntLE(0, 12345).getIntLE(0));
		assertEquals(0x12345678, buf.putIntLE(0, 0x12345678).getIntLE(0));
		assertEquals(-66, buf.putIntLE(0, -66).getIntLE(0));
		assertEquals(-12345, buf.putIntLE(0, -12345).getIntLE(0));
		assertEquals(-0x12345678, buf.putIntLE(0, -0x12345678).getIntLE(0));
	}

}
