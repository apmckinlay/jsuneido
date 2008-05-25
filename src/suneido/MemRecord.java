package suneido;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Used to create a record in memory
 * and then store it in the database.
 * @author Andrew McKinlay
 */
public class MemRecord {
	private ArrayList<byte[]> values = new ArrayList<byte[]>();
	
	void add(byte[] data) {
		values.add(data);
	}
	byte[] get(int i) {
		return i < values.size() ? values.get(i) : new byte[0];
	}
	
	/**
	 * @return The size of buffer required to store the data
	 * in BufRecord format.
	 */
	int bufsize() {
		int size = 0;
		for (byte[] each : values)
			size += each.length;
		return BufRecord.bufsize(values.size(), size);
	}
	
	/**
	 * Stores the values into a ByteBuffer using BufRecord.
	 * The ByteBuffer will normally be a slice of a memory mapped region
	 * of the database file.
	 * @param buf	A ByteBuffer of bufsize() or greater.
	 * @return	The BufRecord used to store into buf.
	 */
	BufRecord store(ByteBuffer buf) {
		BufRecord r = new BufRecord(buf, bufsize());
		for (byte[] each : values)
			r.add(each);
		return r;
	}
}
