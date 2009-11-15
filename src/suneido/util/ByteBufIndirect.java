package suneido.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
// needs to be synchronized to prevent update & then modification during a get
class ByteBufIndirect extends ByteBuf {
	private volatile ByteBuf buf;

	public ByteBufIndirect(ByteBuf buf) {
		this.buf = buf;
	}

	@Override
	public byte[] array(int size) {
		return buf.array(size);
	}

	@Override
	public ByteBuf asReadOnlyBuffer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized ByteBuf copy(int size) {
		return buf.copy(size);
	}

	@Override
	public synchronized byte get(int index) {
		return buf.get(index);
	}

	@Override
	public synchronized ByteBuf get(int index, byte[] dst) {
		buf.get(index, dst);
		return this;
	}

	@Override
	public synchronized ByteBuffer getByteBuffer() {
		return buf.getByteBuffer();
	}

	@Override
	public synchronized ByteBuffer getByteBuffer(int index) {
		return buf.getByteBuffer(index);
	}

	@Override
	public synchronized ByteBuffer getByteBuffer(int index, int size) {
		return buf.getByteBuffer(index, size);
	}

	@Override
	public synchronized int getInt(int index) {
		return buf.getInt(index);
	}

	@Override
	public synchronized int getIntLE(int index) {
		return buf.getIntLE(index);
	}

	@Override
	public synchronized long getLong(int index) {
		return buf.getLong(index);
	}

	@Override
	public synchronized short getShort(int index) {
		return buf.getShort(index);
	}

	@Override
	public synchronized short getShortLE(int index) {
		return buf.getShortLE(index);
	}

	@Override
	public synchronized String getString(int index) {
		return buf.getString(index);
	}

	@Override
	public synchronized boolean isDirect() {
		return buf.isDirect();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public synchronized ByteOrder order() {
		return buf.order();
	}

	@Override
	public ByteBuf readOnlyCopy(int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized int size() {
		return buf.size();
	}

	@Override
	public ByteBuf slice(int index) {
		return slice(index, size() - index);
	}

	@Override
	public ByteBuf slice(int index, int size) {
		assert 0 <= index && index <= size();
		assert 0 <= size && size <= size() - index;
		return new ByteBufIndirectSlice(this, index, size);
	}

	@Override
	public synchronized void update(ByteBuf newbuf) {
		this.buf = newbuf;
	}

}
