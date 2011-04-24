package suneido.database.immudb.schema;

import suneido.database.immudb.*;
import suneido.database.immudb.schema.Bootstrap.TN;

import com.google.common.collect.ImmutableList;

public class SchemaLoader {
	private final Storage stor;
	private Tran tran;

	public SchemaLoader(Storage stor) {
		this.stor = stor;
	}

	public Tables load(DbInfo dbinfo, Redirects redirs) {
		tran = new Tran(stor, redirs);

		Btree tablesIndex = getBtree(dbinfo, TN.TABLES);
		Btree columnsIndex = getBtree(dbinfo, TN.COLUMNS);
		Btree indexesIndex = getBtree(dbinfo, TN.INDEXES);

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

	public Btree getBtree(DbInfo dbinfo, int tblnum) {
		TableInfo ti = dbinfo.get(tblnum);
		IndexInfo ii = ti.firstIndex();
		return new Btree(tran, ii);
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
			return new Column(recordFromKey(key));
		}
	}

	private class IndexesReader {
		Btree.Iter iter;
		Index cur;
		ImmutableList.Builder<Index> list = ImmutableList.builder();

		IndexesReader(Btree indexesIndex) {
			iter = indexesIndex.iterator();
			iter.next();
assert ! iter.eof();
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
			return new Index(recordFromKey(iter.cur()));
		}
	}

	private Record recordFromKey(Record key) {
		int adr = Btree.getAddress(key);
		return tran.getrec(adr);
	}

}
