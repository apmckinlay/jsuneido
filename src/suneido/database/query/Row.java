/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.base.Ascii;
import com.google.common.base.MoreObjects;

import suneido.SuRecord;
import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;
import suneido.database.immudb.Transaction;
import suneido.database.server.DbmsTran;
import suneido.runtime.Pack;
import suneido.util.Util;

// might be simpler to attach header to row
// rather than passing it in all the time

public class Row {
	final Record[] data;
	private DbmsTran tran = null;
	private SuRecord surec = null; // cache
	private Header surecHdr = null;
	private static final Row emptyrow = new Row();

	public Row(Record... data) {
		this.data = data;
	}

	public Row(int n) {
		this(new Record[n]);
		Arrays.fill(data, Dbpkg.MIN_RECORD);
	}

	public Row(Row row1, Row row2) {
		if (row1 == null)
			row1 = emptyrow;
		if (row2 == null)
			row2 = emptyrow;
		data = new Record[row1.data.length + row2.data.length];
		System.arraycopy(row1.data, 0, data, 0, row1.data.length);
		System.arraycopy(row2.data, 0, data, row1.data.length, row2.data.length);
	}

	public Row with(Record... recs) {
		Record[] newdata = new Record[data.length + recs.length];
		System.arraycopy(data, 0, newdata, 0, data.length);
		System.arraycopy(recs, 0, newdata, data.length, recs.length);
		return new Row(newdata);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Record r : data)
			if (r == null)
                sb.append(" null ");
            else
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
		var w = find(hdr, col);
		if (w == null && col.endsWith("_lower!")) {
			var base = Util.beforeLast(col, "_");
			w = find(hdr, base);
			if (w != null) {
				var b = getraw(w);
				var x = Pack.unpack(b);
				if (x instanceof String s)
					return Pack.pack(Ascii.toLowerCase(s));
				return b;
			}
		}
		return getraw(w);
	}

	public Record project(Header hdr, List<String> flds) {
		RecordBuilder key = new RecordBuilder();
		for (String f : flds)
			key.add(getrawval(hdr, f));
		return key.build();
	}

	/** used by TempIndex */
	Record project(Header hdr, List<String> flds, int adr) {
		RecordBuilder key = new RecordBuilder();
		for (String f : flds)
			key.add(getrawval(hdr, f));
		key.add(adr);
		return key.build();
	}

	public Record firstData() {
		return data[data.length > 1 ? 1 : 0]; // 0 is usually index key
	}

	ByteBuffer getrawval(Header hdr, String col) {
		Which w = find(hdr, col);
		if (w != null)
			return getraw(w);
		if (hdr.hasRule(col)) {
			return Pack.pack(surec(hdr).get(col)); // handle rules
		}
		return Record.MIN_FIELD;
	}

	public int address() {
		return firstData().address();
	}

	public SuRecord surec(Header hdr) {
		if (surec == null || !hdr.equals(surecHdr)) {
			surecHdr = hdr;
			surec = new SuRecord(this, hdr, tran);
		}
		return surec;
	}

	private ByteBuffer getraw(Which w) {
		return w == null ? Record.MIN_FIELD : data[w.di].getRaw(w.ri);
	}

	Which find(Header hdr, String col) {
		if ("-".equals(col))
			return null;
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
		if (suneido.database.immudb.Table.isSpecialField(col)) {
			String base = Util.beforeLast(col, "_");
			w = find(hdr, base);
			return (w == null) ? ""
				: Ascii.toLowerCase(data[w.di].getString(w.ri));
		}
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
			return MoreObjects.toStringHelper(this)
					.addValue(di)
					.addValue(ri)
					.toString();
		}
	}

	public static boolean equal(Header hdr1, Row r1, Header hdr2, Row r2,
			List<String> cols) {
		for (String col : cols)
			if (!get(hdr1, r1, col).equals(get(hdr2, r2, col)))
				return false;
		return true;
	}

	private static ByteBuffer get(Header hdr, Row row, String col) {
		if (col.endsWith("_lower!")) {
			col = Util.beforeLast(col, "_");
		}
		return row.getraw(hdr, col);
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
	 * Used by Project lookup
	 * @return An array of either Integer for database records,
	 * 		   or ByteBuffer for in-memory records (e.g. from Extend)
	 */
	public Object[] getRefs() {
		assert data.length > 1 && (data.length % 2) == 0;
		Object[] refs = new Object[data.length / 2];
		int ri = 0;
		for (int di = 1; di < data.length; di += 2)
			refs[ri++] = checkNotNull(data[di].getRef());
		return refs;
	}

	/**
	 * Used by TempIndex.
	 * Adds to refs either Integers for database records,
	 * or ByteBuffers for in-memory records (e.g. from Extend)
	 * NOTE: Depends on actual data being in every second data record.
	 * @return The starting position in refs.
	 */
	int getRefs(List<Object> refs) {
		assert data.length > 1 && (data.length % 2) == 0;
		int offset = refs.size();
		for (int di = 1; di < data.length; di += 2)
			refs.add(checkNotNull(data[di].getRef()));
		refs.add(END);
		return offset;
	}
	static Row fromRefs(Transaction t, List<Object> refs, int offset) {
		int n = size(refs, offset);
		Record[] data = new Record[2 * n];
		int di = 0;
		for (int i = 0; i < n; ++i) {
			data[di++] = null;
			data[di++] = t.fromRef(refs.get(offset + i));
		}
		return new Row(data);
	}
	private static int size(List<Object> refs, int i) {
		int n = 0;
		for (; refs.get(i) != END; ++i)
			++n;
		return n;
	}

	private final static Object END = new Object();

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
			jmax = Math.min(datasize(i), fields.get(i).size());
		}

		private int datasize(int i) {
			return data[i] == null ? 0 : data[i].size();
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
