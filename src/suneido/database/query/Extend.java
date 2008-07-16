package suneido.database.query;

import static suneido.Util.addUnique;
import static suneido.Util.difference;
import static suneido.Util.intersect;
import static suneido.Util.union;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.database.Record;
import suneido.database.query.expr.Expr;

public class Extend extends Query1 {
	private final List<String> rules;
	List<String> flds; // modified by Project.transform
	List<Expr> exprs; // modified by Project.transform
	private List<String> eflds;

	Extend(Query source, List<String> flds, List<Expr> exprs,
			List<String> rules) {
		super(source);
		this.flds = flds;
		this.exprs = exprs;
		this.rules = rules;
//		init();
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
	List<String> columns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Row get(Dir dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Header header() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<List<String>> indexes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<List<String>> keys() {
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
