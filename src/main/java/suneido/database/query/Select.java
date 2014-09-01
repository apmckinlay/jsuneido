/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.SuInternalError.unreachable;
import static suneido.Suneido.dbpkg;
import static suneido.Trace.trace;
import static suneido.Trace.tracing;
import static suneido.Trace.Type.SELECT;
import static suneido.Trace.Type.SLOWQUERY;
import static suneido.intfc.database.Record.MAX_FIELD;
import static suneido.intfc.database.Record.MIN_FIELD;
import static suneido.language.Token.IS;
import static suneido.language.Token.ISNT;
import static suneido.util.ByteBuffers.bufferUcompare;
import static suneido.util.Util.*;
import static suneido.util.Verify.verify;
import gnu.trove.set.hash.TIntHashSet;

import java.nio.ByteBuffer;
import java.util.*;

import suneido.SuException;
import suneido.database.query.expr.*;
import suneido.database.server.DbmsTranLocal;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.intfc.database.Transaction;
import suneido.language.Ops;
import suneido.language.Pack;
import suneido.language.Token;
import suneido.util.ByteBuffers;
import suneido.util.CommaStringBuilder;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

public class Select extends Query1 {
	private Multi expr;
	private boolean optFirst = true;
	private boolean conflicting = false;
	private List<Fixed> fix;
	private List<String> source_index;	// may have extra stuff on the end
										// or be missing fields that are fixed
	private List<List<String>> filter;
	private Set<String> select_needs;
	private Table tbl;
	private List<String> primary;
	private List<List<String>> theindexes;
	private Map<String, Iselect> isels;
	private List<List<String>> possible;
	private Map<String, Double> ffracs;
	private Map<List<String>, Double> ifracs;
	private Set<String> prior_needs;
	private double nrecs = -1;
	// for get
	private boolean getFirst = true;
	private boolean rewound = true;
	private List<Keyrange> ranges = Collections.emptyList();
	private int range_i = 0;
	private final Keyrange sel = new Keyrange();
	private boolean newrange = true;;
	int n_in = 0;
	int n_out = 0;
	private TIntHashSet filterSet;
	private Header hdr;
	private Transaction tran;

	public Select(Transaction tran, Query source, Expr expr) {
		super(source);
		this.tran = tran;
		expr = expr.fold();
		if (!(expr instanceof And))
			expr = new And().add(expr);
		 if (!source.columns().containsAll(expr.fields()))
			throw new SuException("select: nonexistent columns: "
					+ listToParens(difference(expr.fields(), source.columns())));
		this.expr = (Multi) expr;
	}

	@Override
	public String toString() {
		CommaStringBuilder sb = new CommaStringBuilder(source.toString());
		sb.append(" WHERE");
		if (conflicting)
			return sb.append(" nothing").toString();
		if (! nil(source_index))
			sb.append("^").append(listToParens(source_index));
		if (filter != null) {
			sb.append("%(");
			for (List<String> f : filter)
				sb.add(listToParens(f));
			sb.append(")");
		}
		if (! nil(expr.exprs))
			sb.append(" ").append(expr);
		return sb.toString();
	}

	@Override
	List<Fixed> fixed() {
		if (fix != null)
			return fix;
		fix = new ArrayList<>();
		for (Expr e : expr.exprs) {
			Fixed fixed = fixed(e);
			if (fixed != null)
				fix.add(fixed);
		}
		fix = Fixed.combine(fix, source.fixed());
		return fix;
	}

	private static Fixed fixed(Expr e) {
		// MAYBE: handle IN
		if (e instanceof BinOp) {
			BinOp binop = (BinOp) e;
			if (binop.op == IS &&
					binop.left instanceof Identifier &&
					binop.right instanceof Constant) {
				String field = ((Identifier) binop.left).ident;
				Object value = ((Constant) binop.right).value;
				return new Fixed(field, value);
			}
		}
		return null;
	}

