package suneido.database;

import suneido.util.ByteBuf;

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

	public ByteBuf adr(long offset) {
		ByteBuf buf = tran.shadow.get(offset);
		return buf != null ? buf : dest.adr(offset).asReadOnlyBuffer();
	}

	public ByteBuf adrForWrite(long offset) {
		ByteBuf buf = tran.shadow.get(offset);
		if (buf != null) {
			if (buf.isReadOnly()) {
				buf = buf.copy(Btree.NODESIZE);
				tran.shadow.put(offset, buf);
			}
			return buf;
		}
		buf = dest.adr(offset);
assert ! buf.isReadOnly();
		ByteBuf copy = buf.copy(Btree.NODESIZE);
//copy = buf; // TEMP
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
