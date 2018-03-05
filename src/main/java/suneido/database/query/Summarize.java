/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.util.Util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuInternalError;
import suneido.database.immudb.Record;
import suneido.runtime.Ops;

public class Summarize extends Query1 {
	final List<String> by;
	private final List<String> cols;
	final List<String> funcs;
	final List<String> on;
	private enum Strategy { NONE, SEQ, MAP, IDX }

	private Strategy strategy = Strategy.NONE;
	List<String> via;
	private boolean first = true;
	private boolean rewound = true;
	private Header hdr;
	private SummarizeStrategy strategyImp;
	final boolean wholeRecord;

	/**
	 * cols, funcs, and on are parallel arrays storing multiple col = func [on]
	 */
	Summarize(Query source, List<String> by,
			List<String> cols, List<String> funcs, List<String> on) {
		super(source);
		this.by = by;
		this.cols = cols;
		this.funcs = funcs;
		this.on = on;
		if (!source.columns().containsAll(by))
			throw new SuException("summarize: nonexistent columns: "
					+ difference(by, source.columns()));

		for (int i = 0; i < cols.size(); ++i)
			if (cols.get(i) == null)
				cols.set(i, on.get(i) == null ? "count"
						: funcs.get(i) + "_" + on.get(i));

		wholeRecord = minmax1() && source.keys().contains(on);
	}

	private boolean minmax1() {
		if (! by.isEmpty() || funcs.size() != 1)
			return false;
		String fn = funcs.get(0).toLowerCase();
		return fn.equals("min") || fn.equals("max");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(source.toString());
		sb.append(" SUMMARIZE");
		switch (strategy) {
		case NONE: break;
		case SEQ: sb.append("-SEQ"); break;
		case MAP: sb.append("-MAP"); break;
		case IDX: sb.append("-IDX"); break;
		default: throw SuInternalError.unreachable();
		}
		sb.append(" ");
		if (!nil(via))
			sb.append("^").append(listToParens(via)).append(" ");
		if (! by.isEmpty())
			sb.append(listToParens(by)).append(" ");
		for (int i = 0; i < cols.size(); ++i) {
			if (cols.get(i) != null)
				sb.append(cols.get(i)).append(" = ");
			sb.append(funcs.get(i));
			if (on.get(i) != null)
				sb.append(" ").append(on.get(i));
			sb.append(", ");
		}
		return sb.substring(0, sb.length() - 2);
	}

	// optimize ----------------------------------------------------------------

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		Set<String> srcneeds =
				setUnion(without(on, null), setDifference(needs, cols));

		double seqCost = seqCost(index, srcneeds, is_cursor, false);
		double idxCost = idxCost(is_cursor, false);
		double mapCost = mapCost(index, srcneeds, is_cursor, false);

		if (!freeze)
			return Math.min(seqCost, Math.min(idxCost, mapCost));

