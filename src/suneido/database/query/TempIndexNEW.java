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
import suneido.util.FractalTree;
import suneido.util.Util;

public class TempIndexNEW extends Query1 {
	private final List<String> order;
	private final boolean unique;
	private Transaction tran;
	private boolean first = true;
	private boolean rewound = true;
	private Object[][] keys;
	private int cur;
	private final Keyrange sel = new Keyrange();

	public TempIndexNEW(Query source, Transaction tran, List<String> order, boolean unique) {
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
				cur = Util.lowerBound(keys, new Object[] { sel.org }, cmp);
			else
				cur = Util.upperBound(keys, new Object[] { sel.end }, cmp) - 1;
		}
		else if (dir == Dir.NEXT)
			++cur;
		else // dir == PREV
			--cur;
		if (cur < 0 || keys.length <= cur || ! sel.contains(getKey(cur))) {
			rewound = true;
			return null;
		}

		// TODO put iter->key into row
		return Row.fromRefsSkip(tran, keys[cur]);
	}
	
	Record getKey(int i) {
		return (Record) keys[i][0];
	}

	private void iterate_setup(Dir dir) {
		Header srchdr = source.header();
		FractalTree<Object[]> keys = new FractalTree<Object[]>(cmp);
		Row row;
		while (null != (row = source.get(Dir.NEXT))) {
			Record key = row.project(srchdr, order);
			if (key.bufSize() > 4000)
				throw new SuException("index entry size > 4000: " + order);
			keys.add(row.getRefs(key));
		}
		this.keys = keys.toArray(new Object[0][]);
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
