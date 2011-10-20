package suneido.database.query;

import static suneido.Suneido.dbpkg;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import suneido.SuRecord;
import suneido.database.server.DbmsTran;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.intfc.database.Transaction;
import suneido.language.Pack;

import com.google.common.base.Objects;

// might be simpler to attach header to row
// rather than passing it in all the time

public class Row {
	final Record[] data;
	private DbmsTran tran = null;
	private SuRecord surec = null; // cache

	public Row(Record... data) {
		this.data = data;
	}

	public Row(int n) {
		this(new Record[n]);
		Arrays.fill(data, dbpkg.minRecord());
	}

	// used by Project & Extend
	public Row(Row row, Record... recs) {
		this(new Record[row.data.length + recs.length]);
		System.arraycopy(row.data, 0, data, 0, row.data.length);
		System.arraycopy(recs, 0, data, row.data.length, recs.length);
	}

	public Row(Row row1, Row row2) {
		this(new Record[row1.data.length + row2.data.length]);
		System.arraycopy(row1.data, 0, data, 0, row1.data.length);
		System.arraycopy(row2.data, 0, data, row1.data.length, row2.data.length);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Record r : data)
			sb.append(r.toString());
		return sb.toString();
	}

	public String toString(Header hdr) {
		StringBuilder sb = new StringBuilder();
		sb.append("Row{");
		for (String col : hdr.columns()) {
			String val = getval(hdr, col).toString();
			if (! "".equals(val))
				sb.append(col).append(": ").append(val).append(", ");
		}
		if (sb.length() > 4)
			sb.delete(sb.length() - 2, sb.length());
		sb.append("}");
		return sb.toString();
	}

	public int size() {
		return data.length;
	}

	public ByteBuffer getraw(Header hdr, String col) {
		return getraw(find(hdr, col));
	}

	public Record project(Header hdr, List<String> flds) {
		RecordBuilder key = dbpkg.recordBuilder();
		for (String f : flds)
			key.add(getrawval(hdr, f));
		return key.build();
	}

	public Record firstData() {
		return data[data.length > 1 ? 1 : 0]; // 0 is usually index key
	}

	ByteBuffer getrawval(Header hdr, String col) {
		Which w = find(hdr, col);
		if (w != null)
			return getraw(w);
		// else rule
		return Pack.pack(surec(hdr).get(col));
	}

	public int address() {
		return firstData().address();
	}

	public SuRecord surec(Header hdr) {
		if (surec == null)
			surec = new SuRecord(this, hdr, tran);
		return surec;
	}

	private ByteBuffer getraw(Which w) {
		return w == null ? ByteBuffer.allocate(0) : data[w.di].getRaw(w.ri);
	}

	Which find(Header hdr, String col) {
		int j;
		for (int i = 0; i < data.length; ++i)
			if (data[i] != null && !data[i].isEmpty()
					&& -1 != (j = hdr.flds.get(i).indexOf(col)))
				return new Which(i, j);
		return null;
	}

	public Object getval(Header hdr, String col) {
		Which w = find(hdr, col);
		if (w != null || ! hdr.cols.contains(col))
			return Pack.unpack(getraw(w));
		// else rule
		return surec(hdr).get(col);
	}

	static class Which {
		int di; // index into flds
		int ri; // index into flds[i]

		Which(int di, int ri) {
			this.di = di;
			this.ri = ri;
		}
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(di)
					.addValue(ri)
					.toString();
		}
	}

	public void setTransaction(DbmsTran tran) {
		this.tran = tran;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Row))
			return false;
		Row that = (Row) other;
		return Arrays.equals(this.data, that.data);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}

	/**
	 * Used by TempIndex and Project
	 *
	 * @return An array of either Long for database records,
	 * 		   or ByteBuffer for in-memory records (e.g. from Extend)
	 */
	public Object[] getRefs() {
		Object[] refs = new Object[data.length];
		for (int i = 0; i < data.length; ++i)
			refs[i] = data[i].getRef();
		return refs;
	}

	public static Row fromRefs(Transaction t, Object[] refs) {
		Record[] data = new Record[refs.length];
		for (int i = 0; i < data.length; ++i)
			data[i] = t.fromRef(refs[i]);
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

		@Override
		public boolean hasNext() {
			return i < imax;
		}

		@Override
		public Entry next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Entry e = new Entry(fields.get(i).get(j), data[i].getRaw(j));
			++j;
			skipempty();
			return e;
		}

		@Override
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
