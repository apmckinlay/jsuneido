package suneido.database.query;

import static suneido.Util.intersect;
import static suneido.Util.intersect2;
import static suneido.Util.union2;

import java.util.Collections;
import java.util.List;

public class Intersect extends QueryCompatible {
	Intersect(Query source1, Query source2) {
		super(source1, source2);
	}

	@Override
	public String toString() {
		String s = "(" + source + ") INTERSECT";
		if (disjoint != null)
			s += "-DISJOINT";
		if (ki != null)
			s += "^" + ki;
		return s + " (" + source2 + ")";
	}

	@Override
	List<String> columns() {
		return intersect(source.columns(), source2.columns());
	}

	@Override
	Row get(Dir dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<List<String>> indexes() {
		return union2(source.indexes(), source2.indexes());
	}

	@Override
	List<List<String>> keys() {
		List<List<String>> k = intersect2(source.keys(), source2.keys());
		return k == null ? Collections.singletonList(columns()) : k;
	}

	@Override
	double nrecords() {
		return Math.min(source.nrecords(), source2.nrecords()) / 2;
	}

}
