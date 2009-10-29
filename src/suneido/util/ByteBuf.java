package suneido.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import suneido.SuException;

/**
 * A immutable wrapper for a slice of a ByteBuffer.
 * The ByteBuffer contents may be changed, but the ByteBuf itself is immutable.
 * All get's and put's are absolute.
 * The position and limit of the ByteBuffer are only read at initialization,
 * it has no effect if they are changed later.
 *
 * @author Andrew McKinlay
 */
public class ByteBuf {

	private final ByteBuffer buf;
	private final int offset;
	private final int size;

	public ByteBuf(ByteBuffer buf) {
		this(buf, buf.position());
	}

	public ByteBuf(ByteBuffer buf, int offset) {
assert(buf.order() == ByteOrder.BIG_ENDIAN);
		this.buf = buf;
		this.offset = offset;
		this.size = buf.limit() - offset;
	}

	public ByteBuf(ByteBuffer buf, int offset, int size) {
assert(buf.order() == ByteOrder.BIG_ENDIAN);
		this.buf = buf;
		this.offset = offset;
		this.size = size;
		assert size <= buf.limit() - offset;
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

	public ByteBuf slice(int index) {
		if (index == 0)
			return this;
		return new ByteBuf(buf, offset + index);
	}

	public ByteBuf slice(int index, int size) {
		if (index == 0 && size == this.size)
			return this;
		return new ByteBuf(buf, offset + index, size);
	}

	public ByteBuf copy(int size) {
		byte[] data = new byte[size];
		if (buf.hasArray()) {
			byte[] array = buf.array();
			int arrayOffset = buf.arrayOffset();
			System.arraycopy(array, arrayOffset + offset, data, 0, size);
		} else
			get(0, data);
		return wrap(data);
	}

	// NOTE: position is not 0
	public ByteBuffer getByteBuffer() {
		return getByteBuffer(0);
	}

	// NOTE: position is not 0
	public ByteBuffer getByteBuffer(int index) {
		ByteBuffer buf2 = buf.duplicate();
		buf2.position(offset + index);
		buf2.order(ByteOrder.BIG_ENDIAN);
		return buf2.slice(); // TODO eliminate need for slice
	}

	// NOTE: position is not 0
	public ByteBuffer getByteBuffer(int index, int size) {
		ByteBuffer buf2 = buf.duplicate();
		buf2.position(offset + index);
		buf2.limit(offset + index + size);
		buf2.order(ByteOrder.BIG_ENDIAN);
		return buf2.slice(); // TODO eliminate need for slice
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof ByteBuf))
			return false;
		ByteBuf that = (ByteBuf) other;
		if (size != that.size)
			return false;
		for (int i = 0; i < size; ++i)
			if (get(i) != that.get(i))
				return false;
		return true;
	}

	public ByteBuf asReadOnlyBuffer() {
		return new ByteBuf(buf.asReadOnlyBuffer()
				.order(ByteOrder.BIG_ENDIAN), offset);
	}

	public boolean isReadOnly() {
		return buf.isReadOnly();
	}

	private static class Empty {
		public static ByteBuf EMPTY = new ByteBuf(ByteBuffer.allocate(0)
				.order(ByteOrder.BIG_ENDIAN), 0);
	}

	public static ByteBuf empty() {
		return Empty.EMPTY;
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

	private static final Charset charset = Charset.forName("ISO-8859-1");

	public ByteBuf putString(int index, String s) {
		try {
			put(index, s.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			throw new SuException("error packing string: ", e);
		}
		return this;
	}

	public String getString(int index) {
		ByteBuffer buf2 = buf.duplicate();
		buf2.position(offset + index);
		return charset.decode(buf2).toString();
	}

	public byte[] array() {
		return array(size());
	}

	/** @returns An array containing the initial size bytes of the buffer.
	 * Returns the actual backing array when possible, otherwise a copy.
	 */
	public byte[] array(int size) {
		byte[] array;
		if (offset == 0 && buf.hasArray() && buf.arrayOffset() == 0
				&& (array = buf.array()).length == size)
			return array;
		array = new byte[size];
		get(0, array);
		return array;
	}

}
