package suneido.database.query;

import static suneido.Util.*;

import java.util.*;

import suneido.SuException;
import suneido.database.Record;
import suneido.database.query.expr.Expr;

public class Project extends Query1 {
	private List<String> flds;

	enum Strategy {
		NONE(""), COPY("-COPY"), SEQUENTIAL("-SEQ"), LOOKUP("-LOOKUP");
		public String name;

		Strategy(String name) {
			this.name = name;
		}
	};

	private Strategy strategy = Strategy.NONE;
	private boolean first = true;
	private Header hdr;
	// used by LOOKUP
	// VVtree idx = null;
	private Keyrange sel;
	private boolean rewound = true;
	private boolean indexed;
	// used by SEQUENTIAL
	private Row prevrow;
	private Row currow;
	// DestMem td;
	private List<String> via;

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
				: removeDups(args);

		// include dependencies (_deps)
		for (String f : flds) {
			String deps = f + "_deps";
			if (columns.contains(deps) && !flds.contains(deps))
				flds.add(deps);
		}

		// check if project contain candidate key
		if (hasKey(source, flds))
			strategy = Strategy.COPY;
	}

	@Override
	public String toString() {
		String s = source.toString() + " PROJECT" + strategy.name;
		if (via != null)
			s += "^" + listToParens(via);
		return s + " " + listToParens(flds);
	}

	private boolean hasKey(Query source, List<String> flds) {
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
	boolean updateable() {
		return super.updateable() && strategy == Strategy.COPY;
	}

	@Override
	List<List<String>> keys() {
		List<List<String>> keys = new ArrayList<List<String>>();
		for (List<String> k : source.keys())
			if (flds.containsAll(k))
				keys.add(k);
		if (keys.isEmpty())
			keys.add(flds);
		return keys;
	}

	@Override
	List<List<String>> indexes() {
		List<List<String>> idxs = new ArrayList<List<String>>();
		for (List<String> src : source.indexes())
			if (flds.containsAll(src))
				idxs.add(src);
		return idxs;
	}

	@Override
	Query transform() {
		boolean moved = false;
		// remove projects of all fields
		if (flds == source.columns())
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
			List<String> new_from = new ArrayList<String>();
			List<String> new_to = new ArrayList<String>();
			List<String> f = r.from();
			List<String> t = r.to();
			for (int i = 0; i < f.size(); ++i)
				if (flds.contains(t.get(i))) {
					new_from.add(f.get(i));
					new_to.add(t.get(i));
				}
			r.setFrom(new_from);
			r.setTo(new_to);

			// rename fields
			List<String> new_fields = new ArrayList<String>();
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
			List<String> new_flds = new ArrayList<String>();
			List<Expr> new_exprs = new ArrayList<Expr>();
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
			List<String> eflds = new ArrayList<String>();
			for (Expr ex : e.exprs)
				addUnique(eflds, ex.fields());
			if (flds.containsAll(eflds)) {
				// remove extend fields from project
				List<String> new_fields = new ArrayList<String>();
				for (String f : flds)
					if (!e.flds.contains(f))
						new_fields.add(f);
				flds = new_fields;

				source = e.source;
				e.source = this;
				return e.transform();
			}
			e.flds = orig_flds;
			e.exprs = orig_exprs;
		}
		// distribute project over union/intersect (NOT difference)
		else if (source instanceof Compatible
				&& !(source instanceof Difference)) {
			Compatible c = (Compatible) source;
			if (c.disjoint != "" && !flds.contains(c.disjoint)) {
				List<String> flds2 = new ArrayList<String>(flds);
				flds2.add(c.disjoint);
				c.source = new Project(c.source, flds2);
				c.source2 = new Project(c.source2, flds2);
			} else {
				c.source = new Project(c.source, flds);
				c.source2 = new Project(c.source2, flds);
				return source.transform();
			}
		}
		// split project over product/join
		else if (source instanceof Product) {
			Product x = (Product) source;
			x.source = new Project(x.source, intersect(flds, x.source
					.columns()));
			x.source2 = new Project(x.source2, intersect(flds, x.source2
					.columns()));
			moved = true;
		}
		else if (source instanceof Join) {
			Join j = (Join) source;
			if (flds.containsAll(j.joincols)) {
				j.source = new Project(j.source, intersect(flds, j.source
						.columns()));
				j.source2 = new Project(j.source2, intersect(flds,
						j.source2.columns()));
				moved = true;
			}
		}
		source = source.transform();
		return moved ? source : this;
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (strategy == Strategy.COPY)
			return source.optimize(index, needs, firstneeds, is_cursor, freeze);

		// look for index containing result key columns as prefix
		List<String> best_index = null;
		double best_cost = IMPOSSIBLE;
		List<List<String>> idxs = index.isEmpty() ? source.indexes()
				: Collections.singletonList(index);
		for (List<String> ix : idxs)
			// TODO: take fixed into account
			if (prefix_set(ix, flds)) {
				double cost = source.optimize1(ix, needs, firstneeds,
						is_cursor, false);
				if (cost < best_cost) {
					best_cost = cost;
					best_index = ix;
				}
			}
		if (best_index == null) {
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

	@Override
	List<Fixed> fixed() {
		List<Fixed> fixed = new ArrayList<Fixed>();
		for (Fixed f : source.fixed())
			if (flds.contains(f.field))
				fixed.add(f);
		return fixed;
	}

	@Override
	Header header() {
		return source.header().project(flds);
	}

	@Override
	Row get(Dir dir)
	{
		if (strategy == Strategy.COPY)
			return source.get(dir);

		if (first)
		{
			first = false;
			hdr = header();
			if (strategy == Strategy.LOOKUP)
				//			idx = new VVtree(td = new TempDest);
				indexed = false;
		}
		switch (strategy) {
		case SEQUENTIAL:
			return getSequential(dir);
		case LOOKUP:
			return getLookup(dir);
		default:
			throw SuException.unreachable();
		}
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
			while (!rewound && hdr.equal(row, currow));
			rewound = false;
			prevrow = currow;
			currow = row;
			// output the first row of a new group
			return row;
		case PREV:
			// output the last of each group
			// i.e. output when next record is different
			// (to get the same records as NEXT)
			if (rewound)
				prevrow = source.get(Dir.PREV);
			rewound = false;
			do
			{
				if (null == (row = prevrow))
					return null;
				prevrow = source.get(Dir.PREV);
			}
			while (hdr.equal(row, prevrow));
			// output the last row of a group
			currow = row;
			return row;
		default:
			throw SuException.unreachable();
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
			//			Record key = row_to_key(hdr, row, flds);
			//			VVtree::iterator iter = idx.find(key);
			//			if (iter == idx.end())
			//				{
			//				for (List<Record> rs = row.data; ! nil(rs); ++rs)
			//					td.addref(rsptr());
			//				Vdata data(row.data);
			//				verify(idx.insert(VVslot(key, data)));
			//				return row;
			//				}
			//			else
			//				{
			//				Vdata d = iterdata;
			//				Records rs;
			//				for (int i = dn - 1; i >= 0; --i)
			//					rs.add(Record::from_int(dr[i], theDB().mmf));
			//				Row i.row(rs);
			//				if (row == i.row)
			//					return row;
			//				}
		}
		if (dir == Dir.NEXT)
			indexed = true;
		return null;
	}

	private void buildLookupIndex() {
		Row row;
		// pre-build the index
		while (null != (row = source.get(Dir.NEXT))) {
			//			Record key = row_to_key(hdr, row, flds);
			//			Vdata data(row.data);
			//			for (List<Record> rs = row.data; ! nil(rs); ++rs)
			//				td.addref(rs.ptr());
			//			// insert will only succeed on first of dups
			//			idx.insert(VVslot(key, data));
		}
		source.rewind();
		indexed = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
		if (strategy == Strategy.LOOKUP && (sel.org != from || sel.end != to))
			// idx = new VVtree(td = new DestMem());
			indexed = false;
		sel.org = from;
		sel.end = to;
		rewound = true;
	}

	@Override
	void rewind() {
		source.rewind();
		rewound = true;
	}

	@Override
	void output(Record r) {
		ckmodify("output");
		source.output(r);
	}

	void ckmodify(String action) {
		if (strategy != Strategy.COPY)
			throw new SuException(
					"project: can't " + action + ": key required");
	}

}
