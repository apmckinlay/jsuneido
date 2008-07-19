package suneido.database.query;
import static suneido.Util.*;

import java.util.*;

import suneido.SuString;
import suneido.database.Record;
import suneido.database.query.expr.*;

public class Select extends Query1 {
	private And expr;
	private final boolean first = true; // used and then reset by optimize, then used again by next
	private final boolean rewound = true;
	private boolean newrange;
	private final boolean conflicting = false;
	boolean fixdone;
	List<Fixed> fix;
	private List<String> required_index;
	private final List<String> source_index = null; // may have extra stuff on
													// the end, or be
										// missing fields that are fixed
	List<List<String>> filter = null;

	public Select(Query source, Expr expr) {
		super(source);
		// expr = expr.fold();
		if (!(expr instanceof And))
			expr = new And().add(expr);
		// if (!source.columns().containsAll(expr.fields()))
		// throw new SuException("select: nonexistent columns: "
		// + listToParens(difference(expr.fields(), source.columns())));
		this.expr = (And) expr;
	}

	@Override
	public String toString() {
		String s = source + " WHERE";
		if (conflicting)
			return s + " nothing";
		if (! nil(source_index))
			s += "^" + source_index;
		if (! nil(filter))
			s += "%" + filter;
		if (! nil(expr.exprs))
			s += " " + expr;
		return s;
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
			r.source = (new_expr == expr ? this : new Select(source, new_expr));
			return r.transform();
		}
		// move select before extend, unless it depends on rules
		else if (source instanceof Extend) {
			Extend extend = (Extend) source;
			List<Expr> src1 = new ArrayList<Expr>();
			List<Expr> rest = new ArrayList<Expr>();
			for (Expr e : expr.exprs)
				if (nil(intersect(extend.rules, e.fields())))
					src1.add(e.replace(extend.flds, extend.exprs));
				else
					rest.add(e);
			if (!nil(src1))
				extend.source = new Select(extend.source, new And(src1));
			if (!nil(rest))
				expr = new And(rest);
			else
				moved = true;
		}
		// split select before & after summarize
		else if (source instanceof Summarize) {
			Summarize summarize = (Summarize) source;
			List<String> flds1 = summarize.source.columns();
			List<Expr> src1 = new ArrayList<Expr>();
			List<Expr> rest = new ArrayList<Expr>();
			for (Expr e : expr.exprs)
				if (flds1.containsAll(e.fields()))
					src1.add(e);
				else
					rest.add(e);
			if (!nil(src1))
				summarize.source = new Select(summarize.source, new And(src1));
			if (!nil(rest))
				expr = new And(rest);
			else
				moved = true;
		}
		// distribute select over intersect
		else if (source instanceof Intersect) {
			Intersect q = (Intersect) source;
			q.source = new Select(q.source, expr);
			q.source2 = new Select(q.source2, expr);
			moved = true;
		}
		// distribute select over difference
		else if (source instanceof Difference) {
			Difference q = (Difference) source;
			q.source = new Select(q.source, expr);
			q.source2 = new Select(q.source2, project(q.source2));
			moved = true;
		}
		// distribute select over union
		else if (source instanceof Union) {
			Union q = (Union) source;
			q.source = new Select(q.source, project(q.source));
			q.source2 = new Select(q.source2, project(q.source2));
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
			List<Expr> common = new ArrayList<Expr>();
			List<Expr> src1 = new ArrayList<Expr>();
			for (Expr e : expr.exprs)
				if (flds1.containsAll(e.fields()))
					src1.add(e);
				else
					common.add(e);
			if (!nil(src1))
				j.source = new Select(j.source, new And(src1));
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

	static final Expr empty_string = new Constant(SuString.EMPTY);

	private Expr project(Query q) {
		List<String> srcflds = q.columns();
		List<String> exprflds = expr.fields();
		List<String> missing = difference(exprflds, srcflds);
		return expr.replace(missing,
				Collections.nCopies(missing.size(), empty_string));
	}

	private boolean distribute(Query2 q2) {
		List<String> flds1 = q2.source.columns();
		List<String> flds2 = q2.source2.columns();
		List<Expr> common = new ArrayList<Expr>();
		List<Expr> src1 = new ArrayList<Expr>();
		List<Expr> src2 = new ArrayList<Expr>();
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
			q2.source = new Select(q2.source, new And(src1));
		if (! nil(src2))
			q2.source2 = new Select(q2.source2, new And(src2));
		if (! nil(common))
			expr = new And(common);
		else
			return true;
		return false;
	}

	@Override
	Row get(Dir dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void rewind() {
		// TODO Auto-generated method stub

	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// TODO Auto-generated method stub

	}

}