	@Override
	Query transform() {
		boolean moved = false;
		// remove empty selects
		if (nil(expr.exprs))
			return source.transform();
		// combine selects
		if (source instanceof Select) {
			Select s = (Select) source;
			expr = new And(concat(s.expr.exprs, expr.exprs));
			source = s.source;
			return transform();
		}
		// move selects before projects
		else if (source instanceof Project) {
			Project p = (Project) source;
			source = p.source;
			p.source = this;
			return p.transform();
		}
		// move selects before renames
		else if (source instanceof Rename) {
			Rename r = (Rename) source;
			Expr new_expr = expr.rename(r.to, r.from);
			source = r.source;
			r.source = (new_expr == expr ? this : new Select(tran, source, new_expr));
			return r.transform();
		}
		// move select before extend, unless it depends on rules
		else if (source instanceof Extend) {
			Extend extend = (Extend) source;
			List<Expr> src1 = new ArrayList<>();
			List<Expr> rest = new ArrayList<>();
			for (Expr e : expr.exprs)
				if (extend.needRule(e.fields()))
					rest.add(e);
				else
					src1.add(e.replace(extend.flds, extend.exprs));
			if (!nil(src1))
				extend.source = new Select(tran, extend.source, new And(src1));
			if (!nil(rest))
				expr = new And(rest);
			else
				moved = true;
		}
		// split select before & after summarize
		else if (source instanceof Summarize) {
			Summarize summarize = (Summarize) source;
			List<String> flds1 = summarize.source.columns();
			List<Expr> src1 = new ArrayList<>();
			List<Expr> rest = new ArrayList<>();
			for (Expr e : expr.exprs)
				if (flds1.containsAll(e.fields()))
					src1.add(e);
				else
					rest.add(e);
			if (!nil(src1))
				summarize.source = new Select(tran, summarize.source, new And(src1));
			if (!nil(rest))
				expr = new And(rest);
			else
				moved = true;
		}
		// distribute select over intersect
		else if (source instanceof Intersect) {
			Intersect q = (Intersect) source;
			q.source = new Select(tran, q.source, expr);
			q.source2 = new Select(tran, q.source2, expr);
			moved = true;
		}
		// distribute select over difference
		else if (source instanceof Difference) {
			Difference q = (Difference) source;
			q.source = new Select(tran, q.source, expr);
			q.source2 = new Select(tran, q.source2, project(q.source2));
			moved = true;
		}
		// distribute select over union
		else if (source instanceof Union) {
			Query2 q = (Query2) source;
			q.source = new Select(tran, q.source, project(q.source));
			q.source2 = new Select(tran, q.source2, project(q.source2));
			moved = true;
		}
		// split select over product
		else if (source instanceof Product) {
			Product x = (Product) source;
			moved = distribute(x);
		}
		// split select over leftjoin (left side only)
		else if (source instanceof LeftJoin) {
			LeftJoin j = (LeftJoin) source;
			List<String> flds1 = j.source.columns();
			List<Expr> common = new ArrayList<>();
			List<Expr> src1 = new ArrayList<>();
			for (Expr e : expr.exprs)
				if (flds1.containsAll(e.fields()))
					src1.add(e);
				else
					common.add(e);
			if (!nil(src1))
				j.source = new Select(tran, j.source, new And(src1));
			if (!nil(common))
				expr = new And(common);
			else
				moved = true;
		}
		// NOTE: must check for LeftJoin before Join since it's derived
		// split select over join
		else if (source instanceof Join) {
			Join j = (Join) source;
			moved = distribute(j);
		}
		source = source.transform();
		return moved ? source : this;
	}

	private Expr project(Query q) {
		List<String> srcflds = q.columns();
		List<String> exprflds = expr.fields();
		List<String> missing = difference(exprflds, srcflds);
		return expr.replace(missing,
				Collections.nCopies(missing.size(), (Expr) Constant.EMPTY));
	}

