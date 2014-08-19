/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Util.intersect;
import static suneido.util.Util.setIntersect;
import static suneido.util.Util.union;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Intersect extends Compatible {
	Intersect(Query source1, Query source2) {
		super(source1, source2);
	}

	@Override
	public String toString() {
		return toString("INTERSECT");
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (disjoint != null)
			return 0;
		List<String> cols1 = source.columns();
		Set<String> needs1 = setIntersect(needs, cols1);
		List<String> cols2 = source2.columns();
		Set<String> needs2 = setIntersect(needs, cols2);

		List<String> ki2 = source2.key_index(needs2);
		Set<String> needs1_k = setIntersect(cols1, ki2);
		double cost1 = source.optimize(index, needs1, needs1_k, is_cursor, false)
				+ source2.optimize(ki2, needs2, noNeeds, is_cursor, false);

		List<String> ki1 = source.key_index(needs1);
		Set<String> needs2_k = setIntersect(cols2, ki1);
		double cost2 = source2.optimize(index, needs2, needs2_k, is_cursor, false)
				+ source.optimize(ki1, needs1, noNeeds, is_cursor, false)
				+ OUT_OF_ORDER;

		double cost = Math.min(cost1, cost2);
		if (cost >= IMPOSSIBLE)
			return IMPOSSIBLE;
		if (freeze) {
			if (cost2 < cost1) {
				Query t1 = source; source = source2; source2 = t1;
				Set<String> t2 = needs1; needs1 = needs2; needs2 = t2;
				t2 = needs1_k; needs1_k = needs2_k; needs2_k = t2;
				List<String> t3 = ki1; ki1 = ki2; ki2 = t3;
			}
			ki = ki2;
			source.optimize(index, needs1, needs1_k, is_cursor, true);
			source2.optimize(ki, needs2, noNeeds, is_cursor, true);
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
	public List<List<String>> keys() {
		List<List<String>> k = intersect(source.keys(), source2.keys());
		return k == null ? Collections.singletonList(columns()) : k;
	}

	@Override
	double nrecords() {
		return Math.min(source.nrecords(), source2.nrecords()) / 2;
	}

	@Override
	public Header header() {
		return source.header();
	}

	@Override
	public Row get(Dir dir) {
		if (disjoint != null)
			return null;
		Row row;
		while (null != (row = source.get(dir)) && !isdup(row))
			;
		return row;
	}

}
