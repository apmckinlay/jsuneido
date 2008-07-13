package suneido.database.query;

import java.util.List;

import suneido.database.Transaction;

public abstract class Query1 extends Query {
	protected Query source;

	Query1(Query source) {
		this.source = source;
	}

	@Override
	boolean updateable() {
		return source.updateable();
	}

	@Override
	Query transform() { // also defined by Query2
		source = source.transform();
		return this;
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		return source.optimize(index, needs, firstneeds, is_cursor, freeze);
	}

	@Override
	Query addindex() {
		source = source.addindex();
		return super.addindex();
	}

	@Override
	void setTransaction(Transaction tran) {
		source.setTransaction(tran);
	}

	// estimated result sizes
	@Override
	double nrecords() {
		return source.nrecords();
	}

	@Override
	int recordsize() {
		return source.recordsize();
	}

	@Override
	int columnsize() {
		return source.columnsize();
	}

	@Override
	List<Fixed> fixed() {
		return source.fixed();
	}

	void best_prefixed(List<List<String>> indexes, List<String> by,
			List<String> needs, boolean is_cursor, List<String> best_index,
			double best_cost) {
		List<Fixed> fixed = source.fixed();
		for (List<String> ix : indexes)
			if (prefixed(ix, by, fixed)) {
				// NOTE: optimize1 to bypass tempindex
				double cost = source.optimize1(ix, needs, noFields, is_cursor,
						false);
				if (cost < best_cost) {
					best_cost = cost;
					best_index = ix;
				}
			}
	}
	private static boolean prefixed(List<String> index, List<String> order,
			List<Fixed> fixed) {
		int i = 0, o = 0;
		int in = index.size(), on = order.size();
		while (i < in && o < on) {
			if (index.get(i).equals(order.get(o))) {
				++o;
				++i;
			} else if (isfixed(fixed, index.get(i)))
				++i;
			else if (isfixed(fixed, order.get(o)))
				++o;
			else
				return false;
		}
		while (o < on && isfixed(fixed, order.get(o)))
			++o;
		return o < on;
	}
	private static boolean isfixed(List<Fixed> fixed, String field) {
		for (Fixed f : fixed)
			if (f.field == field && f.values.size() == 1)
				return true;
		return false;
	}

}
