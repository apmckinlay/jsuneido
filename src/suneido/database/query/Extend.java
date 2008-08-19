package suneido.database.query;

import static suneido.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.database.Record;
import suneido.database.query.expr.Expr;

public class Extend extends Query1 {
	List<String> rules;
	List<String> flds; // modified by Project.transform
	List<Expr> exprs; // modified by Project.transform
	private List<String> eflds;
	private Header hdr = null;

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
		String s = source + " EXTEND ";
		String sep = "";
		for (String f : rules) {
			s += sep + f;
			sep = ", ";
		}
		for (int i = 0; i < flds.size(); ++i) {
			s += sep + flds.get(i) + " = " + exprs.get(i);
			sep = ", ";
		}
		return s;
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
		row = new Row(row, null, results);
		for (int i = 0; i < flds.size(); ++i)
			results.add(exprs.get(i).eval(hdr, row));
		return row;
	}

	@Override
	public Header header() {
		if (hdr == null)
			hdr = new Header(source.header(),
					new Header(list(noFields, flds), union(flds, rules)));
		return hdr;
	}

}
