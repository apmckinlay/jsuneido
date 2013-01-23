/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

public abstract class Query1 extends Query {
	protected Query source;

	Query1(Query source) {
		this.source = source;
	}

	@Override
	Query transform() {
		source = source.transform();
		return this;
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		return source.optimize(index, needs, firstneeds, is_cursor, freeze);
	}

	@Override
	Query addindex(Transaction t) {
		source = source.addindex(t);
		return super.addindex(t);
	}

	@Override
	public void setTransaction(Transaction tran) {
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

	@Override
	List<String> columns() {
		return source.columns();
	}

	@Override
	public List<List<String>> keys() {
		return source.keys();
	}

	@Override
	List<List<String>> indexes() {
		return source.indexes();
	}

	@Override
	public Header header() {
		return source.header();
	}

	@Override
	boolean singleDbTable() {
		return source.singleDbTable();
	}

	@Override
	public void rewind() {
		source.rewind();
	}

	@Override
	public boolean updateable() {
		return source.updateable();
	}
	@Override
	public int tblnum() {
		return updateable() ? source.tblnum() : super.tblnum();
	}

	@Override
	public void output(Record r) {
		source.output(r);
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
	}

	/** holds an index and a cost, used for optimization */
	protected static class Best {
		double cost = IMPOSSIBLE;
		List<String> index = Collections.emptyList();

		Best update(List<String> index, double cost) {
			if (cost < this.cost) {
				this.cost = cost;
				this.index = index;
			}
			return this;
		}

		@Override
		public String toString() {
			return "Best " + index + " " + cost;
		}
	}

	/**
	 * @return The best index that supplies the required order by
	 * 		   taking fixed into account
	 */
	protected Best best_prefixed(List<List<String>> indexes, List<String> by,
			Set<String> needs, boolean is_cursor) {
		Best best = new Best();
		List<Fixed> fixed = source.fixed();
		for (List<String> ix : indexes)
			if (prefixed(ix, by, fixed))
				// NOTE: optimize1 to bypass tempindex
				best.update(ix,
						source.optimize1(ix, needs, noNeeds, is_cursor, false));
		return best;
	}

	/** @return Whether an index supplies an order, given what's fixed */
	static boolean prefixed(List<String> index, List<String> order,
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
		return o >= on;
	}

	/** @return Whether a field has a single fixed value */
	protected static boolean isfixed(List<Fixed> fixed, String field) {
		for (Fixed f : fixed)
			if (field.equals(f.field) && f.values.size() == 1)
				return true;
		return false;
	}

	@Override
	public void close() {
		source.close();
	}

}
