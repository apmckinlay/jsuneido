/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.SuInternalError.unreachable;
import static suneido.util.Util.addAllUnique;
import static suneido.util.Util.difference;
import static suneido.util.Util.intersect;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.nil;
import static suneido.util.Util.setEquals;
import static suneido.util.Util.startsWithSet;
import static suneido.util.Util.withoutDups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import suneido.SuException;
import suneido.database.query.expr.Expr;
import suneido.intfc.database.Record;

import com.google.common.collect.ImmutableList;

public class Project extends Query1 {
	private List<String> flds;
	private Strategy strategy = Strategy.NONE;
	private boolean first = true;
	private Header projHdr;
	private Header srcHdr;
	private Lookup map = null;
	private final Keyrange sel = new Keyrange();
	private boolean rewound = true;
	private boolean indexed;
	// used by SEQUENTIAL
	private Row prevrow;
	private Row currow;
	// DestMem td;
	private List<String> via;
	enum Strategy {
		NONE(""), COPY("-COPY"), SEQUENTIAL("-SEQ"), LOOKUP("-LOOKUP");
		public String name;

		Strategy(String name) {
			this.name = name;
		}
	};

	Project(Query source, List<String> flds) {
		this(source, flds, false);
	}

	Project(Query source, List<String> args, boolean allbut) {
		super(source);

		List<String> columns = source.columns();
		if (! columns.containsAll(args))
			throw new SuException("project: nonexistent column(s): "
					+ difference(args, columns));
		flds = allbut
				? difference(columns, args)
				: withoutDups(args);

		if (hasKey(source, flds)) {
			strategy = Strategy.COPY;
			includeDeps(columns);
		}
	}

	private void includeDeps(List<String> columns) {
		for (int i = flds.size() - 1; i >= 0; --i) {
			String f = flds.get(i);
			String deps = f + "_deps";
			if (columns.contains(deps) && !flds.contains(deps))
				flds.add(deps);
		}
	}

	@Override
	public String toString() {
		String s = source.toString() + " PROJECT" + strategy.name;
		if (via != null)
			s += "^" + listToParens(via);
		return s + " " + listToParens(flds);
	}

	private static boolean hasKey(Query source, List<String> flds) {
		for (List<String> k : source.keys())
			if (flds.containsAll(k))
				return true;
		return false;
	}

	@Override
	List<String> columns() {
		return flds;
	}

	@Override
	double nrecords() {
		return source.nrecords() / (strategy == Strategy.COPY ? 1 : 2);
	}

	@Override
	public boolean updateable() {
		return source.updateable() && strategy == Strategy.COPY;
	}

	@Override
	public List<List<String>> keys() {
		List<List<String>> keys = new ArrayList<>();
		for (List<String> k : source.keys())
			if (flds.containsAll(k))
				keys.add(k);
		if (keys.isEmpty())
			keys.add(flds);
		return keys;
	}

	@Override
	List<List<String>> indexes() {
		List<List<String>> idxs = new ArrayList<>();
		for (List<String> src : source.indexes())
			if (flds.containsAll(src))
				idxs.add(src);
		return idxs;
	}

