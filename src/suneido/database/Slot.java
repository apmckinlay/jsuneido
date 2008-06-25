package suneido.database;

import java.nio.ByteBuffer;

/**
 * Holds an index node "slot" while in memory.
 * Comparisons are by key only. (not addresses)
 * Used with {@link Slots}
 * Addresses (file offsets) are stored as int's
 * by aligning and shifting right.
 * @author Andrew McKinlay
 */
public class Slot implements suneido.Packable, Comparable<Slot> {
	public final Record key;
	public final long[] adrs;
	
	public Slot() {
		key = Record.MINREC;
		adrs = new long[0];
	}
	
	public Slot(Record key, long ... adrs) {
		this.key = key;
		this.adrs = adrs;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Slot
			? 0 == compareTo((Slot) other)
			: false;
	}
	public int compareTo(Slot other) {
		return key.compareTo(other.key);
	}
	
	public int packSize() {
		return key.packSize() + 4 * adrs.length;
	}
	public void pack(ByteBuffer buf) {
		key.pack(buf);
		for (long adr : adrs)
			buf.putInt(Mmfile.offsetToInt(adr));
	}
	/**
	 * 
	 * @param buf A ByteBuffer containing a packed Slot.
	 * 		<b>Note:</b> The limit of the buffer must be correct.
	 * @return A new Slot containing the values from the buffer.
	 */
	public static Slot unpack(ByteBuffer buf) {
		Record key = new Record(buf);
		int nadrs = (buf.limit() - key.bufSize()) / 4;
		long[] adrs = new long[nadrs];
		buf.position(key.bufSize());
		for (int i = 0; i < nadrs; ++i)
			adrs[i] = Mmfile.intToOffset(buf.getInt());
		return new Slot(key, adrs);
	}
	
	@Override
	public String toString() {
		String s = key.toString();
		for (long adr : adrs)
			s += " " + adr;
		return s;
	}
	
	public boolean isEmpty() {
		return key.isEmpty();
	}
}
