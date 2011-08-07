package suneido.immudb;

import suneido.immudb.Bootstrap.TN;

import com.google.common.collect.ImmutableList;

/**
 * Load the database schema into memory at startup.
 * Used by Database.open
 */
class SchemaLoader {
	private final ReadTransaction t;

	static Tables load(ReadTransaction t) {
		SchemaLoader sl = new SchemaLoader(t);
		return sl.load();
	}

	private SchemaLoader(ReadTransaction t) {
		this.t = t;
	}

	private Tables load() {
		Btree tablesIndex = t.getIndex(TN.TABLES, 0); // table
		Btree columnsIndex = t.getIndex(TN.COLUMNS, 0, 1); // table, field
		Btree indexesIndex = t.getIndex(TN.INDEXES, 0, 1); // table, columns

		TablesReader tr = new TablesReader(tablesIndex);
		ColumnsReader cr = new ColumnsReader(columnsIndex);
		IndexesReader ir = new IndexesReader(indexesIndex);
		Tables.Builder tsb = new Tables.Builder();
		while (true) {
			Record tblrec = tr.next();
			if (tblrec == null)
				break;
			Columns columns = cr.next();
			Indexes indexes = ir.next();
			Table table = new Table(tblrec, columns, indexes);
			tsb.add(table);
		}
		return tsb.build();
	}

	private class TablesReader {
		Btree.Iter iter;

		TablesReader(Btree tablesIndex) {
			iter = tablesIndex.iterator();
		}
		Record next() {
			iter.next();
			if (iter.eof())
				return null;
			Record key = iter.cur();
			return recordFromSlot(key);
		}
	}

	private class ColumnsReader {
		Btree.Iter iter;
		Column cur;
		ImmutableList.Builder<Column> list = ImmutableList.builder();

		ColumnsReader(Btree columnsIndex) {
			iter = columnsIndex.iterator();
			iter.next();
			cur = column(iter.cur());
		}
		Columns next() {
			if (cur == null)
				return null;
			while (true) {
				list.add(cur);
				iter.next();
				if (iter.eof())
					break;
				Column prev = cur;
				cur = column(iter.cur());
				if (prev.tblnum != cur.tblnum) {
					Columns cols = new Columns(list.build());
					list = ImmutableList.builder();
					return cols;
				}
			}
			cur = null;
			return new Columns(list.build());
		}
		Column column(Record key) {
			return new Column(recordFromSlot(key));
		}
	}

	private class IndexesReader {
		Btree.Iter iter;
		Index cur;
		ImmutableList.Builder<Index> list = ImmutableList.builder();

		IndexesReader(Btree indexesIndex) {
			iter = indexesIndex.iterator();
			iter.next();
			cur = index();
		}
		Indexes next() {
			if (cur == null)
				return null;
			while (true) {
				list.add(cur);
				iter.next();
				if (iter.eof())
					break;
				Index prev = cur;
				cur = index();
				if (prev.tblnum != cur.tblnum) {
					Indexes result = new Indexes(list.build());
					list = ImmutableList.builder();
					return result;
				}
			}
			cur = null;
			return new Indexes(list.build());
		}
		private Index index() {
			return new Index(recordFromSlot(iter.cur()));
		}
	}

	private Record recordFromSlot(Record slot) {
		int adr = Btree.getAddress(slot);
		return t.getrec(adr);
	}

}
