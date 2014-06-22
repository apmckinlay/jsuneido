/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.util.Util.concat;
import static suneido.util.Util.difference;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.nil;
import static suneido.util.Util.setDifference;
import static suneido.util.Util.setUnion;
import static suneido.util.Util.startsWith;
import static suneido.util.Util.startsWithSet;
import static suneido.util.Util.union;
import static suneido.util.Util.without;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suneido.InternalError;
import suneido.SuContainer;
import suneido.SuException;
import suneido.intfc.database.Record;
import suneido.language.Ops;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class Summarize extends Query1 {
	final List<String> by;
	private final List<String> cols;
	final List<String> funcs;
	final List<String> on;
	private enum Strategy { NONE, COPY, SEQUENTIAL, MAP };
	private Strategy strategy = Strategy.NONE;
	List<String> via;
	private boolean first = true;
	private boolean rewound = true;
	private Header hdr;
	private SummarizeStrategy strategyImp;

	Summarize(Query source, List<String> by, List<String> cols,
			List<String> funcs, List<String> on) {
		super(source);
		this.by = by;
		this.cols = cols;
		this.funcs = funcs;
		this.on = on;
		if (!source.columns().containsAll(by))
			throw new SuException("summarize: nonexistent columns: "
					+ difference(by, source.columns()));

		if (by.isEmpty() || by_contains_key())
			strategy = Strategy.COPY;

		for (int i = 0; i < cols.size(); ++i)
			if (cols.get(i) == null)
				cols.set(i, on.get(i) == null ? "count"
						: funcs.get(i) + "_" + on.get(i));
	}

	boolean by_contains_key() {
		// check if project contain candidate key
		for (List<String> k : source.keys())
			if (by.containsAll(k))
				return true;
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(source.toString());
		sb.append(" SUMMARIZE");
		switch (strategy) {
		case NONE: break;
		case COPY: sb.append("-COPY"); break;
		case SEQUENTIAL: sb.append("-SEQ"); break;
		case MAP: sb.append("-MAP"); break;
		default: throw InternalError.unreachable();
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

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		Set<String> srcneeds =
				setUnion(without(on, null), setDifference(needs, cols));
		if (strategy == Strategy.COPY)
			return optimizeCopy(index, is_cursor, freeze, srcneeds);
		else
			return optimizeNonCopy(index, is_cursor, freeze, srcneeds);
	}

	private double optimizeCopy(List<String> index, boolean is_cursor,
			boolean freeze, Set<String> srcneeds) {
		if (freeze)
			via = index;
		return source.optimize(index, srcneeds, ImmutableSet.copyOf(by),
				is_cursor, freeze);
	}

	// NOTE: using optimize1 to bypass tempindex
	private double optimizeNonCopy(List<String> index, boolean is_cursor,
			boolean freeze, Set<String> srcneeds) {
		Best best = best_prefixed(sourceIndexes(index), by, srcneeds, is_cursor);

		double mapCost = startsWith(by, index)
				? 1.5 * source.optimize1(noFields, srcneeds, noNeeds, is_cursor, false)
				: IMPOSSIBLE;

		if (! freeze)
			return Math.min(best.cost, mapCost);

		if (mapCost < best.cost) {
			strategy = Strategy.MAP;
			return source.optimize1(noFields, srcneeds, noNeeds, is_cursor, freeze);
		} else {
			if (best.cost >= IMPOSSIBLE)
				return IMPOSSIBLE;
			strategy = Strategy.SEQUENTIAL;
			via = best.index;
			return source.optimize1(best.index, srcneeds, noNeeds, is_cursor, freeze);
		}
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

	@Override
	List<String> columns() {
		return union(by, cols);
	}

	@Override
	List<List<String>> indexes() {
		if (strategy == Strategy.COPY)
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
		return source.nrecords() / 2;
	}

	@Override
	int recordsize() {
		return by.size() * source.columnsize() + cols.size() * 8;
	}

	@Override
	public Header header() {
		if (first)
			iterate_setup();
		List<String> flds = concat(by, cols);
		return new Header(asList(noFields, flds), flds);
	}

	@Override
	boolean singleDbTable() {
		return false;
	}

	private void iterate_setup() {
		first = false;
		hdr = source.header();
		strategyImp = strategy == Strategy.MAP
			? new SummarizeStrategyMap(this)
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
		abstract void add(Object x);
		abstract Object result();

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
			throw InternalError.unreachable();
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

	private static class Max extends Summary {
		Object value;

		@Override
		void init() {
			value = null;
		}

		@Override
		void add(Object x) {
			if (value == null || Ops.cmp(x, value) > 0)
				value = x;
		}

		@Override
		Object result() {
			return value;
		}
	}

	private static class Min extends Summary {
		@Override
		void init() {
			value = null;
		}

		@Override
		void add(Object x) {
			if (value == null || Ops.cmp(x, value) < 0)
				value = x;
		}

		@Override
		Object result() {
			return value;
		}

		Object value;
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
			return list;
		}
	}

}
