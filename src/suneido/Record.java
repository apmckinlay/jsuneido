package suneido;

import java.nio.ByteBuffer;

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
public class Record {
	class Type {
		final static byte BYTE = 'c';
		final static byte SHORT = 's';
		final static byte INT = 'l';
	}
	class Offset {
		final static int TYPE = 0;		// byte
		final static int NFIELDS = 1;	// short
		final static int SIZE = 3;		// byte, short, or int <= type
	};
	
	ByteBuffer buf;
	Rep rep;
	
	Record(int sz) {
		buf = ByteBuffer.allocate(sz);
		if (sz < 0x100)
			setType(Type.BYTE);
		else if (sz < 0x10000)
			setType(Type.SHORT);
		else
			setType(Type.INT);
		init();
	}
	Record(ByteBuffer buf) {
		this.buf = buf;
		init();
	}
	private void init() {
		switch (getType()) {
		case Type.BYTE : rep = new ByteRep();
		case Type.SHORT : rep = new ShortRep();
		case Type.INT : rep = new IntRep();
		}
	}
	
	void add(byte[] data) {
		int n = getNfields();
		int offset = rep.getOffset(n - 1) + data.length;
		rep.setOffset(n++, offset);
		buf.position(offset);
		buf.put(data);
	}
	ByteBuffer get(int i) {
		if (i >= getNfields())
			return ByteBuffer.allocate(0);
		int start = rep.getOffset(i);
		buf.position(start);
		ByteBuffer result = buf.slice();
		int end = rep.getOffset(i - 1);
		result.limit(end - start);
		return result;
	}
	
	void setType(byte t) {
		buf.put(Offset.TYPE, t);
	}
	byte getType() {
		return buf.get(Offset.TYPE);
	}
	void setNfields(short nfields) {
		buf.putShort(Offset.NFIELDS, nfields);
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
