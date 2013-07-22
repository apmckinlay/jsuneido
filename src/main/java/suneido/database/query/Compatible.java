package suneido.database.query;

import static java.util.Collections.disjoint;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.union;

import java.util.List;

import suneido.intfc.database.Record;

public abstract class Compatible extends Query2 {
	protected List<String> ki;
	protected List<String> allcols;
	protected Header hdr1, hdr2;
	protected String disjoint;

	Compatible(Query source1, Query source2) {
		super(source1, source2);
		allcols = union(source.columns(), source2.columns());
		List<Fixed> fixed1 = source.fixed();
		List<Fixed> fixed2 = source2.fixed();
		for (Fixed f1 : fixed1)
			for (Fixed f2 : fixed2)
				if (f1.field.equals(f2.field) && disjoint(f1.values, f2.values)) {
					disjoint = f1.field;
					return ;
				}
		List<String> cols2 = source2.columns();
		for (Fixed f1 : fixed1)
			if (!cols2.contains(f1.field)
					&& !f1.values.contains("")) {
				disjoint = f1.field;
				return ;
			}
		List<String> cols1 = source.columns();
		for (Fixed f2 : fixed2)
			if (!cols1.contains(f2.field)
					&& !f2.values.contains("")) {
				disjoint = f2.field;
				return ;
			}
	}

	public String toString(String name) {
		return toString(name, "");
	}

	public String toString(String name, String strategy) {
		String s = "(" + source + " " + name;
		if (disjoint != null)
			s += "-DISJOINT(" + disjoint + ")";
		else
			s += strategy;
		if (ki != null)
			s += "^" + listToParens(ki);
		return s + " " + source2 + ")";
	}

	boolean isdup(Row row) {
		if (disjoint != null)
			return false;

		// test if row is in source2
		if (hdr1 == null) {
			hdr1 = source.header();
			hdr2 = source2.header();
		}
		Record key = row.project(hdr1, ki);
		source2.select(ki, key);
		Row row2 = source2.get(Dir.NEXT);
		if (row2 == null)
			return false;
		return equal(row, row2);
	}

	boolean equal(Row r1, Row r2) {
		if (disjoint != null)
			return false;

		for (String col : allcols)
			if (!r1.getraw(hdr1, col).equals(r2.getraw(hdr2, col)))
				return false;
		return true;
	}

	@Override
	public void rewind() {
		source.rewind();
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
	}

}
