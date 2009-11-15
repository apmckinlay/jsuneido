package suneido.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Abstract base class for wrapper for ByteBuffer
 * All get's and put's are absolute.
 * ByteOrder is always BIG_ENDIAN
 *
 * @author Andrew McKinlay
 */
public abstract class ByteBuf {

	public static ByteBuf allocate(int capacity) {
		return new ByteBufNormal(ByteBuffer.allocate(capacity)
				.order(ByteOrder.BIG_ENDIAN));
	}

	public static ByteBuf wrap(byte[] array) {
		return new ByteBufNormal(ByteBuffer.wrap(array)
				.order(ByteOrder.BIG_ENDIAN));
	}

	public static ByteBuf wrap(byte[] array, int offset, int len) {
		return new ByteBufNormal(ByteBuffer.wrap(array, offset, len)
				.order(ByteOrder.BIG_ENDIAN));
	}

	public static ByteBuf wrap(ByteBuffer buf) {
		return new ByteBufNormal(buf, buf.position());
	}

	public static ByteBuf wrap(ByteBuffer buf, int offset) {
		return new ByteBufNormal(buf, offset);
	}

	public static ByteBuf wrap(ByteBuffer buf, int offset, int size) {
		return new ByteBufNormal(buf, offset, size);
	}

	public static ByteBuf indirect(ByteBuf buf) {
		return new ByteBufIndirect(buf);
	}

	private static class Empty {
		public static final ByteBuf EMPTY = new ByteBufNormal(
				ByteBuffer.allocate(0)
					.asReadOnlyBuffer()
					.order(ByteOrder.BIG_ENDIAN), 0);
	}

	public static ByteBuf empty() {
		return Empty.EMPTY;
	}

	public abstract ByteBuf slice(int index);

	public abstract ByteBuf slice(int index, int size);

	public abstract ByteBuf copy(int size);

	public abstract ByteBuf readOnlyCopy(int size);

	public abstract ByteBuffer getByteBuffer();

	public abstract ByteBuffer getByteBuffer(int index);

	// NOTE: position is not 0
	public abstract ByteBuffer getByteBuffer(int index, int size);

	public abstract ByteBuf asReadOnlyBuffer();

	public abstract boolean isReadOnly();

	public abstract int size();

	public abstract ByteOrder order();

	public abstract byte get(int index);

	public ByteBuf put(int index, byte b) {
		throw new UnsupportedOperationException();
	}

	public abstract short getShort(int index);

	public ByteBuf putShort(int index, short value) {
		throw new UnsupportedOperationException();
	}

	public abstract short getShortLE(int index);

	public ByteBuf putShortLE(int index, short value) {
		throw new UnsupportedOperationException();
	}

	public abstract int getInt(int index);

	public ByteBuf putInt(int index, int value) {
		throw new UnsupportedOperationException();
	}

	public abstract int getIntLE(int index);

	public ByteBuf putIntLE(int index, int value) {
		throw new UnsupportedOperationException();
	}

	public abstract long getLong(int index);

	public ByteBuf putLong(int index, long value) {
		throw new UnsupportedOperationException();
	}

	public ByteBuf put(int index, byte[] src) {
		throw new UnsupportedOperationException();
	}

	public abstract ByteBuf get(int index, byte[] dst);

	public ByteBuf putString(int index, String s) {
		throw new UnsupportedOperationException();
	}

	public abstract String getString(int index);

	public byte[] array() {
		return array(size());
	}

	public void update(ByteBuf newbuf) {
		throw new UnsupportedOperationException();
	}

	/** @returns An array containing the initial size bytes of the buffer.
	 * Returns the actual backing array when possible, otherwise a copy.
	 */
	public abstract byte[] array(int size);

	public abstract boolean isDirect();

	@Override
	public synchronized boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof ByteBuf))
			return false;
		ByteBuf that = (ByteBuf) other;
		if (size() != that.size())
			return false;
		for (int i = 0; i < size(); ++i)
			if (get(i) != that.get(i))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

}