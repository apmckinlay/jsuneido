package suneido.database;

import java.nio.ByteBuffer;

/**
 * Wrapper for a ByteBuffer.
 * First part is always a key as a BufRecord.
 * If tree slot, then end of buffer is a long address.
 * Comparisons are by initial key only.
 * @author Andrew McKinlay
 *
 */
class Slot {
	ByteBuffer buf;
	
	public Slot(ByteBuffer buf) {
		this.buf = buf;
	}
	
	public int compareTo(Slot other) {
		return 0; //TODO
	}
	public BufRecord key() {
		return new BufRecord(buf);
	}
	public long adr() {
		return buf.getLong(buf.limit() - 4);
	}
	public int bufsize() {
		return buf.limit();
	}
}