		if (seqCost <= idxCost && seqCost <= mapCost)
			return seqCost(index, srcneeds, is_cursor, true);
		else if (idxCost <= mapCost)
			return idxCost(is_cursor, true);
		else
			return mapCost(index, srcneeds, is_cursor, true);
	}

	private double seqCost(List<String> index, Set<String> srcneeds,
			boolean is_cursor, boolean freeze) {
		if (freeze)
			strategy = Strategy.SEQ;
		if (by.isEmpty() || by_contains_key()) {
			via = by.isEmpty() ? noFields : index;
			return source.optimize(via,
					srcneeds, ImmutableSet.copyOf(by), is_cursor, freeze);
		} else {
			Best best = best_prefixed(sourceIndexes(index), by, srcneeds, is_cursor);
			if (! freeze || best.cost >= IMPOSSIBLE)
				return best.cost;
			via = best.index;
			return source.optimize1(best.index, srcneeds, noNeeds, is_cursor, freeze);
		}
	}
	private boolean by_contains_key() {
		// check if project contain candidate key
		for (List<String> k : source.keys())
			if (by.containsAll(k))
				return true;
		return false;
	}
	/** @return the indexes that satisfy the required index */
	private List<List<String>> sourceIndexes(List<String> index) {
		if (nil(index))
			return source.indexes();
		else {
			List<Fixed> fixed = source.fixed();
			List<List<String>> indexes = Lists.newArrayList();
			for (List<String> idx : source.indexes())
				if (prefixed(idx, index, fixed))
					indexes.add(idx);
			return indexes;
		}
	}

	private double idxCost(boolean is_cursor, boolean freeze) {
		if (! minmax1())
			return IMPOSSIBLE;
		// using optimize1 to bypass tempindex
		// dividing by nrecords since we're only reading one record
		double nr = Math.max(1, source.nrecords());
		double cost = source.optimize1(on, noNeeds, noNeeds, is_cursor, freeze) / nr;
		if (freeze) {
			strategy = Strategy.IDX;
			via = on;
		}
		return cost;
	}

	private double mapCost(List<String> index, Set<String> srcneeds,
			boolean is_cursor,	boolean freeze) {
		// can only provide 'by' as index
		if (! startsWith(by, index))
			return IMPOSSIBLE;
		// using optimize1 to bypass tempindex
		// add 50% for map overhead
		double cost = 1.5 *
				source.optimize1(noFields, srcneeds, noNeeds, is_cursor, freeze);
		if (freeze)
			strategy = Strategy.MAP;
		return cost;
	}

	// end of optimize ---------------------------------------------------------

	@Override
	List<String> columns() {
		return wholeRecord
				? union(cols, source.columns())
				: union(by, cols);
	}

	@Override
	List<List<String>> indexes() {
		if (by.isEmpty() || by_contains_key())
			return source.indexes();
		else {
			List<List<String>> idxs = new ArrayList<>();
			for (List<String> src : source.indexes())
				if (startsWithSet(src, by))
					idxs.add(src);
			return idxs;
		}
	}

	@Override
	public List<List<String>> keys() {
		List<List<String>> keys = new ArrayList<>();
		for (List<String> k : source.keys())
			if (by.containsAll(k))
				keys.add(k);
		if (nil(keys))
			keys.add(by);
		return keys;
	}

	@Override
	double nrecords() {
		double nr = source.nrecords();
		return nr == 0 ? 0
				: by.isEmpty() ? 1
				: by_contains_key() ? nr
				: nr / 2;					//TODO review this estimate
	}

	@Override
	int recordsize() {
		return by.size() * source.columnsize() + cols.size() * 8;
	}

	@Override
	public Header header() {
		if (wholeRecord) {
			return new Header(source.header(),
					new Header(asList(noFields, cols), cols));
		} else {
			List<String> flds = concat(by, cols);
			return new Header(asList(noFields, flds), flds);
		}
	}

	@Override
	boolean singleDbTable() {
		return false;
	}

	private void iterate_setup() {
		first = false;
		hdr = source.header();
		strategyImp =
				(strategy == Strategy.MAP) ? new SummarizeStrategyMap(this)
				: (strategy == Strategy.IDX) ? new SummarizeStrategyIdx(this)
				: new SummarizeStrategySeq(this);
	}

	@Override
	public Row get(Dir dir) {
		if (first)
			iterate_setup();
		boolean wasRewound = rewound;
		rewound = false;
		return strategyImp.get(dir, wasRewound);
	}

	@Override
	public void rewind() {
		source.rewind();
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		if (first)
			iterate_setup();
		strategyImp.select(index, from, to);
		strategyImp.sel.org = from;
		strategyImp.sel.end = to;
		rewound = true;
	}

	@Override
	public boolean updateable() {
		return false; // override Query1 source->updateable
	}

	Header getHdr() {
		return hdr;
	}

	public abstract static class Summary {
		abstract void init();
		void add(Object x) {
			add(null, x);
		}
		void add(Row row, Object x) {
			add(x);
		}
		abstract Object result();
		Row getRow() {
			return null;
		}

		static Summary valueOf(String summary) {
			summary = summary.toLowerCase();
			if ("count".equals(summary))
				return new Count();
			if ("total".equals(summary))
				return new Total();
			if ("average".equals(summary))
				return new Average();
			if ("max".equals(summary))
				return new Max();
			if ("min".equals(summary))
				return new Min();
			if ("list".equals(summary))
				return new ListSum();
			throw SuInternalError.unreachable();
		}
	}
	private static class Count extends Summary {
		int n;

		@Override
		void init() {
			n = 0;
		}
		@Override
		void add(Object x) {
			++n;
		}
		@Override
		Object result() {
			return n;
		}
	}
	private static class Total extends Summary {
		Object total;

		@Override
		void init() {
			total = 0;
		}

		@Override
		void add(Object x) {
			try {
				total = Ops.add(total, x);
			} catch (Exception e) {
			}
		}

		@Override
		Object result() {
			return total;
		}
	}

	private static class Average extends Summary {
		int n = 0;
		Object total;

		@Override
		void init() {
			n = 0;
			total = 0;
		}

		@Override
		void add(Object x) {
			++n;
			try {
				total = Ops.add(total, x);
			} catch (Exception e) {
			}
		}

		@Override
		Object result() {
			return Ops.div(total, n);
		}
	}

	private static abstract class MinMax extends Summary {
		Object value;
		Row row;

		@Override
		void init() {
			value = null;
		}

		@Override
		Object result() {
			return value;
		}

		@Override
		Row getRow() {
			return row;
		}
	}

	private static class Max extends MinMax {
		@Override
		void add(Row row, Object x) {
			if (value == null || Ops.cmp(x, value) > 0) {
				this.row = row;
				value = x;
			}
		}
	}

	private static class Min extends MinMax {
		@Override
		void add(Row row, Object x) {
			if (value == null || Ops.cmp(x, value) < 0) {
				this.row = row;
				value = x;
			}
		}
	}

	private static class ListSum extends Summary {
		HashSet<Object> set;

		@Override
		void init() {
			set = new HashSet<>();
		}

		@Override
		void add(Object x) {
			set.add(x);
		}

		@Override
		Object result() {
			SuContainer list = new SuContainer();
			for (Object x : set)
				list.add(x);
			if (list.size() <= 3) // to ensure consistent order for tests
				list.sort(Boolean.FALSE);
			return list;
		}
	}

}
