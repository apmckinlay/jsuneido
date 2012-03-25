package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.startsWith;
import static suneido.util.Verify.verify;

import java.util.Comparator;
import java.util.List;

import suneido.SuException;
import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;
import suneido.util.MergeTree;

public class TempIndex extends Query1 {
	private final List<String> order;
	private final boolean unique;
	private Transaction tran;
	private boolean first = true;
	private boolean rewound = true;
	private final MergeTree<Object[]> keys = new MergeTree<Object[]>(cmp);
	private MergeTree<Object[]>.Iter iter;
	private final Keyrange sel = new Keyrange();

	public TempIndex(Query source, Transaction tran, List<String> order, boolean unique) {
		super(source);
		this.tran = tran;
		this.order = order;
		this.unique = unique;
	}

	@Override
	public void setTransaction(Transaction tran) {
		this.tran = tran;
		super.setTransaction(tran);
	}

	@Override
	public String toString() {
		return source.toString() + " TEMPINDEX" + listToParens(order)
				+ (unique ? " unique" : "");
	}

	@SuppressWarnings("unchecked")
	@Override
	List<List<String>> indexes() {
		return asList(order);
	}

	@Override
	public Row get(Dir dir) {
		if (first) {
			first = false;
			iterate_setup(dir);
		}
		if (rewound) {
			rewound = false;
			if (dir == Dir.NEXT)
				iter.seekFirst(new Object[] { sel.org });
			else // prev
				iter.seekLast(new Object[] { sel.end });
		}
		Object[] cur = (dir == Dir.NEXT) ? iter.next() : iter.prev();
		if (cur == null || ! sel.contains((Record) cur[0])) {
			rewound = true;
			return null;
		}

		// TODO put iter->key into row
		return Row.fromRefsSkip(tran, cur);
	}

	// TODO pack keys into something (ChunkedStorage?)
	// to reduce per-object overhead
	// maybe pack refs along with keys so MergeTree just has offsets
	private void iterate_setup(Dir dir) {
		keys.clear();
		Header srchdr = source.header();
		Row row;
		while (null != (row = source.get(Dir.NEXT))) {
			Record key = row.project(srchdr, order);
			if (key.bufSize() > 4000)
				throw new SuException("index entry size > 4000: " + order);
			keys.add(row.getRefs(key));
		}
		iter = keys.iter();
	}

	private static final Comparator<Object[]> cmp = new Comparator<Object[]>() {
		@Override
		public int compare(Object[] x, Object[] y) {
			Record xkey = (Record) x[0];
			Record ykey = (Record) y[0];
			return xkey.compareTo(ykey);
		}
	};

	@Override
	public void rewind() {
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		verify(startsWith(order, index));
		sel.set(from, to);
		rewound = true;
	}

}
