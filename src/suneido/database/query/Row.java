package suneido.database.query;

import static suneido.Suneido.verify;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import suneido.*;
import suneido.database.Record;
import suneido.database.Transaction;

public class Row {
	final Record[] data;
	long recadr = 0; // if Row contains single update-able record, this is its
	// address
	Transaction tran = null;
	SuContainer surec = null;

	Row(Record... data) {
		this.data = data;
	}

	public Row(Record record, long recadr) {
		data = new Record[] { record };
		this.recadr = recadr;
		verify(recadr > 0);
	}

	public Row(int n) {
		data = new Record[n];
		Arrays.fill(data, Record.MINREC);
	}

	// used by Project
	public Row(Row row, Record... recs) {
		data = new Record[row.data.length + recs.length];
		System.arraycopy(row.data, 0, data, 0, row.data.length);
		System.arraycopy(recs, 0, data, row.data.length, recs.length);
	}

	public Row(Row row1, Row row2) {
		data = new Record[row1.data.length + row2.data.length];
		System.arraycopy(row1.data, 0, data, 0, row1.data.length);
		System
				.arraycopy(row2.data, 0, data, row1.data.length,
						row2.data.length);
	}

	@Override
	public String toString() {
		String s = "";
		for (Record r : data)
			s += r.toString();
		return s;
	}

	public ByteBuffer getraw(Header hdr, String col) {
		return getraw(find(hdr, col));
	}

	public Record project(Header hdr, List<String> flds) {
		Record key = new Record();
		for (String f : flds)
			key.add(getrawval(hdr, f));
		return key;
	}

	ByteBuffer getrawval(Header hdr, String col) {
		Which w = find(hdr, col);
		if (w != null)
			return getraw(w);
		// else rule
		SuValue val = surec().getdata(SuString.valueOf(col));
		return val.pack();
	}

	private SuContainer surec() {
		if (surec == null)
			surec = new SuContainer(); // TODO surec
		return surec;
	}

	private ByteBuffer getraw(Which w) {
		return w == null ? ByteBuffer.allocate(0) : data[w.di].getraw(w.ri);
	}

	Which find(Header hdr, String col) {
		int j;
		for (int i = 0; i < data.length; ++i)
			if (data[i] != null && !data[i].isEmpty()
					&& -1 != (j = hdr.flds.get(i).indexOf(col)))
				return new Which(i, j);
		return null;
	}

	public SuValue getval(Header hdr, String col) {
		Which w = find(hdr, col);
		if (w != null || !hdr.cols.contains(col))
			return SuValue.unpack(getraw(w));
		// else rule
		return SuString.EMPTY; // TODO surec().getdata(SuString.valueOf(col));
	}

	static class Which {
		int di; // index into flds
		int ri; // index into flds[i]

		Which(int di, int ri) {
			this.di = di;
			this.ri = ri;
		}
	}

	/**
	 * Used by TempIndex and Project
	 *
	 * @return An array of either Long for database records, or ByteBuffer for
	 *         in-memory records (e.g. from Extend)
	 */
	public Object[] getRefs() {
		Object[] refs = new Object[data.length];
		for (int i = 0; i < data.length; ++i)
			refs[i] = data[i].getRef();
		return refs;
	}

	public static Row fromRefs(Object[] refs) {
		Record[] data = new Record[refs.length];
		for (int i = 0; i < data.length; ++i)
			data[i] = Record.fromRef(refs[i]);
		return new Row(data);
	}

}
