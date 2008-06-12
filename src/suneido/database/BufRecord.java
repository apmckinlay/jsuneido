package suneido.database;

import java.nio.ByteBuffer;

import suneido.Packable;
import suneido.SuException;
import suneido.SuNumber;
import static suneido.Suneido.verify;

/**
 * Used by database to store field values.
 * Provides a "view" onto a ByteBuffer.
 * <p>Format is:<br>
 * - one byte type = 'c', 's', 'l'<br>
 * - short n = number of fields<br>
 * - size (also referenced as offset[-1])<br>
 * - array of offsets<br>
 * size and array elements are of the type 
 * 
 * @author Andrew McKinlay
 *
 */
public class BufRecord implements suneido.Packable, Comparable<BufRecord> {
	private Rep rep;
	private ByteBuffer buf;
	
	private static class Type {
		final static byte BYTE = 'c';
		final static byte SHORT = 's';
		final static byte INT = 'l';
	}
	private static class Offset {
		final static int TYPE = 0;		// byte
		final static int NFIELDS = 1;	// short
		final static int SIZE = 3;		// byte, short, or int <= type
	}
	
	/**
	 * Create a new BufRecord, allocating a new ByteBuffer
	 * @param sz The required size, including both data and offsets
	 */
	public BufRecord(int size) {
		this(ByteBuffer.allocate(size), size);
	}
	
	/**
	 * Create a new BufRecord using a supplied ByteBuffer.
	 * @param buf
	 * @param size The size of the buffer. Used to determine the required representation.
	 */
	public BufRecord(ByteBuffer buf, int size) {
		verify(size <= buf.limit());
		this.buf = buf;
		setType(type(size));
		init();
		setSize(size);
		setNfields(0);
	}
	private static byte type(int size) {
		return size < 0x100 ? Type.BYTE : size < 0x10000 ? Type.SHORT : Type.INT;
	}
	
	/**
	 * Create a BufRecord on an existing ByteBuffer in BufRecord format.
	 * @param buf Must be in BufRecord format.
	 */
	public BufRecord(ByteBuffer buf) {
		this.buf = buf;
		init();
	}
	private void init() {
		switch (getType()) {
		case Type.BYTE :	rep = new ByteRep(); break ;
		case Type.SHORT :	rep = new ShortRep(); break ;
		case Type.INT :		rep = new IntRep(); break ;
		default :			throw SuException.unreachable();
		}
	}
	
	public String toString() {
		return "BufRecord type " + (char) getType() + " nfields " + getNfields() + " size " + getSize();
	}
	
	ByteBuffer getBuf() {
		return buf;
	}
	
	public BufRecord dup() {
		ByteBuffer dst = ByteBuffer.allocate(packSize());
		pack(dst);
		return new BufRecord(dst);
	}
	
	// add's ========================================================
	
	public void add(byte[] data) {
		buf.position(alloc(data.length));
		buf.put(data);
	}
	public void add(ByteBuffer src) {
		add(src, 0, src.limit());
	}
	public void add(ByteBuffer src, int pos, int len) {
		buf.position(alloc(len));
		for (int i = 0; i < len; ++i)
			buf.put(src.get(pos + i));
	}
	public void add(Packable x) {
		buf.position(alloc(x.packSize()));
		x.pack(buf);
	}
	private int alloc(int len) {
		int n = getNfields();
		int offset = rep.getOffset(n - 1) - len;
		rep.setOffset(n, offset);
		setNfields(n + 1);
		return offset;
	}

	public boolean insert(int at, Packable x) {
		int len = x.packSize();
		if (len > rep.avail())
			return false;
		int n = getNfields();
		// insert into heap
		moveLeft(rep.getOffset(n - 1), rep.getOffset(at - 1), len);
		// insert into offsets
		// adjust offsets after it (after because heap grows down)
		rep.insert1(at, n, len);
		int pos = rep.getOffset(at - 1) - len;
		rep.setOffset(at, pos);
		buf.position(pos);
		x.pack(buf);
		setNfields(n + 1);
		return true;
	}
	private void moveLeft(int start, int end, int amount) {
		for (int i = start; i < end; ++i)
			buf.put(i - amount, buf.get(i));
	}
	
	public void remove(int at) {
		int n = getNfields();
		int len = fieldSize(at);
		// remove from heap
		moveRight(rep.getOffset(n - 1), rep.getOffset(at), len);
		// remove from offsets
		// adjust offsets after it (after because heap grows down)
		rep.remove1(at, n, len);
		setNfields(n - 1);
	}
	private void moveRight(int start, int end, int amount) {
		for (int i = end - 1; i >= start; --i)
			buf.put(i + amount, buf.get(i));
	}

	// get's ========================================================
	
