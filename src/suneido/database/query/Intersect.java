package suneido.database.query;

import static suneido.Util.intersect;
import static suneido.Util.union;

import java.util.Collections;
import java.util.List;

public class Intersect extends Compatible {
	Intersect(Query source1, Query source2) {
		super(source1, source2);
	}

	@Override
	public String toString() {
		return toString("INTERSECT");
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (disjoint != null)
			return 0;
		List<String> cols1 = source.columns();
		List<String> needs1 = intersect(needs, cols1);
		List<String> cols2 = source2.columns();
		List<String> needs2 = intersect(needs, cols2);

		List<String> ki2 = source2.key_index(needs2);
		List<String> needs1_k = intersect(cols1, ki2);
		double cost1 = source.optimize(index, needs1, needs1_k, is_cursor, false)
				+ source2.optimize(ki2, needs2, noFields, is_cursor, false);

		List<String> ki1 = source.key_index(needs1);
		List<String> needs2_k = intersect(cols2, ki1);
		double cost2 = source2.optimize(index, needs2, needs2_k, is_cursor, false)
				+ source.optimize(ki1, needs1, noFields, is_cursor, false)
				+ OUT_OF_ORDER;

		double cost = Math.min(cost1, cost2);
		if (cost >= IMPOSSIBLE)
			return IMPOSSIBLE;
		if (freeze) {
			if (cost2 < cost1) {
				Query t1 = source; source = source2; source2 = t1;
				List<String> t2 = needs1; needs1 = needs2; needs2 = t2;
				t2 = needs1_k; needs1_k = needs2_k; needs2_k = t2;
				t2 = ki1; ki1 = ki2; ki2 = t2;
			}
			ki = ki2;
			source.optimize(index, needs1, needs1_k, is_cursor, true);
			source2.optimize(ki, needs2, noFields, is_cursor, true);
		}
		return cost;
	}

	@Override
	List<String> columns() {
		return intersect(source.columns(), source2.columns());
	}

	@Override
	List<List<String>> indexes() {
		return union(source.indexes(), source2.indexes());
	}

	@Override
	List<List<String>> keys() {
		List<List<String>> k = intersect(source.keys(), source2.keys());
		return k == null ? Collections.singletonList(columns()) : k;
	}

	@Override
	double nrecords() {
		return Math.min(source.nrecords(), source2.nrecords()) / 2;
	}

	@Override
	Row get(Dir dir) {
		// TODO get
		return null;
	}

}
