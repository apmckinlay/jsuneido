package suneido.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import javax.annotation.concurrent.Immutable;

import suneido.SuException;

/**
 * A ByteBuf wrapper for a slice of a ByteBuffer.
 * The position and limit of the ByteBuffer are only read at initialization,
 * it has no effect if they are changed later.
 *
 * @author Andrew McKinlay
 */
@Immutable
class ByteBufNormal extends ByteBuf {
	private final ByteBuffer buf;
	private final int offset;
	private final int size;

	ByteBufNormal(ByteBuffer buf) {
		this(buf, buf.position());
	}

	ByteBufNormal(ByteBuffer buf, int offset) {
		this(buf, offset, buf.limit() - offset);
	}

	ByteBufNormal(ByteBuffer buf, int offset, int size) {
		assert buf.order() == ByteOrder.BIG_ENDIAN;
		assert 0 <= offset && offset <= buf.limit();
		assert size <= buf.limit() - offset;
		assert size >= 0;
		this.buf = buf;
		this.offset = offset;
		this.size = size;
	}

	@Override
	public ByteBufNormal slice(int index) {
		return slice(index, size - index);
	}

	@Override
	public ByteBufNormal slice(int index, int size) {
		if (index == 0 && size == this.size)
			return this; // requires immutable
		assert 0 <= index && index <= this.size;
		assert 0 <= size && size <= this.size - index;
		return new ByteBufNormal(buf, offset + index, size);
	}

	@Override
	public ByteBufNormal copy(int size) {
		return new ByteBufNormal(
				ByteBuffer.wrap(arrayCopy(size))
					.order(ByteOrder.BIG_ENDIAN));
	}

	@Override
	public ByteBufNormal readOnlyCopy(int size) {
		return new ByteBufNormal(
				ByteBuffer.wrap(arrayCopy(size))
					.asReadOnlyBuffer()
					.order(ByteOrder.BIG_ENDIAN));
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return getByteBuffer(0);
	}

	@Override
	public ByteBuffer getByteBuffer(int index) {
		ByteBuffer buf2 = buf.duplicate();
		buf2.position(offset + index);
		buf2.order(ByteOrder.BIG_ENDIAN);
		return buf2.slice(); // TODO eliminate slice (needed to make position 0)
	}

	@Override
	public ByteBuffer getByteBuffer(int index, int size) {
		ByteBuffer buf2 = buf.duplicate();
		buf2.position(offset + index);
		buf2.limit(offset + index + size);
		buf2.order(ByteOrder.BIG_ENDIAN);
		return buf2.slice(); // TODO eliminate slice (needed to make position 0)
	}

	@Override
	public ByteBufNormal asReadOnlyBuffer() {
		return new ByteBufNormal(
				buf.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN),
				offset);
	}

	@Override
	public boolean isReadOnly() {
		return buf.isReadOnly();
	}

	private static class Empty {
		public static ByteBufNormal EMPTY = new ByteBufNormal(
				ByteBuffer.allocate(0).order(ByteOrder.BIG_ENDIAN), 0);
	}

	public static ByteBufNormal empty() {
		return Empty.EMPTY;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public ByteOrder order() {
		return buf.order();
	}

	@Override
	public byte get(int index) {
		return buf.get(offset + index);
	}

	@Override
	public ByteBufNormal put(int index, byte b) {
		buf.put(offset + index, b);
		return this;
	}

	@Override
	public short getShort(int index) {
		return buf.getShort(offset + index);
	}

	@Override
	public ByteBufNormal putShort(int index, short value) {
		buf.putShort(offset + index, value);
		return this;
	}

	@Override
	public short getShortLE(int index) {
		return (short) ((buf.get(offset + index) & 0xff)
				| (buf.get(offset + index + 1) << 8));
	}

	@Override
	public ByteBufNormal putShortLE(int index, short value) {
		buf.put(offset + index, (byte) (value & 0xff));
		buf.put(offset + index + 1, (byte) (value >>> 8));
		return this;
	}

	@Override
	public int getInt(int index) {
		return buf.getInt(offset + index);
	}

	@Override
	public ByteBufNormal putInt(int index, int value) {
		buf.putInt(offset + index, value);
		return this;
	}

	@Override
	public int getIntLE(int index) {
		return ((getShortLE(index) & 0xffff)
				| (getShortLE(index + 2) << 16));
	}

	@Override
	public ByteBufNormal putIntLE(int index, int value) {
		putShortLE(index, (short) (value & 0xffff));
		putShortLE(index + 2, (short) (value >>> 16));
		return this;
	}

	@Override
	public long getLong(int index) {
		return buf.getLong(offset + index);
	}

	@Override
	public ByteBufNormal putLong(int index, long value) {
		buf.putLong(offset + index, value);
		return this;
	}

	@Override
	public ByteBufNormal put(int index, byte[] src) {
		for (byte b : src)
			buf.put(offset + index++, b);
		return this;
	}

	@Override
	public ByteBufNormal get(int index, byte[] dst) {
		int j = offset + index;
		for (int i = 0; i < dst.length; ++i)
			dst[i] = buf.get(j++);
		return this;
	}

	private static final Charset charset = Charset.forName("ISO-8859-1");

	@Override
	public ByteBufNormal putString(int index, String s) {
		try {
			put(index, s.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			throw new SuException("error packing string: ", e);
		}
		return this;
	}

	@Override
	public String getString(int index) {
		ByteBuffer buf2 = buf.duplicate();
		buf2.position(offset + index);
		return charset.decode(buf2).toString();
	}

	/** @returns An array containing the initial size bytes of the buffer.
	 * Returns the actual backing array when possible, otherwise a copy.
	 */
	@Override
	public byte[] array(int size) {
		byte[] array;
		if (offset == 0 && buf.hasArray() && buf.arrayOffset() == 0
				&& (array = buf.array()).length == size)
			return array;
		else
			return arrayCopy(size);
	}

	private byte[] arrayCopy(int size) {
		byte[] array = new byte[size];
		if (buf.hasArray())
			System.arraycopy(buf.array(), buf.arrayOffset() + offset, array, 0, size);
		else
			get(0, array);
		return array;
	}

	@Override
	public boolean isDirect() {
		return buf.isDirect();
	}

}
