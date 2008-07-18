package suneido.database.query;

import static suneido.SuException.unreachable;
import static suneido.Util.*;
import static suneido.database.Database.theDB;

import java.util.List;

import suneido.SuValue;
import suneido.database.*;

public class Table extends Query {
	private final String table;
	private suneido.database.Table tbl;
	private int choice;
	private boolean first = true;
	private boolean rewound = true;
	private Keyrange sel;
	private Header hdr;
	private BtreeIndex ix;
	private Transaction tran = null;
	private final SuValue trigger = null;
	private List<Integer> flds;
	private boolean singleton; // i.e. key()
	private List<String> idx;
	private BtreeIndex.Iter iter;

	public Table(String tablename) {
		table = tablename;
		tbl = theDB.ck_getTable(table);
		singleton = indexes().get(0).isEmpty();
	}

	@Override
	public String toString() {
		return table + (idx == null ? "" : "^" + idx);
	}

	@Override
	List<String> columns() {
		return tbl.get_columns();
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
		return recordsize() / columns().size();
	}

	@Override
	int recordsize() {
		return tbl.nrecords == 0 ? 0 : tbl.totalsize / tbl.nrecords;
	}

	@Override
	double nrecords() {
		return tbl.nrecords;
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
		Record r = theDB.input(iter.keyadr());

		// TODO

		return new Row(iter.cur().key, r);
	}

	private void iterate_setup(Dir dir) {
		hdr = header();
		ix = (idx == null || singleton
			? tbl.firstIndex()
			: tbl.getIndex(listToCommas(idx))).btreeIndex;
	}

	@Override
	Header header() {
		Index i = nil(idx) || singleton ? null
				: theDB.getIndex(tbl, listToCommas(idx));
		boolean lower = i != null && i.isLower();
		List<String> index = singleton || lower ? noFields : idx;
		return new Header(list(index, tbl.get_fields()), tbl.get_columns());
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
	}

	@Override
	boolean updateable() {
		return true;
	}

	@Override
	void output(Record r) {
		theDB.addRecord(tran, table, r);
	}

}