	private boolean distribute(Query2 q2) {
		List<String> flds1 = q2.source.columns();
		List<String> flds2 = q2.source2.columns();
		List<Expr> common = new ArrayList<>();
		List<Expr> src1 = new ArrayList<>();
		List<Expr> src2 = new ArrayList<>();
		for (Expr e : expr.exprs) {
			boolean used = false;
			if (flds1.containsAll(e.fields())) {
				src1.add(e);
				used = true;
			}
			if (flds2.containsAll(e.fields())) {
				src2.add(e);
				used = true;
			}
			if (! used)
				common.add(e);
		}
		if (! nil(src1))
			q2.source = new Select(tran, q2.source, new And(src1));
		if (! nil(src2))
			q2.source2 = new Select(tran, q2.source2, new And(src2));
		if (! nil(common))
			expr = new And(common);
		else
			return true;
		return false;
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (optFirst) {
			prior_needs = needs;
			select_needs = ImmutableSet.copyOf(expr.fields());
			tbl = source instanceof Table ? (Table) source : null;
			isels = new HashMap<>();
		}
		if (tbl == null || // source isnt a Table
				tbl.singleton) { // empty key() singleton, index irrelevant
			optFirst = false;
			source_index = index;
			double cost = source.optimize(index, setUnion(needs, select_needs),
					setUnion(firstneeds, select_needs), is_cursor, freeze);
			if (cost < IMPOSSIBLE)
				nrecs = source.nrecords();
			return cost;
		}

		if (tracing(SELECT)) {
			trace(SELECT, "Select::optimize " + source + (freeze ? " FREEZE" : "") +
					" index " + index + ", needs " + needs);
			trace(SELECT, "orig exprs: " + expr);
		}
		if (optFirst) {
			optFirst = false;
			optimize_setup();
		}

		if (conflicting)
			return 0;

		primary = null;
		filter = null;

		double cost = choose_primary(index);
		if (primary == null)
			return IMPOSSIBLE;

		if (!freeze)
			return cost;

		source_index = primary;
		tbl.select_index(source_index);

		return cost;
	}

	private void optimize_setup() {
		fixed(); // calc before altering expr

		theindexes = tbl.indexes();

		ffracs = new HashMap<>();
		List<Cmp> cmps = extract_cmps(); // WARNING: modifies expr
		cmps_to_isels(cmps);
		if (conflicting) {
			nrecs = 0;
			return;
		}
		possible = new ArrayList<>();
		identify_possible();
		calc_field_fracs();
		ifracs = new HashMap<>();
		calc_index_fracs();

		// TODO should be frac of complete select, not just indexes
		nrecs = datafrac(theindexes) * tbl.nrecords();
	}
	private List<Cmp> extract_cmps() {
		List<Cmp> cmps = new ArrayList<>();
		List<String> fields = tbl.tbl.getFields();
		List<Expr> new_exprs = new ArrayList<>();
		for (Expr e : expr.exprs) {
			if (e == Constant.FALSE)
				conflicting = true;

			if (e.isTerm(fields))
				if (e instanceof In) {
					In in = (In) e;
					Identifier id = (Identifier) in.expr;
					cmps.add(new Cmp(id.ident, in.packed));
					continue;
				} else if (e instanceof BinOp) {
					BinOp binop = (BinOp) e;
					if (binop.op != ISNT) {
						String field = ((Identifier) binop.left).ident;
						ByteBuffer value = ((Constant) binop.right).packed;
						cmps.add(new Cmp(field, binop.op, value));
						continue;
					}
				}

			if (e instanceof BinOp) {
				BinOp binop = (BinOp) e;
				if ((binop.op == Token.MATCH || binop.op == Token.MATCHNOT || binop.op == Token.ISNT)
						&& binop.left.isField(fields)
						&& binop.right instanceof Constant) {
					String field = ((Identifier) binop.left).ident;
					ffracs.put(field, .5);
				}
			}

			new_exprs.add(e);
		}
		if (new_exprs.size() != expr.exprs.size())
			expr = new And(new_exprs);
		if (tracing(SELECT)) {
			trace(SELECT, "exprs: " + expr);
			trace(SELECT, "cmps: " + cmps);
		}
		return cmps;
	}
	private void cmps_to_isels(List<Cmp> cmps) {
		if (cmps.isEmpty())
			return ;
		Collections.sort(cmps);
		Iselect isel = new Iselect();
		for (int cmpi = 0; cmpi < cmps.size(); ++cmpi) {
			Cmp cmp = cmps.get(cmpi);
			Iselect r = new Iselect();
			if (cmp.op == null) { // IN
				r.values = cmp.values;
				r.type = IselType.VALUES;
			} else
				switch (cmp.op) {
				case IS:	r.org.x = r.end.x = cmp.value; break;
				case LT:	r.end.x = cmp.value; r.end.d = -1; break;
				case LTE:	r.end.x = cmp.value; break;
				case GT:	r.org.x = cmp.value; r.org.d = +1; break;
				case GTE:	r.org.x = cmp.value; break;
				default:	throw unreachable();
				}
			isel.and_with(r);

			if (cmpi + 1 >= cmps.size()
					|| !cmp.ident.equals(cmps.get(cmpi + 1).ident)) {
				// end of group
				if (isel.none())
					{ nrecs = 0; conflicting = true; return ; }
				if (isel.values != null)
					Collections.sort(isel.values, ByteBuffers::bufferUcompare);
				if (! isel.all())
					isels.put(cmp.ident, isel);
				isel = new Iselect();
			}
		}
		if (tracing(SELECT))
			trace(SELECT, "isels: " + isels);
	}


