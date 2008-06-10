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
		return key.packSize() + 4 * adrs.length;
	}
	public void pack(ByteBuffer buf) {
		key.pack(buf);
		for (long adr : adrs)
			buf.putLong(adr);
	}
	public static Slot unpack(ByteBuffer buf) {
		return null; //TODO
	}
}
