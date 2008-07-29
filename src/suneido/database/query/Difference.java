package suneido.database.query;

import static suneido.Util.intersect;

import java.util.List;

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
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (disjoint != null)
			return 0;
		List<String> cols1 = source.columns();
		List<String> cols2 = source2.columns();
		List<String> needs1 = intersect(needs, cols1);
		List<String> needs2 = intersect(needs, cols2);
		ki = source2.key_index(needs2);
		List<String> needs1_k = intersect(cols1, ki);
		return source.optimize(index, needs1, needs1_k, is_cursor, freeze)
				+ source2.optimize(ki, needs2, noFields, is_cursor, freeze);
	}

	@Override
	double nrecords() {
		double n1 = source.nrecords();
		return (Math.max(0.0, n1 - source2.nrecords()) + n1) / 2;
	}

	@Override
	Row get(Dir dir) {
		// TODO get
		return null;
	}

}
