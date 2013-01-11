/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.SuException.unreachable;
import static suneido.util.Util.listToCommas;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.nil;
import static suneido.util.Util.startsWith;
import static suneido.util.Verify.verify;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import suneido.Suneido;
import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.intfc.database.Transaction;
import suneido.util.CommaStringBuilder;
import suneido.util.Util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class Table extends Query {
	private final String table;
	final suneido.intfc.database.Table tbl;
	private boolean first = true;
	private boolean rewound = true;
	private final Keyrange sel = new Keyrange();
	private Header hdr;
	private String icols; // null to use first index
	private Transaction tran;
	final boolean singleton; // i.e. key()
	private List<String> idx = noFields;
	private final Impl impl;
	IndexIter iter;

	public Table(Transaction tran, String tablename) {
		this.tran = tran;
		table = tablename;
		tbl = tran.ck_getTable(table);
		singleton = tbl.singleton();
		impl = table.equals("indexes") ? new IndexesImpl() : new Impl();
	}

	@Override
	public String toString() {
		return table + (nil(idx) ? "" : "^" + listToParens(idx));
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		assert columns().containsAll(needs);
		if (!columns().containsAll(index))
			return IMPOSSIBLE;

		List<List<String>> indexes = indexes();
		if (nil(indexes))
			return IMPOSSIBLE;
		if (singleton) {
			idx = nil(index) ? indexes.get(0) : index;
			return recordsize();
		}

		Set<Idx> idxs = getIndexSizes(indexes);

		double cost1 = IMPOSSIBLE;
		double cost2 = IMPOSSIBLE;
		double cost3 = IMPOSSIBLE;

		Idx idx1, idx2 = null, idx3 = null;
		if (null != (idx1 = match(idxs, index, needs)))
			// index found that meets all needs
			cost1 = idx1.size; // cost of reading index
		if (!nil(firstneeds) && null != (idx2 = match(idxs, index, firstneeds)))
			// index found that meets firstneeds
			// assume this means we only have to read 75% of data
			cost2 = .75 * totalSize() + // cost of reading data
					idx2.size; // cost of reading index
		if (!nil(needs) && null != (idx3 = match(idxs, index, noFields)))
			cost3 = totalSize() + // cost of reading data
					idx3.size; // cost of reading index

		if (cost1 <= cost2 && cost1 <= cost3) {
			idx = (idx1 == null) ? null : idx1.index;
			return cost1;
		} else if (cost2 <= cost1 && cost2 <= cost3) {
			assert idx2 != null;
			idx = idx2.index; // suppress warning
			return cost2;
		} else {
			assert idx3 != null; // suppress warning
			idx = idx3.index;
			return cost3;
		}
	}

	private Set<Idx> getIndexSizes(List<List<String>> indexes) {
		ImmutableSet.Builder<Idx> idxs = ImmutableSet.builder();
		for (List<String> index : indexes)
			idxs.add(new Idx(index, indexSize(index)));
		return idxs.build();
	}

	private static class Idx {
		final List<String> index;
		final int size;

		public Idx(List<String> index, int size) {
			this.index = index;
			this.size = size;
		}
		@Override
		public String toString() {
			return "Idx [index=" + index + ", size=" + size + "]";
		}
	}

	@Override
	List<String> columns() {
		return impl.columns();
	}

	@Override
	List<List<String>> indexes() {
		return tbl.indexesColumns();
	}

	@Override
	public List<List<String>> keys() {
		return tbl.keysColumns();
	}

	@Override
	int columnsize() {
		return recordsize() / columns().size();
	}

	@Override
	int recordsize() {
		int nrecs = nrecs();
		return nrecs <= 0 ? 0 : (int) (totalSize() / nrecs);
	}

	@Override
	double nrecords() {
		return nrecs();
	}

	int num() {
		return tbl.num();
	}

	int nrecs() {
		return tran.tableCount(tbl.num());
	}

	long totalSize() {
		return tran.tableSize(tbl.num());
	}

	// find the smallest index with index as a prefix & containing needs
	private static Idx match(Set<Idx> idxs,
			List<String> index, Collection<String> needs) {
		Idx best = null;
		for (Idx idx : idxs)
			if (startsWith(idx.index, index) && idx.index.containsAll(needs))
				if (best == null || best.size > idx.size)
					best = idx;
		return best;
	}

	int keySize(List<String> index) {
		return tran.keySize(tbl.num(), listToCommas(index));
	}

	int indexSize(List<String> index) {
		return tran.indexSize(tbl.num(), listToCommas(index)) + index.size();
		// add index.size() to favor shorter indexes
	}

	/* package */void select_index(List<String> index) {
		// used by Select::optimize
		idx = index;
	}

	@Override
	public void setTransaction(Transaction tran) {
		if (this.tran == tran)
			return;
		this.tran = tran;
		set_ix();
		if (iter != null)
			iter = tran.iter(tbl.num(), icols, iter);
	}

	@Override
	public Row get(Dir dir) {
		if (first) {
			first = false;
			iterate_setup(dir);
		}
		if (rewound) {
			rewound = false;
			iter = iter();
		}
		switch (dir) {
		case NEXT :
			iter.next();
			break;
		case PREV :
			iter.prev();
			break;
		default:
			throw unreachable();
		}
		if (iter.eof()) {
			rewound = true;
			return null;
		}

		Row row = new Row(iter.curKey(), impl.process(tran.input(iter.keyadr())));

		if (singleton && !sel.contains(row.project(hdr, idx))) {
			rewound = true;
			return null;
		}
		return row;
	}

	private IndexIter iter() {
		return singleton || sel == null
				? tran.iter(tbl.num(), icols)
				: tran.iter(tbl.num(), icols, sel.org, sel.end);
	}

	private void iterate_setup(Dir dir) {
		hdr = header();
		set_ix();
	}

	private void set_ix() {
		icols = nil(idx) || singleton ? null : listToCommas(idx);
	}

	@Override
	public Header header() {
		// MAYBE cache
		List<String> index = singleton || idx == null ? noFields : idx;
		List<String> fields = impl.fields();
		return new Header(asList(index, fields), columns());
	}

	@Override
	boolean singleDbTable() {
		return true;
	}

	@Override
	public void rewind() {
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		verify(startsWith(idx, index));
		sel.set(from, to);
		rewound = true;
	}

	void set_index(List<String> index) {
		idx = index;
		set_ix();
		rewound = true;
	}

	@Override
	public boolean updateable() {
		return true;
	}
	@Override
	public int tblnum() {
		return tbl.num();
	}

	@Override
	public void output(Record r) {
		tran.addRecord(table, r);
	}

	@Override
	public void close() {
	}

	private class Impl {
		List<String> fields() {
			return tbl.getFields();
		}

		List<String> columns() {
			return tbl.getColumns();
		}

		Record process(Record rec) {
			return rec;
		}

	}

	private class IndexesImpl extends Impl {

		@Override
		List<String> fields() {
			return add_columns(super.fields());
		}

		@Override
		List<String> columns() {
			return add_columns(super.columns());
		}

		private List<String> add_columns(List<String> list) {
			ImmutableList.Builder<String> b = ImmutableList.builder();
			b.addAll(list);
			b.add("columns");
			return b.build();
		}

		@Override
		Record process(Record rec) {
			RecordBuilder rb = Suneido.dbpkg.recordBuilder();
			rb.addAll(rec);
			for (int i = rec.size(); i < 6; ++i)
				rb.add("");
			rb.add(convert(rec));
			return rb.build();
		}

		private String convert(Record rec) {
			String colnums = rec.getString(1);
			if (colnums.isEmpty())
				return "";
			List<String> fields = tran.ck_getTable(rec.getInt(0)).getFields();
			CommaStringBuilder csb = new CommaStringBuilder();
			for (String col : Util.commaSplitter(colnums))
				csb.add(fields.get(Integer.parseInt(col)));
			return csb.toString();
		}

	}

}
