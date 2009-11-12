package suneido.database;

import static suneido.Suneido.verify;
import suneido.util.ByteBuf;

/** A wrapper for another Destination that uses nodeForWrite to copy-on-write
 *
 * @author Andrew McKinlay
 */
public class TranDest extends Destination {
	private final Transaction tran;
	private final Destination dest;

	TranDest(Transaction tran, Destination dest) {
		this.tran = tran;
		this.dest = dest;
assert ! (dest instanceof TranDest);
	}

	@Override
	public ByteBuf node(long offset) {
		if (tran.isReadWrite())
			tran.readLock(offset);
		ByteBuf buf = tran.shadows.get(offset);
		if (buf == null) {
			buf = dest.adr(offset);
			assert buf.isDirect(); // shadowing depends on this
			tran.shadows.put(offset, buf);
		}
		return buf;
	}

	@Override
	public ByteBuf nodeForWrite(long offset) {
		verify(tran.isReadWrite());
		tran.writeLock(offset);
		ByteBuf buf = tran.shadows.get(offset);
		if (buf == null) {
			buf = dest.adr(offset);
		} else if (! buf.isDirect() && ! buf.isReadOnly()) {
			return buf;
		}
		buf = buf.copy(Btree.NODESIZE);
		tran.shadows.put(offset, buf);
		return buf;
	}

	@Override
	public ByteBuf adr(long offset) {
		return dest.adr(offset);
	}

	@Override
	public long alloc(int size, byte type) {
		return dest.alloc(size, type);
	}

	@Override
	public void close() {
		dest.close();
	}

	@Override
	public long first() {
		return dest.first();
	}

	@Override
	public int length(long adr) {
		return dest.length(adr);
	}

	@Override
	public long size() {
		return dest.size();
	}

	@Override
	public void sync() {
		dest.sync();
	}

	@Override
	public Destination unwrap() {
		return dest;
	}

}