	@Override
	Query transform() {
		boolean moved = false;
		// remove projects of all fields
		if (setEquals(flds, source.columns()))
			return source.transform();
		// combine projects
		if (source instanceof Project) {
			Project p = (Project) source;
			flds = intersect(flds, p.flds);
			source = p.source;
			return transform();
		}
		// move projects before renames, renaming
		else if (source instanceof Rename) {
			Rename r = (Rename) source;
			// remove renames not in project
			List<String> new_from = new ArrayList<>();
			List<String> new_to = new ArrayList<>();
			List<String> f = r.from;
			List<String> t = r.to;
			for (int i = 0; i < f.size(); ++i)
				if (flds.contains(t.get(i))) {
					new_from.add(f.get(i));
					new_to.add(t.get(i));
				}
			r.from = new_from;
			r.to = new_to;

			// rename fields
			List<String> new_fields = new ArrayList<>();
			for (String fld : flds) {
				int i = t.indexOf(fld);
				new_fields.add(i == -1 ? fld : f.get(i));
			}
			flds = new_fields;

			source = r.source;
			r.source = this;
			return r.transform();
		}
		// move projects before extends
		else if (source instanceof Extend) {
			Extend e = (Extend) source;
			// remove portions of extend not included in project
			List<String> new_flds = new ArrayList<>();
			List<Expr> new_exprs = new ArrayList<>();
			for (int i = 0; i < e.flds.size(); ++i)
				if (flds.contains(e.flds.get(i))) {
					new_flds.add(e.flds.get(i));
					new_exprs.add(e.exprs.get(i));
				}
			List<String> orig_flds = e.flds;
			e.flds = new_flds;
			List<Expr> orig_exprs = e.exprs;
			e.exprs = new_exprs;

			// project must include all fields required by extend
			// there must be no rules left
			// since we don't know what fields are required by rules
			if (! e.hasRules()) {
				List<String> eflds = new ArrayList<>();
				for (Expr ex : e.exprs)
					addAllUnique(eflds, ex.fields());
				if (flds.containsAll(eflds)) {
					// remove extend fields from project
					List<String> new_fields = new ArrayList<>();
					for (String f : flds)
						if (!e.flds.contains(f))
							new_fields.add(f);
					flds = new_fields;

					source = e.source;
					e.source = this;
					e.init();
					return e.transform();
				}
			}
			e.flds = orig_flds;
			e.exprs = orig_exprs;
		}
		// distribute project over union/intersect (NOT difference)
		else if (source instanceof Union || source instanceof Intersect) {
			Compatible c = (Compatible) source;
			if (c.disjoint != null && ! flds.contains(c.disjoint)) {
				List<String> flds2 = new ArrayList<>(flds);
				flds2.add(c.disjoint);
				c.source = new Project(c.source,
						intersect(flds2, c.source.columns()));
				c.source2 = new Project(c.source2,
						intersect(flds2, c.source2.columns()));
			} else {
				c.source = new Project(c.source,
						intersect(flds, c.source.columns()));
				c.source2 = new Project(c.source2,
						intersect(flds, c.source2.columns()));
				return source.transform();
			}
		}
		// split project over product
		else if (source instanceof Product) {
			Product x = (Product) source;
			x.source = new Project(x.source,
					intersect(flds, x.source.columns()));
			x.source2 = new Project(x.source2,
					intersect(flds, x.source2.columns()));
			moved = true;
		}
		// split project over join
		else if (source instanceof Join) {
			Join j = (Join) source;
			if (flds.containsAll(j.joincols)) {
				j.source = new Project(j.source,
						intersect(flds, j.source.columns()));
				j.source2 = new Project(j.source2,
						intersect(flds, j.source2.columns()));
				moved = true;
			}
		}
		source = source.transform();
		return moved ? source : this;
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {

		// check if project contain candidate key
		if (strategy == Strategy.COPY)
			return source.optimize(index, needs, firstneeds, is_cursor, freeze);

		// look for index containing result key columns as prefix
		List<String> best_index = null;
		double best_cost = IMPOSSIBLE;
		List<List<String>> idxs = index.isEmpty() ? source.indexes()
				: Collections.singletonList(index);
		List<Fixed> fixed = source.fixed();
		List<String> unfixed_flds = withoutFixed(flds, fixed);
		for (List<String> ix : idxs)
			if (startsWithSet(withoutFixed(ix, fixed), unfixed_flds)) {
				// NOTE: optimize1 to avoid tempindex
				double cost = source.optimize1(ix, needs, firstneeds,
						is_cursor, false);
				if (cost < best_cost) {
					best_cost = cost;
					best_index = ix;
				}
			}
		if (nil(best_index)) {
			if (is_cursor)
				return IMPOSSIBLE;
			if (freeze)
				strategy = Strategy.LOOKUP;
			return 2 * source.optimize(index, needs, firstneeds, is_cursor,
					freeze); // 2 for lookups
		} else {
			if (!freeze)
				return best_cost;
			strategy = Strategy.SEQUENTIAL;
			via = best_index;
			// NOTE: optimize1 to avoid tempindex
			return source.optimize1(best_index, needs, firstneeds, is_cursor,
					freeze);
		}
	}

	List<String> withoutFixed(List<String> list, List<Fixed> fixed) {
		if (! hasFixed(list, fixed))
			return list;
		ImmutableList.Builder<String> bldr = ImmutableList.builder();
		for (String fld : list)
			if (! isfixed(fixed, fld))
				bldr.add(fld);
		return bldr.build();
	}

	boolean hasFixed(List<String> list, List<Fixed> fixed) {
		for (String fld : list)
			if (isfixed(fixed, fld))
				return true;
		return false;
	}

	@Override
	List<Fixed> fixed() {
		List<Fixed> fixed = new ArrayList<>();
		for (Fixed f : source.fixed())
			if (flds.contains(f.field))
				fixed.add(f);
		return fixed;
	}

	@Override
	public Header header() {
		return source.header().project(flds);
	}

	@Override
	public Row get(Dir dir) {
		if (first) {
			first = false;
			srcHdr = source.header();
			projHdr = srcHdr.project(flds);
			if (strategy == Strategy.LOOKUP) {
				map = new Lookup();
				indexed = false;
			}
		}
		switch (strategy) {
		case COPY:
			return getCopy(dir);
		case SEQUENTIAL:
			return getSequential(dir);
		case LOOKUP:
			return getLookup(dir);
		default:
			throw unreachable();
		}
	}

	private Row getCopy(Dir dir) {
		return source.get(dir);
	}

	private Row getSequential(Dir dir) {
		Row row;
		switch (dir) {
		case NEXT:
			// output the first of each group
			// i.e. skip over rows the same as previous output
			do
				if (null == (row = source.get(Dir.NEXT)))
					return null;
				while (! rewound && projHdr.equal(row, currow));
			rewound = false;
			prevrow = currow;
			currow = row;
			return row;
		case PREV:
			// output the last of each group
			// i.e. output when next record is different
			// (to get the same records as NEXT)
			if (rewound)
				prevrow = source.get(Dir.PREV);
			rewound = false;
			do {
				if (null == (row = prevrow))
					return null;
				prevrow = source.get(Dir.PREV);
			} while (prevrow != null && projHdr.equal(row, prevrow));
			// output the last row of a group
			currow = row;
			return row;
		default:
			throw unreachable();
		}
	}

	//TODO pack keys into RecordStorage to reduce per-object overhead
	// not easy because then you need int to object map with custom hash
	private Row getLookup(Dir dir) {
		if (rewound) {
			rewound = false;
			if (dir == Dir.PREV && ! indexed)
				buildLookupIndex();
		}
		Row row;
		while (null != (row = source.get(dir))) {
			Record key = row.project(srcHdr, flds).squeeze();
			Object[] data = map.get(key);
			if (data == null) {
				map.put(key, row.getRefs());
				return row;
			} else if (Arrays.equals(data, row.getRefs()))
				return row;
		}
		if (dir == Dir.NEXT)
			indexed = true;
		return null;
	}

	private void buildLookupIndex() {
		Row row;
		while (null != (row = source.get(Dir.NEXT))) {
			Record key = row.project(projHdr, flds).squeeze();
			if (null == map.get(key))
				map.put(key, row.getRefs());
		}
		source.rewind();
		indexed = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
		if (strategy == Strategy.LOOKUP && !sel.equals(from, to))
			// idx = new VVtree(td = new DestMem());
			indexed = false;
		sel.set(from, to);
		rewound = true;
	}

	@Override
	public void rewind() {
		source.rewind();
		rewound = true;
	}

	@Override
	public void output(Record r) {
		ckmodify("output");
		source.output(r);
	}

	void ckmodify(String action) {
		if (strategy != Strategy.COPY)
			throw new SuException(
					"project: can't " + action + ": key required");
	}

}
