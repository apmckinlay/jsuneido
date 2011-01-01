package suneido.util;

import static suneido.util.Util.getStringFromBuffer;
import static suneido.util.Util.putStringToByteBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.concurrent.Immutable;

/**
 * Wrapper for ByteBuffer
 * All get's and put's are absolute.
 * ByteOrder is always BIG_ENDIAN
 */
@Immutable
public class ByteBuf {
	private final ByteBuffer buf;
	private final int offset;
	private final int size;

	private ByteBuf(ByteBuffer buf) {
		this(buf, buf.position());
	}

	private ByteBuf(ByteBuffer buf, int offset) {
		this(buf, offset, buf.limit() - offset);
	}

	private ByteBuf(ByteBuffer buf, int offset, int size) {
		assert buf.order() == ByteOrder.BIG_ENDIAN;
		assert 0 <= offset && offset <= buf.limit();
		assert size <= buf.limit() - offset;
		assert size >= 0;
		this.buf = buf;
		this.offset = offset;
		this.size = size;
	}

	public static ByteBuf allocate(int capacity) {
		return new ByteBuf(ByteBuffer.allocate(capacity)
				.order(ByteOrder.BIG_ENDIAN));
	}

	public static ByteBuf wrap(byte[] array) {
		return new ByteBuf(ByteBuffer.wrap(array)
				.order(ByteOrder.BIG_ENDIAN));
	}

	public static ByteBuf wrap(byte[] array, int offset, int len) {
		return new ByteBuf(ByteBuffer.wrap(array, offset, len)
				.order(ByteOrder.BIG_ENDIAN));
	}

	public static ByteBuf wrap(ByteBuffer buf) {
		return new ByteBuf(buf, buf.position());
	}

	public static ByteBuf wrap(ByteBuffer buf, int offset) {
		return new ByteBuf(buf, offset);
	}

	public static ByteBuf wrap(ByteBuffer buf, int offset, int size) {
		return new ByteBuf(buf, offset, size);
	}

	private static class Empty {
		public static final ByteBuf EMPTY = new ByteBuf(
				ByteBuffer.allocate(0)
					.asReadOnlyBuffer()
					.order(ByteOrder.BIG_ENDIAN), 0);
	}

	public static ByteBuf empty() {
		return Empty.EMPTY;
	}

	public ByteBuf slice(int index) {
		return slice(index, size - index);
	}

	public ByteBuf slice(int index, int size) {
		if (index == 0 && size == this.size)
			return this; // requires immutable
		assert 0 <= index && index <= this.size;
		assert 0 <= size && size <= this.size - index;
		return new ByteBuf(buf, offset + index, size);
	}

	public ByteBuf copy() {
		return copy(size);
	}
	public ByteBuf copy(int size) {
		return new ByteBuf(copyByteBuffer(size));
	}

	public ByteBuf readOnlyCopy(int size) {
		return new ByteBuf(
				copyByteBuffer(size)
					.asReadOnlyBuffer()
					.order(ByteOrder.BIG_ENDIAN));
	}

	private ByteBuffer copyByteBuffer(int size) {
		ByteBuffer dst = ByteBuffer.allocate(size);
		dst.put(duplicate(0, size));
		dst.position(0);
		dst.order(ByteOrder.BIG_ENDIAN);
		return dst;
	}

	public ByteBuffer getByteBuffer() {
		return getByteBuffer(0, size);
	}
	public ByteBuffer getByteBuffer(int index) {
		return getByteBuffer(index, size - index);
	}
	public ByteBuffer getByteBuffer(int index, int size) {
		return duplicate(index, size).slice();
		// PERF eliminate slice (needed to make position 0)
	}

	private ByteBuffer duplicate() {
		return duplicate(0, size);
	}
	private ByteBuffer duplicate(int index) {
		return duplicate(index, size - index);
	}
	private ByteBuffer duplicate(int index, int size) {
		ByteBuffer buffer = buf.duplicate();
		buffer.position(offset + index);
		buffer.limit(offset + index + size);
		buffer.order(ByteOrder.BIG_ENDIAN);
		return buffer;
	}

	public ByteBuf asReadOnlyBuffer() {
		return new ByteBuf(
				buf.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN),
				offset);
	}

	public boolean isReadOnly() {
		return buf.isReadOnly();
	}

	public boolean isDirect() {
		return buf.isDirect();
	}

	public int size() {
		return size;
	}

	public ByteOrder order() {
		return buf.order();
	}

	public byte get(int index) {
		return buf.get(offset + index);
	}

	public ByteBuf put(int index, byte b) {
		buf.put(offset + index, b);
		return this;
	}

	public short getShort(int index) {
		return buf.getShort(offset + index);
	}

	public ByteBuf putShort(int index, short value) {
		buf.putShort(offset + index, value);
		return this;
	}

	public short getShortLE(int index) {
		return (short) ((buf.get(offset + index) & 0xff)
				| (buf.get(offset + index + 1) << 8));
	}

	public ByteBuf putShortLE(int index, short value) {
		buf.put(offset + index, (byte) (value & 0xff));
		buf.put(offset + index + 1, (byte) (value >>> 8));
		return this;
	}

	public int getInt(int index) {
		return buf.getInt(offset + index);
	}

	public ByteBuf putInt(int index, int value) {
		buf.putInt(offset + index, value);
		return this;
	}

	public int getIntLE(int index) {
		return ((getShortLE(index) & 0xffff)
				| (getShortLE(index + 2) << 16));
	}

	public ByteBuf putIntLE(int index, int value) {
		putShortLE(index, (short) (value & 0xffff));
		putShortLE(index + 2, (short) (value >>> 16));
		return this;
	}

	public long getLong(int index) {
		return buf.getLong(offset + index);
	}

	public ByteBuf putLong(int index, long value) {
		buf.putLong(offset + index, value);
		return this;
	}

	public ByteBuf put(int index, byte[] src) {
		for (byte b : src)
			buf.put(offset + index++, b);
		return this;
	}

	public ByteBuf get(int index, byte[] dst) {
		int j = offset + index;
		for (int i = 0; i < dst.length; ++i)
			dst[i] = buf.get(j++);
		return this;
	}

	public ByteBuf put(int index, ByteBuf src) {
		duplicate(index).put(src.duplicate());
		return this;
	}

	public ByteBuf putString(int index, String s) {
		putStringToByteBuffer(s, buf, offset + index);
		return this;
	}

	public String getString(int index) {
		return getStringFromBuffer(buf, offset + index);
	}

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
		int hashCode = 1;
		for (int i = 0; i < size(); ++i)
		      hashCode = 31 * hashCode + get(i);
		return hashCode;
	}

	@Override
	public String toString() {
		return "ByteBuf@" + System.identityHashCode(this);
	}

}