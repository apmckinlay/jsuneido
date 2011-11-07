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

import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

public class Table extends Query {
	private final String table;
	final suneido.intfc.database.Table tbl;
	private boolean first = true;
	private boolean rewound = true;
	private final Keyrange sel = new Keyrange();
	private Header hdr;
	private String icols; // null to use first index
	private Transaction tran;
	private boolean singleton; // i.e. key()
	private List<String> idx = noFields;
	IndexIter iter;

	public Table(Transaction tran, String tablename) {
		this.tran = tran;
		table = tablename;
		tbl = tran.ck_getTable(table);
		singleton = indexes().get(0).isEmpty();
	}

	@Override
	public String toString() {
		return table + (nil(idx) ? "" : "^" + listToParens(idx));
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		singleton = tbl.singleton();

		assert columns().containsAll(needs);
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
			cost1 = nrecords() * keySize(idx1); // cost of reading index
		if (!nil(firstneeds) && !nil(idx2 = match(idxs, index, firstneeds)))
			// index found that meets firstneeds
			// assume this means we only have to read 75% of data
			cost2 = .75 * nrecords() * recordsize() + // cost of reading data
			nrecords() * keySize(idx2); // cost of reading index
		if (!nil(needs) && !nil(idx3 = match(idxs, index, noFields)))
			cost3 = nrecords() * recordsize() + // cost of reading data
			nrecords() * keySize(idx3); // cost of reading index
		//System.out.println("Table have " + indexes() + " want " + index);
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
		return nrecs == 0 ? 0 : (int) (totalSize() / nrecs);
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

	private static List<String> match(List<List<String>> idxs,
			List<String> index, Collection<String> needs) {
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

		Row row = new Row(iter.curKey(), tran.input(iter.keyadr()));

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

	@SuppressWarnings("unchecked")
	@Override
	public Header header() {
		List<String> index = singleton  ? noFields : idx;
		return new Header(asList(index, tbl.getFields()), tbl.getColumns());
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

}
