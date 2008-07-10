package suneido.database.query;

import static suneido.Util.listToCommas;
import static suneido.database.Database.theDB;

import java.util.List;

import suneido.SuValue;
import suneido.database.BtreeIndex;
import suneido.database.Record;
import suneido.database.Table;
import suneido.database.Transaction;

public class QueryTable extends Query {
	private final String table;
	private Table tbl;
	private int choice;
	private boolean first = true;
	private boolean rewound = true;
	private Keyrange sel;
	private Header hdr;
	private BtreeIndex ix;
	private Transaction tran;
	private SuValue trigger;
	private List<Integer> flds;
	private boolean singleton; // i.e. key()
	private List<String> idx;
	private BtreeIndex.Iter iter;


	public QueryTable(String tablename) {
		table = tablename;
	}

	@Override
	public String toString() {
		return table + (idx == null ? "" : "^" + idx);
	}

	@Override
	void close() {
	}

	@Override
	List<String> columns() {
		return tbl.columnNames();
	}

	@Override
	List<List<String>> indexes() {
		return tbl.indexesColumns();
	}

	@Override
	List<List<String>> keys() {
		return tbl.keysColumns();
	}

	@Override
	int columnsize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	Row get(Dir dir) {
		if (first) {
			first = false;
			iterate_setup(dir);
		}
		if (rewound) {
			rewound = false;
			iter = singleton || sel == null
				? ix.iter(tran)
				: ix.iter(tran, sel.org, sel.end);
		}
		switch (dir) {
		case NEXT :
			iter.next();
		case PREV :
			iter.prev();
		}
		if (iter.eof()) {
			rewound = true;
			return null;
		}
		Record r = theDB.input(iter.keyadr());

		// TODO

		return new Row(r);
	}

	private void iterate_setup(Dir dir) {
		hdr = header();
		ix = (idx == null || singleton
			? tbl.firstIndex()
			: tbl.getIndex(listToCommas(idx))).btreeIndex;
	}

	@Override
	Header header() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<String> key_index(List<String> needs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	double nrecords() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	int recordsize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		// TODO optimize2
		tbl = theDB.ck_getTable(table);
		singleton = tbl.singleton();
		idx = indexes().get(0);
		return 10;
	}

	@Override
	void rewind() {
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// TODO Auto-generated method stub

	}

	@Override
	void setTransaction(Transaction tran) {
		this.tran = tran;
		// iter.setTransaction(tran);
	}

}