	private void identify_possible() {
		// possible = indexes with isels
		for (List<String> idx : theindexes)
			for (String fld : idx)
				if (isels.containsKey(fld)) {
					possible.add(idx);
					break;
				}
	}

	private void calc_field_fracs() {
		for (List<String> idx : possible)
			for (String fld : idx)
				if (isels.containsKey(fld) && !ffracs.containsKey(fld))
					ffracs.put(fld, field_frac(fld));
	}

	private double field_frac(String field) {
		List<String> best_index = null;
		int best_size = Integer.MAX_VALUE;
		// look for smallest index starting with field
		for (List<String> idx : theindexes)
			if (idx.get(0).equals(field) && tbl.indexSize(idx) < best_size) {
				best_index = idx;
				best_size = tbl.indexSize(idx);
			}
		if (best_index == null)
			return .5;
		Iselect fsel = isels.get(field);
		double tmp = iselsize(best_index, asList(fsel));
		if (Double.isNaN(tmp))
			throw new SuException("field_frac " + field +
					" => iselsize " + best_index + " " + asList(fsel) +
					" => NaN");
		return tmp;
	}

	private void calc_index_fracs() {
		// ifracs = fraction selected from each index
		for (List<String> idx : theindexes) {
			List<Iselect> multisel = iselects(idx);
			ifracs.put(idx, multisel.isEmpty() ? (double) 1 : iselsize(idx, multisel));
		}
	}
	private List<Iselect> iselects(List<String> idx) {
		List<Iselect> multisel = new ArrayList<>();
		for (String field : idx) {
			Iselect r;
			if (null == (r = isels.get(field)))
				break;
			multisel.add(r);
			if (r.type == IselType.RANGE && !r.one())
				break; // can't add anything after range
		}
		return multisel;
	}

	public float iselsize(List<String> index, List<Iselect> iselects) {

		// BUG still calling rangefrac for exact match on key

		// first check for matching a known number of records
		if (keys().contains(index) && index.size() == iselects.size()) {
			int nexact = 1;
			for (Iselect isel : iselects) {
				verify(! isel.none());
				if (isel.type == IselType.VALUES)
					nexact *= isel.values.size();
				else if (isel.org != isel.end) { // range
					nexact = 0;
					break ;
				}
			}
			if (nexact > 0) {
				int nrecs = tbl.nrecs();
				if (nrecs <= 0)
					return 0;
				return (float) nexact / nrecs;
				// NOTE: assumes they all exist ???
			}
		}

		// MAYBE: convert this to use Select::selects()

		for (Iselect isel : iselects) {
			verify(! isel.none());
			if (isel.one())
				continue;
			if (isel.type == IselType.RANGE)
				break ;
			// set - recurse through values
			List<ByteBuffer> save = isel.values;
			float sum = 0;
			for (ByteBuffer v : isel.values) {
				isel.values = asList(v);
				sum += iselsize(index, iselects);
			}
			isel.values = save;
			return sum;
		}

		// now build the key
		int i = 0;
		int n = index.size();
		RecordBuilder org = dbpkg.recordBuilder();
		RecordBuilder end = dbpkg.recordBuilder();
		for (int iseli = 0; iseli < iselects.size(); ++iseli, ++i) {
			Iselect isel = iselects.get(iseli);
			verify(! isel.none());
			if (isel.one()) {
				if (isel.type == IselType.RANGE) {
					org.add(isel.org.x);
					end.add(isel.org.x);
				} else { // in set
					org.add(isel.values.get(0));
					end.add(isel.values.get(0));
				}
				if (iseli == iselects.size() - 1)
					// final exact value (inclusive end)
					addMax(++i, n, end);
			} else if (isel.type == IselType.RANGE) {
				// final range
				org.add(isel.org.x);
				end.add(isel.end.x);
				++i;
				if (isel.org.d != 0) // exclusive
					addMax(i, n, org);
				if (isel.end.d == 0) // inclusive
					addMax(i, n, end);
				break ;
			} else
				throw unreachable();
		}
		Record rorg = org.build();
		Record rend = end.build();
		float frac = tran.rangefrac(tbl.num(), listToCommas(index), rorg, rend);
		if (tracing(SELECT))
			trace(SELECT, tbl.tbl.name() + "^" + index +
					" from " + rorg + " to " + rend + " => " + frac);
		return frac;
	}

