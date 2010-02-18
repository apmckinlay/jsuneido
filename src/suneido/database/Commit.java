package suneido.database;

import java.text.SimpleDateFormat;

import suneido.util.ByteBuf;

public class Commit {
	private final ByteBuf buf;
	final static int INTSIZE = 4;
	final static int LONGSIZE = 8;
	final static int DATE = 0;
	final static int NUM = DATE + LONGSIZE;
	final static int NCREATES = NUM + INTSIZE;
	final static int NDELETES = NCREATES + INTSIZE;
	final static int CREATES = NDELETES + INTSIZE;
	final int DELETES;
	final int CKSUM;

	public Commit(ByteBuf buf) {
		this.buf = buf;
		DELETES = CREATES + getNCreates() * INTSIZE;
		CKSUM = DELETES + getNDeletes() * INTSIZE;
	}

	public long getDate() {
		return buf.getLong(DATE);
	}

	public int getNum() {
		return buf.getInt(NUM);
	}

	public int getNCreates() {
		return buf.getInt(NCREATES);
	}

	public int getNDeletes() {
		return buf.getInt(NDELETES);
	}

	public long getCreate(int i) {
		return Mmfile.intToOffset(buf.getInt(CREATES + i * INTSIZE));
	}

	public void putCreate(int i, long offset) {
		buf.putInt(CREATES + i * INTSIZE, Mmfile.offsetToInt(offset));
	}

	public long getDelete(int i) {
		return Mmfile.intToOffset(buf.getInt(DELETES + i * INTSIZE));
	}

	public void putDelete(int i, long offset) {
		buf.putInt(DELETES + i * INTSIZE, Mmfile.offsetToInt(offset));
	}

	public long getChecksum() {
		return buf.getInt(CKSUM);
	}

	public void putChecksum(long value) {
		buf.putInt(CKSUM, (int) value);
	}

	public int sizeWithoutChecksum() {
		return CKSUM;
	}

	@Override
	public String toString() {
		return toStringBase().toString();
	}

	public String toStringVerbose() {
		StringBuilder sb = toStringBase();
		sb.append(" ");
		for (int i = 0; i < getNCreates(); ++i)
			sb.append("+" + (getCreate(i) - 4));
		for (int i = 0; i < getNDeletes(); ++i)
			sb.append("-" + (getDelete(i) - 4));
		return sb.toString();
	}

	private StringBuilder toStringBase() {
		StringBuilder sb = new StringBuilder();
		sb.append("Commit ")
				.append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(getDate()))
				.append(" num ")
				.append(getNum());
		return sb;
	}

}
