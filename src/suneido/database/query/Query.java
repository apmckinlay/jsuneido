package suneido.database.query;

import static suneido.Suneido.verify;
import static suneido.Util.union;

import java.util.Collections;
import java.util.List;

import suneido.SuException;
import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.expr.Expr;

/**
 * Base class for query operation classes.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class Query {
	private final Cache cache = new Cache();
	private boolean willneed_tempindex;
	private List<String> tempindex;
	protected enum Dir {
		NEXT, PREV
	};
	protected final static List<String> noFields = Collections.emptyList();
	// cost of writing index relative to reading data
	private final static int WRITE_FACTOR = 4;
	// minimal penalty for changing order of operations
	private final static int OUT_OF_ORDER = 10;
	// allow for adding impossibles together
	protected final static double IMPOSSIBLE = Double.MAX_VALUE / 10;

	static Query query(String s, boolean is_cursor) {
		return query_setup(ParseQuery.parse(s), is_cursor);
	}

	static Query query_setup(Query q, boolean is_cursor) {
		q = q.transform();
		if (q.optimize(noFields, q.columns(), noFields, is_cursor, true) >= IMPOSSIBLE)
			throw new SuException("invalid query");
		q = q.addindex();
		return q;
	}

	static int update(Transaction tran, Query qq, List<String> c, List<Expr> e) {
		return 0; // TODO
	}

	abstract void setTransaction(Transaction tran);

	// iteration
	abstract Header header();
	abstract List<List<String>> indexes();
	List<String> ordering() { // overridden by QSort
		return noFields;
	}
	abstract void select(List<String> index, Record from, Record to);
	void select(List<String> index, Record key) {
	}
	abstract void rewind();
	abstract Row get(Dir dir);
	List<Fixed> fixed() {
		return Collections.EMPTY_LIST;
	}

	// updating
	boolean updateable() {
		return false;
	}
	boolean output(Record record) {
		return false;
	}

	@Override
	public abstract String toString();

	abstract List<String> columns();

	abstract List<List<String>> keys();

	Query transform() {
		return this;
	}
	double optimize(List<String> index, List<String> needs, List<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (is_cursor || index.isEmpty())
			return optimize1(index, needs, firstneeds, is_cursor, freeze);
		if (!columns().containsAll(index))
			return IMPOSSIBLE;

		// use existing index
		double cost1 = optimize1(index, needs, firstneeds, is_cursor, false);

		// tempindex
		double cost2 = IMPOSSIBLE;
		int keysize = index.size() * columnsize() * 2; // *2 for index overhead
		cost2 = optimize1(noFields, needs, firstneeds.isEmpty() ? firstneeds
				: union(firstneeds, index), is_cursor, false)
				+ nrecords() * keysize * WRITE_FACTOR // write index
				+ nrecords() * keysize // read index
				+ 4000; // minimum fixed cost
		verify(cost2 >= 0);

		double cost = Math.min(cost1, cost2);
		willneed_tempindex = (cost2 < cost1);
		if (!freeze)
			return cost;

		if (cost >= IMPOSSIBLE)
			cost = IMPOSSIBLE;
		else if (cost1 <= cost2)
			optimize1(index, needs, firstneeds, is_cursor, true);
		else // cost2 < cost1
		{
			tempindex = index;
			optimize1(noFields, needs, index, is_cursor, true);
		}
		return cost;
	}

	// caching
	double optimize1(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		double cost;
		if (!freeze && 0 <= (cost = cache.get(index, needs, firstneeds)))
			return cost;

		cost = optimize2(index, needs, firstneeds, is_cursor, freeze);
		verify(cost >= 0);

		if (!freeze)
			cache.add(index, needs, firstneeds, cost);
		return cost;
	}

	abstract double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze);

	protected List<String> key_index(List<String> needs) {
		List<String> best_index = null;
		double best_cost = IMPOSSIBLE;
		for (List<String> key : keys()) {
			double cost = optimize(key, needs, noFields, false, false);
			if (cost < best_cost) {
				best_cost = cost;
				best_index = key;
			}
		}
		return best_index;
	}

	// estimated result sizes
	abstract double nrecords();
	abstract int recordsize();
	abstract int columnsize();

	// used to insert TempIndex nodes
	Query addindex() { // redefined by Query1 and Query2
		if (tempindex.isEmpty())
			return this;
		if (header().size() > 2)
			return new TempIndexN(this, tempindex, isUnique(tempindex));
		else
			return new TempIndex1(this, tempindex, isUnique(tempindex));
	}

	private boolean isUnique(List<String> tempindex) {
		for (List<String> k : keys())
			if (tempindex.containsAll(k))
				return true;
		return false;
	}
}