	private double datafrac(List<List<String>> indexes) {
		// take the union of all the index fields
		// to ensure you don't use a field more than once
		List<String> flds = new ArrayList<>();
		for (List<String> idx : indexes)
			addAllUnique(flds, idx);

		// frac = product of frac of each field
		double frac = 1;
		for (String fld : flds) {
			Double f = ffracs.get(fld);
			if (f != null)
				frac *= f;
			assert frac >= 0;
		}
		return frac;
	}

	private double choose_primary(List<String> index) {
		// find index that satisfies required index with least cost
		double best_cost = IMPOSSIBLE;
		for (List<String> idx : theindexes) {
			if (!prefixed(idx, index, fixed()))
				continue;
			double cost = primarycost(idx);
			if (cost < best_cost) {
				primary = idx;
				best_cost = cost;
			}
		}
		return best_cost;
	}

	private double primarycost(List<String> idx) {
		double index_read_cost = ifracs.get(idx) * tbl.indexSize(idx);

		double data_frac =
				idx.containsAll(select_needs) && idx.containsAll(prior_needs)
						? 0 : datafrac(asList(idx));

		double data_read_cost = data_frac * tbl.totalSize();

		return index_read_cost + data_read_cost;
	}

	// end of optimize ==============================================

	@Override
	double nrecords() {
		assert (nrecs >= 0);
		return nrecs;
	}

	@Override
	public void setTransaction(Transaction tran) {
		super.setTransaction(tran);
		this.tran = tran;
	}

	// get ----------------------------------------------------------

	@Override
	public Row get(Dir dir) {
		if (conflicting)
			return null;
		if (getFirst) {
			getFirst = false;
			iterate_setup();
		}
		if (rewound) {
			rewound = false;
			newrange = true;
			range_i = (dir == Dir.NEXT ? -1 : ranges.size()); // allow for ++/--
		}
		while (true) {
			if (newrange) {
				Keyrange range;
				do 	{
					range_i += (dir == Dir.NEXT ? 1 : -1);
					if (dir == Dir.NEXT ? range_i >= ranges.size() : range_i < 0)
						return null;
					range = Keyrange.intersect(sel, ranges.get(range_i));
				} while (range.isEmpty());
				source.select(source_index, range.org, range.end);
				newrange = false;
				}
			Row row;
			do {
				row = source.get(dir);
				++n_in;
			} while (row != null && !matches(row));
			if (row != null) {
				++n_out;
				return row;
			}
			newrange = true;
		}
	}

	private void iterate_setup() {
		processFilters();
		hdr = source.header();
		ranges = selects(source_index, iselects(source_index));
		if (tracing(SELECT))
			trace(SELECT, "ranges: " + ranges);
	}

	private void processFilters() {
		if (nil(filter))
			return;

		for (List<String> ix : filter) {
			TIntHashSet newset = new TIntHashSet();
			tbl.set_index(ix);
			for (Keyrange range : selects(ix, iselects(ix))) {
				for (source.select(ix, range.org, range.end);
						null != source.get(Dir.NEXT); )
					if (matches(ix, tbl.iter.curKey())
							&& (filterSet == null || filterSet.contains(tbl.iter.keyadr())))
						newset.add(tbl.iter.keyadr());
			}
			filterSet = newset;
		}
		tbl.set_index(source_index); // restore primary index

		// remove filter isels - no longer needed
		for (List<String> idx : filter)
			for (String fld : idx)
				isels.remove(fld);
	}

