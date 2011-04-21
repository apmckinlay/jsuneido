package suneido.database.immudb.schema;

import java.util.ArrayList;

import suneido.database.Database.TN;
import suneido.database.immudb.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class Schema {
	private final Storage stor;

	public Schema(Storage stor) {
		this.stor = stor;
	}

	public void load(int root, int redirs) {
		Tran tran = new Tran(stor, redirs);
		Record r = new Record(stor.buffer(root));
//System.out.println(r);
		BtreeInfo info = Index.btreeInfo(r);
		Btree indexesIndex = new Btree(tran, info);

		int adr = indexesIndex.get(key(TN.COLUMNS, "table,column"));
		r = new Record(stor.buffer(adr));
//System.out.println(r);
		info = Index.btreeInfo(r);
		Btree columnsIndex = new Btree(tran, info);

		adr = indexesIndex.get(key(TN.TABLES, "table"));
		r = new Record(stor.buffer(adr));
//System.out.println(r);
		info = Index.btreeInfo(r);
		Btree tablesIndex = new Btree(tran, info);

		ColumnsReader cr = new ColumnsReader(columnsIndex);
		IndexesReader ir = new IndexesReader(indexesIndex);
		while (true) {
			Columns columns = cr.next();
			if (columns == null)
				break;
System.out.println(columns);
			Indexes indexes = ir.next(columns);
System.out.println(indexes);
		}

		Btree.Iter iter;

//		iter = columnsIndex.iterator();
//		for (iter.next(); ! iter.eof(); iter.next()) {
//			Record key = iter.cur();
//			adr = Btree.getAddress(key);
//			r = new Record(stor.buffer(adr));
//System.out.println("column " + r);
//		}
/*
		iter = indexesIndex.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record key = iter.cur();
			adr = Btree.getAddress(key);
			r = new Record(stor.buffer(adr));
System.out.println("index " + r);
		}

		iter = tablesIndex.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record key = iter.cur();
			adr = Btree.getAddress(key);
			r = new Record(stor.buffer(adr));
System.out.println("table " + r);
		}
*/
	}

	private Record key(Object... values) {
		RecordBuilder rb = new RecordBuilder();
		for (Object x : values)
			rb.add(x);
		return rb.build();
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
			int adr = Btree.getAddress(key);
			Record r = new Record(stor.buffer(adr));
			return new Column(r);
		}
	}

	private class IndexesReader {
		Btree.Iter iter;
		boolean first = true;
		Record cur;
		ArrayList<Record> list = Lists.newArrayList();

		IndexesReader(Btree indexesIndex) {
			iter = indexesIndex.iterator();
		}
		Indexes next(Columns cols) {
			if (first) {
				first = false;
				iter.next();
				cur = index(iter.cur());
			}
			if (cur == null)
				return null;
			while (true) {
				list.add(cur);
				iter.next();
				if (iter.eof())
					break;
				Record prev = cur;
				cur = index(iter.cur());
				if (Index.tblnum(prev) != Index.tblnum(cur)) {
					Indexes result = result(cols);
					list = Lists.newArrayList();
					return result;
				}
			}
			cur = null;
			return result(cols);
		}
		Record index(Record key) {
			int adr = Btree.getAddress(key);
			return new Record(stor.buffer(adr));
		}
		Indexes result(Columns cols) {
			ImmutableList.Builder<Index> builder = ImmutableList.builder();
			for (Record r : list)
				builder.add(new Index(cols, r));
			return new Indexes(builder.build());
		}
	}

}
