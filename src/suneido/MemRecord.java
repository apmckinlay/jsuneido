package suneido;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Used to create a record in memory
 * and then store it in the database.
 * @author Andrew McKinlay
 */
public class MemRecord {
	private ArrayList<byte[]> values;
	
	void add(byte[] data) {
		values.add(data);
	}
	byte[] get(int i) {
		return values.get(i);
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
		BufRecord r = new BufRecord(buf);
		for (byte[] each : values)
			r.add(each);
		return r;
	}
}
