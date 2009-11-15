package suneido.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.concurrent.Immutable;

@Immutable
class ByteBufIndirectSlice extends ByteBuf {
	private final ByteBufIndirect buf;
	private final int offset;
	private final int size;

	public ByteBufIndirectSlice(ByteBufIndirect buf, int offset, int size) {
		this.buf = buf;
		this.offset = offset;
		this.size = size;
	}

	@Override
	public byte[] array(int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ByteBufNormal asReadOnlyBuffer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ByteBufNormal copy(int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte get(int index) {
		return buf.get(offset + index);
	}

	@Override
	public ByteBuf get(int index, byte[] dst) {
		buf.get(offset + index, dst);
		return this;
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return buf.getByteBuffer(offset);
	}

	@Override
	public ByteBuffer getByteBuffer(int index) {
		return buf.getByteBuffer(offset + index);
	}

	@Override
	public ByteBuffer getByteBuffer(int index, int size) {
		return buf.getByteBuffer(offset + index, size);
	}

	@Override
	public int getInt(int index) {
		return buf.getInt(offset + index);
	}

	@Override
	public int getIntLE(int index) {
		return buf.getIntLE(offset + index);
	}

	@Override
	public long getLong(int index) {
		return buf.getLong(offset + index);
	}

	@Override
	public short getShort(int index) {
		return buf.getShort(offset + index);
	}

	@Override
	public short getShortLE(int index) {
		return buf.getShortLE(offset + index);
	}

	@Override
	public String getString(int index) {
		return buf.getString(offset + index);
	}

	@Override
	public boolean isDirect() {
		return buf.isDirect();
	}

	@Override
	public boolean isReadOnly() {
		return buf.isReadOnly();
	}

	@Override
	public ByteOrder order() {
		return buf.order();
	}

	@Override
	public ByteBufNormal readOnlyCopy(int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public ByteBuf slice(int index) {
		return slice(index, size() - index);
	}

	@Override
	public ByteBuf slice(int index, int size) {
		return new ByteBufIndirectSlice(buf, offset + index, size);
	}

}
