/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.SuInternalError.unreachable;
import static suneido.util.Util.*;

import java.util.*;

import com.google.common.collect.ImmutableList;

import suneido.SuException;
import suneido.database.immudb.Record;
import suneido.database.query.expr.Expr;

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
	private List<String> via;
	enum Strategy {
		NONE(""), COPY("-COPY"), SEQUENTIAL("-SEQ"), LOOKUP("-LOOKUP");
		public String name;

		Strategy(String name) {
			this.name = name;
		}
	}

	static Project project(Query source, List<String> flds) {
		List<String> srcCols = source.columns();
		if (!srcCols.containsAll(flds))
			throw new SuException("project: nonexistent column(s): "
					+ difference(flds, srcCols));
		flds = withoutDups(flds);
		for (var fld : flds) {
			if (fld.endsWith("_lower!"))
				throw new SuException("can't project _lower! fields");
		}
		var p = new Project(source, flds);
		if (p.strategy == Strategy.COPY)
			p.includeDeps(srcCols);
		return p;
	}

	private void includeDeps(List<String> srcCols) {
		for (int i = flds.size() - 1; i >= 0; --i) {
			String f = flds.get(i);
			String deps = f + "_deps";
			if (srcCols.contains(deps) && !flds.contains(deps))
				flds.add(deps);
		}
	}

	static Project remove(Query source, List<String> flds) {
		var srcCols = source.columns();
		var proj = new ArrayList<String>();
		for (var col : srcCols) {
			if (!(flds.contains(col) ||
					col.endsWith("_lower!") ||
					(col.endsWith("_deps") && flds.contains(removeDeps(col)))))
				proj.add(col);
		}
		if (proj.isEmpty())
            throw new SuException("remove: can't remove all columns");
		return new Project(source, proj);
	}

	private static String removeDeps(String field) {
		return field.substring(0, field.length() - 5);
	}

	private Project(Query source, List<String> flds) {
		super(source);
		this.flds = flds;
		if (hasKey(source, flds)) {
			strategy = Strategy.COPY;
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(source.toString())
				.append(" PROJECT").append(strategy.name);
		if (via != null)
			s.append("^").append(listToParens(via));
		return s.append(" ").append(listToParens(flds)).toString();
	}

	private static boolean hasKey(Query source, List<String> flds) {
		var fixed = source.fixed();
		outer:
		for (List<String> keys : source.keys()) {
			for (String fld : keys)
				if (!Query1.isfixed(fixed, fld) && !flds.contains(fld))
					continue outer;
			return true;
		}
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
		return projectKeys(source.keys(), flds);
	}

	public static List<List<String>> projectKeys(List<List<String>> keys,
			List<String> flds) {
		var keys2 = projectIndexes(keys, flds);
		if (keys2.isEmpty())
			keys2.add(flds);
		return keys2;
	}

	@Override
	List<List<String>> indexes() {
		return projectIndexes(source.indexes(), flds);
	}

	private static List<List<String>> projectIndexes(List<List<String>> idxs,
			List<String> flds) {
		var idxs2 = new ArrayList<List<String>>();
		for (var ix : idxs)
			if (flds.containsAll(ix))
				idxs2.add(ix);
		return idxs2;
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
			if (c.disjoint == null || flds.contains(c.disjoint)) {
				c.source = new Project(c.source,
						intersect(flds, c.source.columns()));
				c.source2 = new Project(c.source2,
						intersect(flds, c.source2.columns()));
				c.reset();
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

	private Row getLookup(Dir dir) {
		if (rewound) {
			rewound = false;
			if (dir == Dir.PREV && ! indexed)
				buildLookupIndex();
		}
		Row row;
		while (null != (row = source.get(dir))) {
			Record key = row.project(srcHdr, flds);
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
			Record key = row.project(projHdr, flds);
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
		if (strategy != Strategy.COPY)
			throw new SuException(
					"project: can't output: key required");
		source.output(r);
	}

}
