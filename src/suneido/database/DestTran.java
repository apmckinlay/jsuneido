package suneido.database;

import static suneido.SuException.verify;
import suneido.util.ByteBuf;

/**
 * Wraps another Destination and handles shadowing
 *
 * @author Andrew McKinlay
 */
public class DestTran extends Destination {
	private final Transaction tran;
	private final Destination dest;

	public DestTran(Transaction tran, Destination dest) {
		this.tran = tran;
		this.dest = dest;
		assert ! (dest instanceof DestTran);
	}

	@Override
	public ByteBuf node(long offset) {
		if (tran.isReadWrite())
			tran.readLock(offset);
		return tran.shadows.node(dest, offset);
	}

	@Override
	public ByteBuf nodeForWrite(long offset) {
		verify(tran.isReadWrite());
		tran.writeLock(offset);
		ByteBuf buf = tran.shadows.nodeForWrite(dest, offset);
		if (buf == null)
			tran.abortThrow("write-write conflict with completed transaction");
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
	public long last() {
		return dest.last();
	}

	@Override
	public int length(long adr) {
		return dest.length(adr);
	}

	@Override
	public byte type(long adr) {
		return dest.type(adr);
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