	private List<Keyrange> selects(List<String> index, List<Iselect> iselects) {
		for (Iselect isel : iselects) {
			verify(!isel.none());
			if (isel.one())
				continue;
			if (isel.type == IselType.RANGE)
				break;
			// set - recurse through values
			List<ByteBuffer> save = isel.values;
			List<Keyrange> result = new ArrayList<>();
			for (ByteBuffer value : isel.values) {
				isel.values = asList(value);
				result.addAll(selects(index, iselects));
			}
			isel.values = save;
			return result;
		}

		// now build the keys
		int i = 0;
		int n = index.size();
		RecordBuilder org = dbpkg.recordBuilder();
		RecordBuilder end = dbpkg.recordBuilder();
		if (nil(iselects))
			end.addMax();
		for (int iseli = 0; iseli < iselects.size(); ++iseli) {
			Iselect isel = iselects.get(iseli);
			verify(!isel.none());
			if (isel.one()) {
				if (isel.type == IselType.RANGE) {
					// range
					org.add(isel.org.x);
					end.add(isel.org.x);
				} else {
					// in set
					org.add(isel.values.get(0));
					end.add(isel.values.get(0));
				}
				if (iseli + 1 >= iselects.size()) {
					// final exact value (inclusive end)
					addMax(++i, n, end);
				}
			} else if (isel.type == IselType.RANGE) {
				// final range
				org.add(isel.org.x);
				end.add(isel.end.x);
				++i;
				if (isel.org.d != 0) // exclusive
					addMax(i, n, org);
				if (isel.end.d == 0) // inclusive
					addMax(i, n, end);
				break;
			} else
				throw unreachable();
		}
		return asList(new Keyrange(org.build(), end.build()));
	}

	private static void addMax(int i, int n, RecordBuilder end) {
		for (int j = i; j < n; ++j)
			end.addMax();
		if (i >= n) // ensure at least one added
			end.addMax();
	}

	private boolean matches(Row row) {
		// first check against filter
		if (filterSet != null && ! filterSet.contains(tbl.iter.keyadr()))
			return false;

		// then check against isels
		// PERF: check keys before data (every other one)
		for (Map.Entry<String,Iselect> e : isels.entrySet()) {
			Iselect isel = e.getValue();
			ByteBuffer value = row.getraw(hdr, e.getKey());
			if (! isel.matches(value))
				return false;
		}
		// finally check remaining expressions
		row.setTransaction(new DbmsTranLocal(tran));
		return expr.eval(hdr, row) == Boolean.TRUE;
	}

	private boolean matches(List<String> idx, Record key) {
		for (int i = 0; i < idx.size(); ++i)
			if (!isels.get(idx.get(i)).matches(key.getRaw(i)))
				return false;
		return true;
	}

	// end of get ---------------------------------------------------

	@Override
	public void rewind() {
		source.rewind();
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		if (conflicting) {
			sel.setNone();
		} else if (startsWith(source_index, index)) {
			sel.set(from, to);
		} else
			convert_select(index, from, to);
		rewound = true;
	}

	private void convert_select(List<String> index, Record from, Record to) {
		// PERF: could optimize for case where from == to
		if (from.equals(dbpkg.minRecord()) && to.equals(dbpkg.maxRecord())) {
			sel.setAll();
			return ;
		}
		RecordBuilder newfrom = dbpkg.recordBuilder();
		RecordBuilder newto = dbpkg.recordBuilder();
		int si = 0; // source_index;
		int ri = 0; // index;
		Object fixval;
		while (ri < index.size()) {
			String ridx = index.get(ri);
			if (si < source_index.size() && source_index.get(si).equals(ridx)) {
				int i = index.indexOf(ridx);
				newfrom.add(from.getRaw(i));
				newto.add(to.getRaw(i));
				++si;
				++ri;
			}
			else if (si < source_index.size()
					&& null != (fixval = getfixed(fix, source_index.get(si)))) {
				newfrom.add(fixval);
				newto.add(fixval);
				++si;
			}
			else if (null != (fixval = getfixed(fix, ridx))) {
				int i = index.indexOf(ridx);
				if (Ops.cmp(fixval, from.get(i)) < 0
						|| Ops.cmp(fixval, to.get(i)) > 0)
					{
					// select doesn't match fixed so empty select
					sel.setNone();
					return ;
					}
				++ri;
			}
			else
				throw unreachable();
			}
		if (from.getRaw(from.size() - 1).equals(MAX_FIELD))
			newfrom.add(MAX_FIELD);
		if (to.getRaw(to.size() - 1).equals(MAX_FIELD))
			newto.add(MAX_FIELD);
		sel.set(newfrom.build(), newto.build());
	}

	private static Object getfixed(List<Fixed> fixed, String field) {
		for (Fixed f : fixed)
			if (f.field.equals(field) && f.values.size() == 1)
				return f.values.get(0);
		return null;
	}

	private static class Cmp implements Comparable<Cmp> {
		final String ident;
		final Token op;
		final ByteBuffer value;
		final List<ByteBuffer> values;

