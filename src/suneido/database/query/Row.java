package suneido.database.query;

import static suneido.Suneido.verify;

import java.nio.ByteBuffer;
import java.util.*;

import suneido.*;
import suneido.database.Record;
import suneido.database.Transaction;

// maybe it would be simpler to attach header to row
// rather than passing it in all the time

public class Row {
	final Record[] data;
	public long recadr = 0; // if Row contains single update-able record, this
							// is its address
	private Transaction tran = null;
	private SuRecord surec = null;

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

	// used by Project & Extend
	public Row(Row row, Record... recs) {
		data = new Record[row.data.length + recs.length];
		System.arraycopy(row.data, 0, data, 0, row.data.length);
		System.arraycopy(recs, 0, data, row.data.length, recs.length);
	}

	public Row(Row row1, Row row2) {
		data = new Record[row1.data.length + row2.data.length];
		System.arraycopy(row1.data, 0, data, 0, row1.data.length);
		System.arraycopy(row2.data, 0, data, row1.data.length, row2.data.length);
	}

	@Override
	public String toString() {
		String s = "";
		for (Record r : data)
			s += r.toString();
		return s;
	}

	public int size() {
		return data.length;
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

	public Record getFirstData() {
		return data[data.length > 1 ? 1 : 0]; // 0 is index key
	}

	ByteBuffer getrawval(Header hdr, String col) {
		Which w = find(hdr, col);
		if (w != null)
			return getraw(w);
		// else rule
		return surec(hdr).getdata(SuString.valueOf(col)).pack();
	}

	public SuRecord surec(Header hdr) {
		if (surec == null)
			surec = new SuRecord(this, hdr, tran);
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
		return surec(hdr).getdata(SuString.valueOf(col));
	}

	static class Which {
		int di; // index into flds
		int ri; // index into flds[i]

		Which(int di, int ri) {
			this.di = di;
			this.ri = ri;
		}
	}

	public void setTransaction(Transaction tran) {
		this.tran = tran;
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

	public Iterator<Entry> iterator(Header hdr) {
		return new Iter(hdr.flds);
	}

	private class Iter implements Iterator<Entry> {
		private final List<List<String>> fields;
		private int i = 0; // index into fields/data
		private int j = 0; // index into fields[i]
		private final int imax;
		private int jmax;

		Iter(List<List<String>> fields) {
			this.fields = fields;
			imax = Math.min(data.length, fields.size());
			set_jmax();
			skipempty();
		}

		private void set_jmax() {
			jmax = Math.min(data[i].size(), fields.get(i).size());
		}

		private void skipempty() {
			while (j >= jmax) {
				if (++i >= imax)
					break;
				j = 0;
				set_jmax();
			}
		}

		public boolean hasNext() {
			return i < imax;
		}

		public Entry next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Entry e = new Entry(fields.get(i).get(j), data[i].getraw(j));
			++j;
			skipempty();
			return e;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public static class Entry {
		public final String field;
		public final ByteBuffer value;

		public Entry(String field, ByteBuffer value) {
			this.field = field;
			this.value = value;
		}
	}

}
