package suneido.database;

import java.nio.ByteBuffer;

/** A wrapper for another Destination that uses adrForWrite to copy-on-write
 *
 * @author Andrew McKinlay
 */
public class TranDest implements Destination {

	private final Transaction tran;
	private final Destination dest;

	TranDest(Transaction tran, Destination dest) {
		this.tran = tran;
		this.dest = dest;
assert ! (dest instanceof TranDest);
	}

	public ByteBuffer adr(long offset) {
		ByteBuffer buf = tran.shadow.get(offset);
		return buf != null ? buf : dest.adr(offset).asReadOnlyBuffer();
	}

	public ByteBuffer adrForWrite(long offset) {
		ByteBuffer buf = tran.shadow.get(offset);
		if (buf != null)
			return buf;
		buf = dest.adr(offset);
assert ! buf.isReadOnly();
		byte[] data = new byte[Btree.NODESIZE];
		assert buf.position() == 0;
		buf.get(data);
		buf.position(0);
		ByteBuffer copy = ByteBuffer.wrap(data);
copy = buf; // TEMP
		tran.shadow.put(offset, copy);
		return copy;
	}

	public long alloc(int size, byte type) {
		return dest.alloc(size, type);
	}

	public void close() {
		dest.close();
	}

	public long first() {
		return dest.first();
	}

	public int length(long adr) {
		return dest.length(adr);
	}

	public long size() {
		return dest.size();
	}

	public void sync() {
		dest.sync();
	}

	public Destination unwrap() {
		return dest;
	}

}