		Cmp(String ident, Token op, ByteBuffer value) {
			this.ident = ident;
			this.op = op;
			this.value = value;
			values = null;
		}

		/** IN */
		Cmp(String ident, Record rec) {
			this.ident = ident;
			op = null;
			value = null;
			values = new ArrayList<>();
			for (ByteBuffer buf : rec)
				values.add(buf);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof Cmp))
				return false;
			Cmp c = (Cmp) other;
			return Objects.equal(ident, c.ident) &&
					op == c.op &&
					Objects.equal(value, c.value);
			// what about values ?
		}

		@Override
		public int hashCode() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int compareTo(Cmp other) {
			return ident.compareTo(other.ident);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.addValue(ident)
					.addValue(op == null ? "in" : op.string)
					.addValue(valueToString(value))
					.addValue(valuesToString(values))
					.toString();
		}
	}

	// Iselect ------------------------------------------------------

	enum IselType { RANGE, VALUES };
	private static class Iselect {
		IselType type = IselType.RANGE;
		// range
		Point org = new Point();
		Point end = new Point();
		// set
		List<ByteBuffer> values = Collections.emptyList();

		Iselect() {
			org.x = MIN_FIELD;
			end.x = MAX_FIELD;
		}
		boolean matches(ByteBuffer value) {
			return type == IselType.RANGE ? inrange(value)
					: values.contains(value);
		}
		boolean inrange(ByteBuffer x) {
			org.x.rewind();
			end.x.rewind();
			int org_cmp = bufferUcompare(org.x, x);
			if (org_cmp > 0 || (org_cmp == 0 && org.d != 0))
				return false;
			int end_cmp = bufferUcompare(end.x, x);
			if (end_cmp < 0 || (end_cmp == 0 && end.d != 0))
				return false;
			return true;
		}
		void and_with(Iselect r) {
			if (type == IselType.RANGE && r.type == IselType.RANGE) {
				// both ranges
				if (r.org.compareTo(org) > 0)
					org = r.org;
				if (r.end.compareTo(end) < 0)
					end = r.end;
			} else if (type == IselType.VALUES && r.type == IselType.VALUES)
				// both sets
				values = intersect(values, r.values);
			else {
				// set and range
				if (type == IselType.VALUES) {
					org = r.org;
					end = r.end;
					r.values = values;
				}
				values = new ArrayList<>();
				for (ByteBuffer x : r.values)
					if (inrange(x))
						values.add(x);
				org.x = MAX_FIELD;
				end.x = MIN_FIELD; // empty range
				type = IselType.VALUES;
			}
		}
		boolean all() {
			return org.x.equals(MIN_FIELD) && org.d == 0 && end.x.equals(MAX_FIELD);
		}
		boolean none() {
			return org.compareTo(end) > 0 && values.size() == 0;
		}
		boolean one() {
			return org.equals(end) || values.size() == 1;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.addValue(type)
					.addValue(org + ".." + end)
					.addValue(valuesToString(values))
					.toString();
		}
	}
	private static class Point implements Comparable<Point> {
		ByteBuffer x;
		int d = 0;		// 0 if inclusive, +1 or -1 if exclusive

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof Point))
				return false;
			Point p = (Point) other;
			return d == p.d && Objects.equal(x, p.x);
		}

		@Override
		public int hashCode() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int compareTo(Point p) {
			int cmp = bufferUcompare(x, p.x);
			return cmp == 0 ? d - p.d : cmp;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.addValue(x == null ? "null" : valueToString(x))
					.addValue(d)
					.toString();
		}
	}

	private static String valueToString(ByteBuffer value) {
		if (value == null)
			return "";
		return value.equals(MIN_FIELD) ? "min"
				: value.equals(MAX_FIELD) ? "max"
				: toString(value);
	}

	public static String toString(ByteBuffer buf) {
		int pos = buf.position();
		Object x = Pack.unpack(buf);
		buf.position(pos);
		return x.toString();
	}

	private static String valuesToString(List<ByteBuffer> values) {
		if (values == null)
			return "";
		StringBuilder sb = new StringBuilder();
		for (ByteBuffer b : values)
			sb.append(" ").append(valueToString(b));
		return sb.toString();
	}

	@Override
	public void close() {
		if (tracing(SLOWQUERY) && n_in > 100 && n_in > 100 * n_out)
			trace(SLOWQUERY, n_in + "->" + n_out + "  " + this);
		super.close();
	}

}
