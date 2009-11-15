package suneido.util;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

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

	@Test
	public void test_indirect() {
		ByteBuf normal = ByteBuf.allocate(20);
		normal.putInt(8, 123);
		assertEquals(123, normal.getInt(8));
		ByteBuf buf = ByteBuf.indirect(normal);
		assertEquals(123, buf.getInt(8));

		ByteBuf slice = buf.slice(4);
		assertEquals(123, slice.getInt(4));

		ByteBuf another = ByteBuf.allocate(20);
		another.putInt(8, 456);
		buf.update(another);
		assertEquals(456, buf.getInt(8));
		assertEquals(456, slice.getInt(4));

		try {
			buf.putInt(0, 0);
			fail();
		} catch (UnsupportedOperationException e) { }

		try {
			slice.putInt(0, 0);
			fail();
		} catch (UnsupportedOperationException e) { }
	}

	@Test
	public void test_indirect2() {
		final int SIZE = 10;
		ByteBuffer buffer = ByteBuffer.allocate(SIZE);
		ByteBuf normal = ByteBuf.wrap(buffer);
		normal.putInt(0, 123);
		assertEquals(123, normal.getInt(0));
		ByteBuf indirect = ByteBuf.indirect(normal);
		assertEquals(123, indirect.getInt(0));

		ByteBuf mybuf = ByteBuf.wrap(buffer).copy(SIZE);
		mybuf.putInt(0, 456);

		ByteBuf copy = ByteBuf.wrap(buffer).readOnlyCopy(SIZE);
		indirect.update(copy);

		ByteBuf to = ByteBuf.wrap(buffer);
		to.put(0, mybuf.array());

		assertEquals(456, normal.getInt(0));
		assertEquals(123, indirect.getInt(0));
	}

}
