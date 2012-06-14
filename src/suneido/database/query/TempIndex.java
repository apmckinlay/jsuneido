package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.startsWith;
import static suneido.util.Verify.verify;

import java.util.List;

import suneido.SuException;
import suneido.Suneido;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordStore;
import suneido.intfc.database.Transaction;
import suneido.util.ArraysList;
import suneido.util.IntComparator;
import suneido.util.IntMergeTree;

public class TempIndex extends Query1 {
	private final List<String> order;
	private final boolean unique;
	private Transaction tran;
	private boolean first = true;
	private boolean rewound = true;
	private RecordStore stor;
	private final ArraysList<Object> refs = new ArraysList<Object>();
	private final IntComparator cmp = new IntComparator() {
		@Override
		public int compare(int xi, int yi) {
			Record xkey = stor.get(xi);
			Record ykey = stor.get(yi);
			return xkey.compareTo(ykey);
		}
	};
	private final IntMergeTree index = new IntMergeTree(cmp);
	private IntMergeTree.Iter iter;
	private final Keyrange sel = new Keyrange();
	private boolean single;

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
				iter.seekFirst(stor.add(sel.org));
			else // prev
				iter.seekLast(stor.add(sel.end));
		}
		Record key;
		int cur = (dir == Dir.NEXT) ? iter.next() : iter.prev();
		if (cur == Integer.MIN_VALUE || cur == Integer.MAX_VALUE ||
				! sel.contains(key = stor.get(cur))) {
			rewound = true;
			return null;
		}
		int adr = key.getInt(key.size() - 1);
		return single
				? new Row(new Record[] { null, tran.input(adr) })
				: Row.fromRefs(tran, refs, adr);
	}

	private void iterate_setup(Dir dir) {
		stor = Suneido.dbpkg.recordStore();
		index.clear();
		refs.clear();
		Header srchdr = source.header();
		single = (srchdr.size() <= 2);
		Row row;
		while (null != (row = source.get(Dir.NEXT))) {
			int adr = single ? row.firstData().address() : row.getRefs(refs);
			Record key = row.project(srchdr, order, adr);
			if (key.bufSize() > 4000)
				throw new SuException("temp index entry size > 4000: " + order);
			index.add(stor.add(key));
		}
		iter = index.iter();
	}

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
