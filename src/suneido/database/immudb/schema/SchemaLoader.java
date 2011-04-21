package suneido.database.immudb.schema;

import suneido.database.Database.TN;
import suneido.database.immudb.*;

import com.google.common.collect.ImmutableList;

public class SchemaLoader {
	private final Storage stor;

	public SchemaLoader(Storage stor) {
		this.stor = stor;
	}

	public Tables load(int root, int redirs) {
		Tran tran = new Tran(stor, redirs);
		Record r = new Record(stor.buffer(root));
		BtreeInfo info = Index.btreeInfo(r);
		Btree indexesIndex = new Btree(tran, info);

		int adr = indexesIndex.get(key(TN.COLUMNS, "table,column"));
		r = new Record(stor.buffer(adr));
		info = Index.btreeInfo(r);
		Btree columnsIndex = new Btree(tran, info);

		adr = indexesIndex.get(key(TN.TABLES, "table"));
		r = new Record(stor.buffer(adr));
		info = Index.btreeInfo(r);
		Btree tablesIndex = new Btree(tran, info);

		TablesReader tr = new TablesReader(tablesIndex);
		ColumnsReader cr = new ColumnsReader(columnsIndex);
		IndexesReader ir = new IndexesReader(indexesIndex);
		Tables.Builder tsb = new Tables.Builder();
		while (true) {
			Record tblrec = tr.next();
			if (tblrec == null)
				break;
			Columns columns = cr.next();
			Indexes indexes = ir.next(columns);
			Table table = new Table(tblrec, columns, indexes);
			tsb.add(table);
		}
		return tsb.build();
	}

	private Record key(Object... values) {
		RecordBuilder rb = new RecordBuilder();
		for (Object x : values)
			rb.add(x);
		return rb.build();
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
			return recordFromKey(key);
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
			Record r = recordFromKey(key);
			return new Column(r);
		}
	}

	private class IndexesReader {
		Btree.Iter iter;
		Record cur;
		ImmutableList.Builder<Index> list = ImmutableList.builder();

		IndexesReader(Btree indexesIndex) {
			iter = indexesIndex.iterator();
			iter.next();
			cur = recordFromKey(iter.cur());
		}
		Indexes next(Columns cols) {
			if (cur == null)
				return null;
			while (true) {
				list.add(new Index(cols, cur));
				iter.next();
				if (iter.eof())
					break;
				Record prev = cur;
				cur = recordFromKey(iter.cur());
				if (Index.tblnum(prev) != Index.tblnum(cur)) {
					Indexes result = new Indexes(list.build());
					list = ImmutableList.builder();
					return result;
				}
			}
			cur = null;
			return new Indexes(list.build());
		}
	}

	private Record recordFromKey(Record key) {
		int adr = Btree.getAddress(key);
		return new Record(stor.buffer(adr));
	}

}
