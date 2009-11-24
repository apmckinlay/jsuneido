package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.util.Util.*;

import java.util.*;

import suneido.SuContainer;
import suneido.SuException;
import suneido.database.Record;
import suneido.language.Ops;

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
	Header hdr;
	private SummarizeStrategy strategyImp;

	Summarize(Query source, List<String> by, List<String> cols,
			List<String> funcs, List<String> on) {
		super(source);
		this.by = by;
		this.cols = cols;
		this.funcs = funcs;
		this.on = on;
		if (!source.columns().containsAll(by))
			throw new SuException(
					"summarize: nonexistent columns: "
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
		default: throw SuException.unreachable();
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
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		List<String> srcneeds = union(without(on, null), difference(needs, cols));

		if (strategy == Strategy.COPY) {
			if (freeze)
				via = index;
			return source.optimize(index, srcneeds, by, is_cursor, freeze);
		}

		List<List<String>> indexes;
		if (nil(index))
			indexes = source.indexes();
		else {
			List<Fixed> fixed = source.fixed();
			indexes = new ArrayList<List<String>>();
			for (List<String> idx : source.indexes())
				if (prefixed(idx, index, fixed))
					indexes.add(idx);
		}
		Best best = best_prefixed(indexes, by, srcneeds, is_cursor, new Best());
		if (nil(best.index) && startsWith(by, index)) {
			// accumulate results in memory
			// doesn't require any order, can only supply in order of "by"
			strategy = Strategy.MAP;
			return source.optimize(noFields, srcneeds, by, is_cursor, freeze);
		}
		if (nil(best.index))
			return IMPOSSIBLE;
		strategy = Strategy.SEQUENTIAL;
		if (!freeze)
			return best.cost;
		via = best.index;
		return source.optimize(best.index, srcneeds, noFields, is_cursor,
				freeze);
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
			List<List<String>> idxs = new ArrayList<List<String>>();
			for (List<String> src : source.indexes())
				if (startsWithSet(src, by))
					idxs.add(src);
			return idxs;
		}
	}

	@Override
	public List<List<String>> keys() {
		List<List<String>> keys = new ArrayList<List<String>>();
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

	@SuppressWarnings("unchecked")
	@Override
	public Header header() {
		if (first)
			iterate_setup();
		List<String> flds = concat(by, cols);
		return new Header(asList(noFields, flds), flds);
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
			throw SuException.unreachable();
		}
	}
	private static class Count extends Summary {
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
		int n;
	}
	private static class Total extends Summary {
		@Override
		void init() {
			total = 0;
		}

		@Override
		void add(Object x) {
			total = Ops.add(total, x);
		}

		@Override
		Object result() {
			return total;
		}

		Object total;
	}

	private static class Average extends Summary {
		@Override
		void init() {
			n = 0;
			total = 0;
		}

		@Override
		void add(Object x) {
			++n;
			total = Ops.add(total, x);
		}

		@Override
		Object result() {
			return Ops.div(total, n);
		}

		int n = 0;
		Object total;
	}

	private static class Max extends Summary {
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

		Object value;
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
		@Override
		void init() {
			set = new HashSet<Object>();
		}

		@Override
		void add(Object x) {
			set.add(x);
		}

		@Override
		Object result() {
			SuContainer list = new SuContainer();
			for (Object x : set)
				list.append(x);
			return list;
		}

		HashSet<Object> set;
	}

}
