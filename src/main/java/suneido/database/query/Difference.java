/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Util.setIntersect;

import java.util.List;
import java.util.Set;

public class Difference extends Compatible {
	Difference(Query source1, Query source2) {
		super(source1, source2);
	}

	@Override
	public String toString() {
		return toString("MINUS");
	}

	@Override
	Query transform() {
		// remove disjoint difference
		return disjoint == null ? super.transform() : source.transform();
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (disjoint != null)
			return 0;
		List<String> cols1 = source.columns();
		List<String> cols2 = source2.columns();
		Set<String> needs1 = setIntersect(needs, cols1);
		Set<String> needs2 = setIntersect(needs, cols2);
		ki = source2.key_index(needs2);
		Set<String> needs1_k = setIntersect(cols1, ki);
		return source.optimize(index, needs1, needs1_k, is_cursor, freeze)
				+ source2.optimize(ki, needs2, noNeeds, is_cursor, freeze);
	}

	@Override
	double nrecords() {
		double n1 = source.nrecords();
		return (Math.max(0.0, n1 - source2.nrecords()) + n1) / 2;
	}

	@Override
	public Header header() {
		return source.header();
	}

	@Override
	public Row get(Dir dir) {
		Row row;
		while (null != (row = source.get(dir)) && isdup(row))
			;
		return row;
	}

}
