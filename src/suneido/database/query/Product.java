package suneido.database.query;

import static suneido.SuException.verify;
import static suneido.util.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.database.Record;

public class Product extends Query2 {
	private boolean rewound = true;
	private Row row1 = null;

	Product(Query source1, Query source2) {
		super(source1, source2);
		List<String> dups = intersect(source1.columns(), source2.columns());
		if (!dups.isEmpty())
			throw new SuException("product: common columns not allowed: " + dups);
	}

	@Override
	public String toString() {
		return "(" + source + " TIMES " + source2 + ")";
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		List<String> needs1 = intersect(source.columns(), needs);
		List<String> needs2 = intersect(source2.columns(), needs);
		verify(union(needs1, needs2).size() == needs.size());
		List<String> firstneeds1 = intersect(source.columns(), needs);
		List<String> firstneeds2 = intersect(source2.columns(), needs);
		if (! nil(firstneeds1) && ! nil(firstneeds2))
			firstneeds1 = firstneeds2 = noFields;

		double cost1 = source.optimize(index, needs1, firstneeds1, is_cursor, false) +
			source2.optimize(noFields, needs2, noFields, is_cursor, false);
		double cost2 = source2.optimize(index, needs2, firstneeds2, is_cursor, false) +
			source.optimize(noFields, needs1, noFields, is_cursor, false) + OUT_OF_ORDER;
		double cost = Math.min(cost1, cost2);
		if (cost >= IMPOSSIBLE)
			return IMPOSSIBLE;
		if (! freeze)
			return cost;

		if (cost2 < cost1) {
			Query t1 = source; source = source2; source2 = t1;
			List<String> t2 = needs1; needs1 = needs2; needs2 = t2;
			t2 = firstneeds1; firstneeds1 = firstneeds2; firstneeds2 = t2;
		}
		source.optimize(index, needs1, firstneeds1, is_cursor, true);
		source2.optimize(noFields, needs2, noFields, is_cursor, true);
		return cost;
	}

	@Override
	List<String> columns() {
		return union(source.columns(), source2.columns());
	}

	@Override
	List<List<String>> indexes() {
		return union(source.indexes(), source2.indexes());
	}

	@Override
	public List<List<String>> keys() {
		// keys are all pairs of source keys
		// there are no columns in common so no keys in common
		// so there won't be any duplicates in the result
		List<List<String>> k = new ArrayList<List<String>>();
		for (List<String> k1 : source.keys())
			for (List<String> k2 : source2.keys())
				addUnique(k, union(k1, k2));
		verify(!nil(k));
		return k;
	}

	@Override
	public void rewind() {
		rewound = true;
		source.rewind();
		source2.rewind();
	}

	@Override
	public Row get(Dir dir) {
		Row row2 = source2.get(dir);
		if (rewound) {
			rewound = false;
			row1 = source.get(dir);
			if (row1 == null || row2 == null)
				return null;
		}
		if (row2 == null) {
			row1 = source.get(dir);
			if (row1 == null)
				return null;
			source2.rewind();
			row2 = source2.get(dir);
		}
		return new Row(row1, row2);
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		rewound = true;
		source.select(index, from, to);
		source2.rewind();
	}
}
