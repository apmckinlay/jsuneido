package suneido.database;

import java.nio.ByteBuffer;

/**
 * Holds an index node "slot" while in memory.
 * Comparisons are by key only. (not addresses)
 * Used with {@link Slots}
 * @author Andrew McKinlay
 */
public class Slot implements suneido.Packable, Comparable<Slot> {
	public final BufRecord key;
	public final long[] adrs;
	
	public Slot(BufRecord key, long ... adrs) {
		this.key = key;
		this.adrs = adrs;
	}
	
	public int compareTo(Slot other) {
		return key.compareTo(other.key);
	}
	
	public int packSize() {
		return key.packSize() + 8 * adrs.length;
	}
	public void pack(ByteBuffer buf) {
		key.pack(buf);
		for (long adr : adrs)
			buf.putLong(adr);
	}
	/**
	 * 
	 * @param buf A ByteBuffer containing a packed Slot.
	 * 		<b>Note:</b> The limit of the buffer must be correct.
	 * @return A new Slot containing the values from the buffer.
	 */
	public static Slot unpack(ByteBuffer buf) {
		BufRecord key = new BufRecord(buf);
		int nadrs = (buf.limit() - key.bufSize()) / 8;
		long[] adrs = new long[nadrs];
		buf.position(key.bufSize());
		for (int i = 0; i < nadrs; ++i)
			adrs[i] = buf.getLong();
		return new Slot(key, adrs);
	}
	
	@Override
	public String toString() {
		String s = key.toString();
		for (long adr : adrs)
			s += " " + adr;
		return s;
	}
}
