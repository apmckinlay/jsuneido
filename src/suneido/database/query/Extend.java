package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.util.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.database.Record;
import suneido.database.query.expr.Constant;
import suneido.database.query.expr.Expr;

public class Extend extends Query1 {
	List<String> rules;
	List<String> flds; // modified by Project.transform
	List<Expr> exprs; // modified by Project.transform
	private List<String> eflds;
	private Header hdr = null;
	private List<Fixed> fix;

	Extend(Query source, List<String> flds, List<Expr> exprs,
			List<String> rules) {
		super(source);
		this.flds = flds;
		this.exprs = exprs;
		this.rules = rules;
		init();
	}

	private void init() {
		List<String> srccols = source.columns();

		List<String> dups = intersect(srccols, flds);
		if (!dups.isEmpty())
			throw new SuException("extend: column(s) already exist: " + dups);

		eflds = new ArrayList<String>();
		for (Expr e : exprs)
			addUnique(eflds, e.fields());

		List<String> avail = union(union(srccols, rules), flds);
		List<String> invalid = difference(eflds, avail);
		if (!invalid.isEmpty())
			throw new SuException("extend: invalid column(s) in expressions: "
					+ invalid);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(source.toString());
		sb.append(" EXTEND ");
		String sep = "";
		for (String f : rules) {
			sb.append(sep).append(f);
			sep = ", ";
		}
		for (int i = 0; i < flds.size(); ++i) {
			sb.append(sep).append(flds.get(i)).append(" = ").append(exprs.get(i));
			sep = ", ";
		}
		return sb.toString();
	}

	@Override
	Query transform() {
		// remove empty Extends
		if (nil(flds) && nil(rules))
			return source.transform();
		// combine Extends
		if (source instanceof Extend) {
			Extend e = (Extend) source;
			flds = concat(e.flds, flds);
			exprs = concat(e.exprs, exprs);
			rules = union(e.rules, rules);
			source = e.source;
			init();
			return transform();
			}
		return super.transform();
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (!nil(intersect(index, flds)))
			return IMPOSSIBLE;
		List<String> extendfields = union(flds, rules);
		// NOTE: optimize1 to bypass tempindex
		return source.optimize1(index,
			union(difference(eflds, extendfields), difference(needs, extendfields)),
			difference(firstneeds, extendfields), is_cursor, freeze);
	}

	@Override
	List<String> columns() {
		return union(source.columns(), union(flds, rules));
	}

	@Override
	int recordsize() {
		return source.recordsize() + flds.size() * columnsize();
	}

	@Override
	public Row get(Dir dir) {
		if (hdr == null)
			header();
		Row row = source.get(dir);
		if (row == null)
			return null;
		Record results = new Record();
		row = new Row(row, Record.MINREC, results);
		for (int i = 0; i < flds.size(); ++i)
			results.add(exprs.get(i).eval(hdr, row));
		return row;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Header header() {
		if (hdr == null)
			hdr = new Header(source.header(),
					new Header(asList(noFields, flds), union(flds, rules)));
		return hdr;
	}

	@Override
	List<Fixed> fixed() {
		if (fix != null)
			return fix;
		fix = new ArrayList<Fixed>();
		for (int i = 0; i < flds.size(); ++i)
			if (exprs.get(i) instanceof Constant)
				fix.add(new Fixed(flds.get(i), ((Constant) exprs.get(i)).value));
		fix = Fixed.combine(fix, source.fixed());
		return fix;
	}

}
