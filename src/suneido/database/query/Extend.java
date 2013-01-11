package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.Suneido.dbpkg;
import static suneido.util.Util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import suneido.SuException;
import suneido.database.query.expr.Constant;
import suneido.database.query.expr.Expr;
import suneido.intfc.database.RecordBuilder;

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

	void init() {
		List<String> srccols = source.columns();

		if (!Collections.disjoint(srccols, flds))
			throw new SuException("extend: column(s) already exist: "
					+ intersect(srccols, flds));

		eflds = new ArrayList<String>();
		for (Expr e : exprs)
			addUnique(eflds, e.fields());

		Set<String> avail = setUnion(setUnion(srccols, rules), flds);
		if (!avail.containsAll(eflds))
			throw new SuException("extend: invalid column(s) in expressions: "
					+ setDifference(eflds, avail));
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
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (!nil(intersect(index, flds)))
			return IMPOSSIBLE;
		Set<String> extendfields = setUnion(flds, rules);
		// NOTE: optimize1 to bypass tempindex
		return source.optimize1(index,
				setDifference(setUnion(eflds, needs), extendfields),
				setDifference(firstneeds, extendfields), is_cursor, freeze);
	}

	@Override
	List<String> columns() {
		return union(source.columns(), union(flds, rules));
	}

	@Override
	int recordsize() {
		return source.recordsize() + flds.size() * columnsize();
	}

	// the changes below are so that the fields (used for output)
	// don't include the extended fields
	// since they should not be output
	// BUT had to undo these changes because they don't work client-server
	// since Header.schema and DbmsQueryRemote.parseHeader don't handle it

	@Override
	public Row get(Dir dir) {
		if (hdr == null)
			header();
		Row srcrow = source.get(dir);
		if (srcrow == null)
			return null;
		RecordBuilder rb = dbpkg.recordBuilder();
		for (int i = 0; i < flds.size(); ++i) {
//			Row row = new Row(srcrow, rb.build(), dbpkg.minRecord());
			Row row = new Row(srcrow, dbpkg.minRecord(), rb.build());
			rb.add(exprs.get(i).eval(hdr, row));
		}
//		return new Row(srcrow, rb.build(), dbpkg.minRecord());
		return new Row(srcrow, dbpkg.minRecord(), rb.build());
	}

	@Override
	public Header header() {
		if (hdr == null)
			hdr = new Header(source.header(),
//					new Header(asList(flds, noFields), union(flds, rules)));
					new Header(asList(noFields, flds), union(flds, rules)));
		return hdr;
	}

	@Override
	boolean singleDbTable() {
		return false;
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
