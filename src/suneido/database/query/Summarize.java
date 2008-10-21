package suneido.database.query;

import static suneido.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.*;
import suneido.database.Record;

public class Summarize extends Query1 {
	private final List<String> by;
	private final List<String> cols;
	private final List<String> funcs;
	private final List<String> on;
	private final boolean copy;
	private List<String> via;
	private boolean first = true;
	private boolean rewound = true;
	private Header hdr;
	private List<Summary> sums;
	private Row nextrow;
	private Row currow;
	private Dir curdir;
	private Keyrange sel;

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

		copy = by.isEmpty() || by_contains_key();

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
		String s = source + " SUMMARIZE";
		if (copy)
			s += "-COPY";
		s += " ";
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
		List<String> srcneeds = union(remove(on, null), difference(needs, cols));

		if (copy)
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
		if (copy)
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

	@Override
	public Header header() {
		if (first)
			iterate_setup();
		List<String> flds = concat(by, cols);
		return new Header(list(noFields, flds), flds);
	}

	private void iterate_setup() {
		first = false;
		sums = new ArrayList<Summary>();
		for (String f : funcs)
			sums.add(Summary.valueOf(f.toUpperCase()));
		hdr = source.header();
	}

	@Override
	public Row get(Dir dir) {
		if (first)
			iterate_setup();
		if (rewound) {
			rewound = false;
			curdir = dir;
			currow = new Row();
			nextrow = source.get(dir);
			if (nextrow == null)
				return null;
		}

		// if direction changes, have to skip over previous result
		if (dir != curdir) {
			if (nextrow == null)
				source.rewind();
			do
				nextrow = source.get(dir);
			while (nextrow != null && equal());
			curdir = dir;
		}

		if (nextrow == null)
			return null;

		currow = nextrow;
		for (Summary s : sums)
			s.init();
		do {
			if (nextrow == null)
				break;
			for (int i = 0; i < on.size(); ++i)
				sums.get(i).add(nextrow.getval(hdr, on.get(i)));
			nextrow = source.get(dir);
		} while (equal());
		// output after reading a group

		// build a result record
		Record r = new Record();
		for (String f : by)
			r.add(currow.getval(hdr, f));
		for (Summary s : sums)
			r.add(s.result());

		return new Row(Record.MINREC, r);
	}

	private boolean equal() {
		if (nextrow == null)
			return false;
		for (String f : by)
			if (!currow.getval(hdr, f).equals(nextrow.getval(hdr, f)))
				return false;
		return true;
	}

	@Override
	public void rewind() {
		source.rewind();
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
		sel.set(from, to);
		rewound = true;
	}

	@Override
	public boolean updateable() {
		return false; // override Query1 source->updateable
	}

	private static enum Summary {
		COUNT {
			@Override
			void init() {
				n = 0;
			}
			@Override
			void add(SuValue x) {
				++n;
			}
			@Override
			SuValue result() {
				return SuInteger.valueOf(n);
			}
			int n;
		},
		TOTAL {
			@Override
			void init() {
				total = SuInteger.ZERO;
			}
			@Override
			void add(SuValue x) {
				total = total.add(x);
			}
			@Override
			SuValue result() {
				return total;
			}
			SuValue total;
		},
		AVERAGE {
			@Override
			void init() {
				n = 0;
				total = SuInteger.ZERO;
			}
			@Override
			void add(SuValue x) {
				++n;
				total = total.add(x);
			}
			@Override
			SuValue result() {
				return total.div(SuInteger.valueOf(n));
			}
			int n = 0;
			SuValue total;
		},
		MAX {
			@Override
			void init() {
				value = null;
			}
			@Override
			void add(SuValue x) {
				if (value == null || x.compareTo(value) > 0)
					value = x;
			}
			@Override
			SuValue result() {
				return value;
			}
			SuValue value;
		},
		MIN {
			@Override
			void init() {
				value = null;
			}
			@Override
			void add(SuValue x) {
				if (value == null || x.compareTo(value) < 0)
					value = x;
			}
			@Override
			SuValue result() {
				return value;
			}
			SuValue value;
		},
		LIST {
			@Override
			void init() {
				list = new SuContainer();
			}
			@Override
			void add(SuValue x) {
				list.append(x);
			}
			@Override
			SuValue result() {
				return list;
			}
			SuContainer list;
		};

		abstract void init();
		abstract void add(SuValue x);
		abstract SuValue result();
	}

}
