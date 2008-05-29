package suneido;

import java.nio.ByteBuffer;
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
public class BufRecord {
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
	BufRecord(int sz) {
		this(ByteBuffer.allocate(sz), sz);
	}
	
	/**
	 * Create a new BufRecord using a supplied ByteBuffer.
	 * @param buf
	 * @param sz The size of the buffer. Used to determine the required representation.
	 */
	BufRecord(ByteBuffer buf, int sz) {
		verify(buf.limit() >= sz);
		this.buf = buf;
		if (sz < 0x100)
			setType(Type.BYTE);
		else if (sz < 0x10000)
			setType(Type.SHORT);
		else
			setType(Type.INT);
		init();
		setSize(sz);
	}
	
	/**
	 * Create a BufRecord on an existing ByteBuffer in BufRecord format.
	 * @param buf Must be in BufRecord format.
	 */
	BufRecord(ByteBuffer buf) {
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
	
	public ByteBuffer getBuf() {
		return buf;
	}
	
	void add(byte[] data) {
		int n = getNfields();
		int offset = rep.getOffset(n - 1) - data.length;
		rep.setOffset(n, offset);
		setNfields(n + 1);
		buf.position(offset);
		buf.put(data);
	}
	public ByteBuffer get(int i) {
		if (i >= getNfields())
			return ByteBuffer.allocate(0);
		int start = rep.getOffset(i);
		buf.position(start);
		ByteBuffer result = buf.slice();
		int end = rep.getOffset(i - 1);
		result.limit(end - start);
		return result;
	}
	public byte[] getBytes(int i) {
		if (i >= getNfields())
			return new byte[0];
		int start = rep.getOffset(i);
		int end = rep.getOffset(i - 1);
		byte[] result = new byte[end - start];
		buf.position(start);
		buf.get(result);
		return result;
	}
	/**
	 * @param i The index of the field to get the size of.
	 * @return The size of the i'th field.
	 */
	public int size(int i) {
		if (i >= getNfields())
			return 0;
		return rep.getOffset(i - 1) - rep.getOffset(i);
	}
	public static int bufsize(int nfields, int datasize) {
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
	
	void setType(byte t) {
		buf.put(Offset.TYPE, t);
	}
	byte getType() {
		return buf.get(Offset.TYPE);
	}
	void setNfields(int nfields) {
		buf.putShort(Offset.NFIELDS, (short) nfields);
	}
	short getNfields() {
		return buf.getShort(Offset.NFIELDS);
	}
	void setSize(int sz) {
		rep.setOffset(-1, sz);
	}
	int getSize() {
		return rep.getOffset(-1);
	}
	
	/**
	 * A "strategy" object to avoid switching on type all the time.
	 */
	abstract class Rep {
		abstract void setOffset(int i, int offset);
		abstract int getOffset(int i);
	}
	class ByteRep extends Rep {
		void setOffset(int i, int sz) {
			buf.put(Offset.SIZE + i + 1, (byte) sz);
		}
		int getOffset(int i) {
			return buf.get(Offset.SIZE + i + 1);
		}
	}
	class ShortRep extends Rep {
		void setOffset(int i, int sz) {
			buf.putShort(Offset.SIZE + 2 * (i + 1), (short) sz);
		}
		int getOffset(int i) {
			return buf.getShort(Offset.SIZE + 2 * (i + 1));
		}
	}
	class IntRep extends Rep {
		void setOffset(int i, int sz) {
			buf.putInt(Offset.SIZE + 4 * (i + 1), sz);
		}
		int getOffset(int i) {
			return buf.getInt(Offset.SIZE + 4 * (i + 1));
		}
	}
}
