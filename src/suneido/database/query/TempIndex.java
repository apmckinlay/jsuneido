/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.startsWith;
import static suneido.util.Verify.verify;

import java.util.List;

import suneido.SuException;
import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.Record;
import suneido.database.immudb.RecordStore;
import suneido.database.immudb.Transaction;
import suneido.util.ArraysList;
import suneido.util.BlockList;
import suneido.util.IntComparator;

public class TempIndex extends Query1 {
	private final List<String> order;
	private final boolean unique;
	private Transaction tran;
	private boolean first = true;
	private boolean rewound = true;
	private RecordStore stor;
	private final ArraysList<Object> refs = new ArraysList<>();
	private final IntComparator cmp = (int xi, int yi) -> {
		Record xkey = stor.get(xi);
		Record ykey = stor.get(yi);
		return xkey.compareTo(ykey);
	};
	private final BlockList index = new BlockList(cmp);
	private BlockList.Iter iter;
	private final Keyrange sel = new Keyrange();
	private final boolean single;

	public TempIndex(Query source, Transaction tran, List<String> order, boolean unique) {
		super(source);
		this.tran = tran;
		this.order = order;
		this.unique = unique;
		single = source.singleDbTable();
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

	@Override
	List<List<String>> indexes() {
		return asList(order);
	}

	@Override
	public Row get(Dir dir) {
		if (first) {
			iterate_setup(dir);
			first = false;
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
				? new Row(null, tran.input(adr))
				: Row.fromRefs(tran, refs, adr);
	}

	private void iterate_setup(Dir dir) {
		stor = Dbpkg.recordStore();
		Header srchdr = source.header();
		Row row;
		while (null != (row = source.get(Dir.NEXT))) {
			int adr = single ? row.firstData().address() : row.getRefs(refs);
			assert ! single || adr != 0;
			Record key = row.project(srchdr, order, adr);
			if (key.bufSize() > 4000)
				throw new SuException("temp index entry size > 4000: " + order);
			index.add(stor.add(key));
		}
		index.sort();
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
