package suneido.database.query;

import static suneido.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.database.Record;

public class Summarize extends Query1 {
	private final List<String> by;
	private final List<String> cols;
	private final List<String> funcs;
	private final List<String> on;
	private List<String> via;
	Strategy strategy = Strategy.NONE;
	enum Strategy {
		NONE(""), COPY("-COPY"), SEQUENTIAL("-SEQ");
		public String name;
		Strategy(String name) {
			this.name = name;
		}
	};

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
			if (cols.get(i) == null && on.get(i) != null)
				cols.set(i, funcs.get(i) + "_" + on.get(i));
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
		String s = source + " SUMMARIZE" + strategy.name + " ";
		if (via != null)
			s += "^" + listToParens(via) + " ";
		if (! by.isEmpty())
			s += listToParens(by) + " ";
		for (int i = 0; i < cols.size(); ++i) {
			if (cols.get(i) != null)
				s += cols.get(i) + " = ";
			s += funcs.get(i);
			if (on.get(i) != null)
				s += " " + on.get(i);
			s += ", ";
		}
		return s.substring(0, s.length() - 2);
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		List<String> srcneeds = union(remove(on, ""), difference(needs, cols));

		if (strategy == Strategy.COPY)
			return source.optimize(index, srcneeds, by, is_cursor, freeze);

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
		Best best = best_prefixed(indexes, by, srcneeds, is_cursor);
		if (nil(best.index) && nil(index)) {
			best.index = by;
			best.cost = source.optimize(by, srcneeds, noFields, is_cursor,
					false);
		}
		if (nil(best.index))
			return IMPOSSIBLE;
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
				if (prefix_set(src, by))
					idxs.add(src);
			return idxs;
		}
	}

	@Override
	List<List<String>> keys() {
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

	@Override
	Header header() {
		// TODO header
		return null;
	}

	@Override
	Row get(Dir dir) {
		// TODO get
		return null;
	}

	@Override
	void rewind() {
		// TODO rewind
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// TODO select
	}

	@Override
	boolean updateable() {
		return false; // override Query1 source->updateable
	}

}
