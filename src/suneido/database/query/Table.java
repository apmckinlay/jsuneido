package suneido.database.query;

import static suneido.SuException.unreachable;
import static suneido.Suneido.verify;
import static suneido.Util.*;
import static suneido.database.Database.theDB;

import java.util.List;

import suneido.SuException;
import suneido.database.*;

public class Table extends Query {
	private final String table;
	/* package */suneido.database.Table tbl;
	private boolean first = true;
	private boolean rewound = true;
	private final Keyrange sel = new Keyrange();
	private Header hdr;
	private BtreeIndex ix;
	private Transaction tran = null;
	private boolean singleton; // i.e. key()
	private List<String> idx;
	/* package */BtreeIndex.Iter iter;

	public Table(String tablename) {
		table = tablename;
		tbl = theDB.ck_getTable(table);
		singleton = indexes().get(0).isEmpty();
	}

	@Override
	public String toString() {
		return table + (idx == null ? "" : "^" + listToParens(idx));
	}

	@Override
		double optimize2(List<String> index, List<String> needs,
				List<String> firstneeds, boolean is_cursor, boolean freeze) {
			tbl = theDB.ck_getTable(table);
			singleton = tbl.singleton();

			if (!columns().containsAll(needs))
				throw new SuException("Table::optimize columns does not contain: "
						+ difference(needs, columns()));
			if (!columns().containsAll(index))
				return IMPOSSIBLE;

			List<List<String>> idxs = indexes();
			if (nil(idxs))
				return IMPOSSIBLE;
			if (singleton) {
				idx = nil(index) ? idxs.get(0) : index;
				return recordsize();
			}
			double cost1 = IMPOSSIBLE;
			double cost2 = IMPOSSIBLE;
			double cost3 = IMPOSSIBLE;
			List<String> idx1, idx2 = null, idx3 = null;

			if (!nil(idx1 = match(idxs, index, needs)))
				// index found that meets all needs
				cost1 = nrecords() * keysize(idx1); // cost of reading index
			if (!nil(firstneeds) && !nil(idx2 = match(idxs, index, firstneeds)))
				// index found that meets firstneeds
				// assume this means we only have to read 75% of data
				cost2 = .75 * nrecords() * recordsize() + // cost of reading data
				nrecords() * keysize(idx2); // cost of reading index
			if (!nil(needs) && !nil(idx3 = match(idxs, index, noFields)))
				cost3 = nrecords() * recordsize() + // cost of reading data
				nrecords() * keysize(idx3); // cost of reading index
	//System.out.println(idx1 + " = " + cost1 + ", " + idx2 + " = " + cost2 + ", "
	//	+ idx3 + " = " + cost3);

			double cost;
			if (cost1 <= cost2 && cost1 <= cost3) {
				cost = cost1;
				idx = idx1;
			} else if (cost2 <= cost1 && cost2 <= cost3) {
				cost = cost2;
				idx = idx2;
			} else {
				cost = cost3;
				idx = idx3;
			}

			return cost;
		}

	@Override
	List<String> columns() {
		return tbl.getColumns();
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
		return tbl.nrecords() == 0 ? 0 : tbl.totalsize / tbl.nrecords();
	}

	@Override
	double nrecords() {
		return tbl.nrecords();
	}

	private static List<String> match(List<List<String>> idxs,
			List<String> index, List<String> needs) {
		List<String> best = null;
		int bestremainder = 9999;
		for (List<String> idx : idxs) {
			int i;
			for (i = 0; i < idx.size() && i < index.size(); ++i)
				if (!idx.get(i).equals(index.get(i)))
					break;
			if (i < index.size() || !idx.containsAll(needs))
				continue;
			int remainder = idx.size();
			if (remainder < bestremainder) {
				best = idx;
				bestremainder = remainder;
			}
		}
		return best;
	}

	int keysize(List<String> index) {
		int nrecs = tbl.nrecords();
		if (nrecs == 0)
			return 0;
		Index idx = tbl.getIndex(listToCommas(index));
		verify(idx != null);
		int nnodes = idx.nnodes();
		int nodesize = Btree.NODESIZE / (nnodes <= 1 ? 4 : 2);
		return (nnodes * nodesize) / nrecs + index.size();
		// add index.size() to favor shorter indexes
	}

	public int indexsize(List<String> index) {
		Index idx = tbl.getIndex(listToCommas(index));
		return idx.nnodes() == 1 ? index.size() * 100 : idx.nnodes()
				* Btree.NODESIZE;
	}

	/* package */void select_index(List<String> index) {
		// used by Select::optimize
		idx = index;
	}

	@Override
	void setTransaction(Transaction tran) {
		this.tran = tran;
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
		Row row = new Row(iter.cur().key, theDB.input(iter.keyadr()));

		if (singleton && !sel.contains(row.project(hdr, idx))) {
			rewound = true;
			return null;
		}

		return row;
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
		return new Header(list(index, tbl.getFields()), tbl.getColumns());
	}

	@Override
	void rewind() {
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		verify(prefix(idx, index));
		sel.set(from, to);
		rewound = true;
	}

	void set_index(List<String> index) {
		idx = index;
		ix = (nil(idx) || singleton
				? tbl.firstIndex()
				: tbl.getIndex(listToCommas(idx))).btreeIndex;
		verify(ix != null);
		rewound = true;
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