	private final static ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);
	public ByteBuffer get(int i) {
		if (i >= getNfields())
			return EMPTY_BUF;
		buf.position(rep.getOffset(i));
		ByteBuffer result = buf.slice();
		result.limit(fieldSize(i));
		return result;
	}
	
	private final static byte[] NO_BYTES = new byte[0];
	public byte[] getBytes(int i) {
		if (i >= getNfields())
			return NO_BYTES;
		byte[] result = new byte[fieldSize(i)];
		buf.position(rep.getOffset(i));
		buf.get(result);
		return result;
	}
	
	public long getLong(int i) {
		return SuNumber.unpackLong(get(i));
	}
	
	/**
	 * @return The number of fields in the BufRecord.
	 */
	public int size() {
		return getNfields();
	}
	
	public boolean isEmpty() {
		return size() == 0;
	}
	
	/**
	 * @param i The index of the field to get the size of.
	 * @return The size of the i'th field.
	 */
	public int fieldSize(int i) {
		if (i >= getNfields())
			return 0;
		return rep.getOffset(i - 1) - rep.getOffset(i);
	}
	/**
	 * @return The current buffer size.
	 * May be larger than the packsize.
	 */
	public int bufSize() {
		return getSize();
	}
	/**
	 * Used by MemRecord
	 * @param nfields The number of fields.
	 * @param datasize The total size of the field data.
	 * @return The minimum required buffer size.
	 */
	public static int bufSize(int nfields, int datasize) {
		int e = 1;
		int size = 1 /* type */ + 2 /* nfields */ + e /* size */ + nfields * e + datasize;
		if (size < 0x100)
			return size;
		e = 2;
		size = 1 /* type */ + 2 /* nfields */ + e /* size */ + nfields * e + datasize;
		if (size < 0x10000)
			return size;
		e = 4;
		return 1 /* type */ + 2 /* nfields */ + e /* size */ + nfields * e + datasize;
	}
	
	/**
	 * @return The minimum size the current data would fit into.
	 * 		<b>Note:</b> This may be smaller than the current buffer size.
	 */
	public int packSize() {
		int n = getNfields();
		int datasize = getSize() - rep.getOffset(n-1);
		return bufSize(n, datasize);
		}
	public void pack(ByteBuffer dst) {
		int packsize = packSize();
		if (getSize() == packsize) {
			// already "compacted" so just bulk copy
			for (int i = 0; i < packsize; ++i)
				dst.put(buf.get(i));
		} else {
			BufRecord dstRec = new BufRecord(dst.slice(), packsize);
			for (int i = 0; i < getNfields(); ++i)
				dstRec.add(buf, rep.getOffset(i), fieldSize(i));
			dst.position(dst.position() + packsize);
		}
	}
	
	private void setType(byte t) {
		buf.put(Offset.TYPE, t);
	}
	private byte getType() {
		return buf.get(Offset.TYPE);
	}
	private void setNfields(int nfields) {
		buf.putShort(Offset.NFIELDS, (short) nfields);
	}
	private short getNfields() {
		return buf.getShort(Offset.NFIELDS);
	}
	private void setSize(int sz) {
		rep.setOffset(-1, sz);
	}
	private int getSize() {
		return rep.getOffset(-1);
	}
	
	// "strategy" object to avoid switching on type.
	private abstract class Rep {
		abstract void setOffset(int i, int offset);
		abstract int getOffset(int i);
		abstract int avail(); // allows for new items offset
		private void insert1(int start, int end, int adjust) {
			for (int i = end; i > start; --i)
				setOffset(i, getOffset(i - 1) - adjust);
		}
		private void remove1(int start, int end, int adjust) {
			for (int i = start; i < end; ++i)
				setOffset(i, getOffset(i + 1) + adjust);
		}
	}
	private class ByteRep extends Rep {
		void setOffset(int i, int sz) {
			buf.put(Offset.SIZE + i + 1, (byte) sz);
		}
		int getOffset(int i) {
			return buf.get(Offset.SIZE + i + 1);
		}
		int avail() {
			int n = getNfields();
			return getOffset(n - 1) - (1 /*type*/ + 2 /*nfields*/ + 1 /*byte*/ * (n + 2));
			}
	}
	private class ShortRep extends Rep {
		void setOffset(int i, int sz) {
			buf.putShort(Offset.SIZE + 2 * (i + 1), (short) sz);
		}
		int getOffset(int i) {
			return buf.getShort(Offset.SIZE + 2 * (i + 1));
		}
		int avail() {
			int n = getNfields();
			return getOffset(n - 1) - (1 /*type*/ + 2 /*nfields*/ + 2 /*short*/ * (n + 2));
			}
	}
	private class IntRep extends Rep {
		void setOffset(int i, int sz) {
			buf.putInt(Offset.SIZE + 4 * (i + 1), sz);
		}
		int getOffset(int i) {
			return buf.getInt(Offset.SIZE + 4 * (i + 1));
		}
		int avail() {
			int n = getNfields();
			return getOffset(n - 1) - (1 /* type */ + 2 /* nfields */ + 4 /*int*/ * (n + 2));
			}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BufRecord
				? 0 == compareTo((BufRecord) other)
				: false;
	}

	public int compareTo(BufRecord rec) {
		if (this == rec)
			return 0;
		int n = Math.min(size(), rec.size());
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(i, rec);
			if (cmp != 0)
				return cmp;
		}
		return size() - rec.size();
	}
	private int compare1(int fld, BufRecord rec) {
		buf.position(rep.getOffset(fld));
		rec.buf.position(rec.rep.getOffset(fld));
		int n = Math.min(fieldSize(fld), rec.fieldSize(fld));
		for (int i = 0; i < n; ++i) {
			int cmp = buf.get() - rec.buf.get();
			if (cmp != 0)
				return cmp;
		}
		return fieldSize(fld) - rec.fieldSize(fld);
	}
}
