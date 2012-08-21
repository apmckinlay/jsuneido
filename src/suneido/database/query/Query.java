/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.Suneido.dbpkg;
import static suneido.util.Util.nil;
import static suneido.util.Util.setUnion;
import static suneido.util.Verify.verify;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import suneido.SuException;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.intfc.database.Transaction;

import com.google.common.collect.ImmutableSet;

/**
 * Base class for query operation classes.
 */
public abstract class Query {
	private final Cache cache = new Cache();
	private List<String> tempindex;
	public enum Dir { NEXT, PREV };
	protected static final List<String> noFields = Collections.emptyList();
	protected static final Set<String> noNeeds = Collections.emptySet();
	// cost of writing index relative to reading data
	protected static final int WRITE_FACTOR = 4;
	// minimal penalty for changing order of operations
	protected static final int OUT_OF_ORDER = 10;
	// allow for adding impossibles together
	protected static final double IMPOSSIBLE = Double.MAX_VALUE / 10;

	Query setup(Transaction t) {
		return setup(false, t);
	}

	Query setup(boolean is_cursor, Transaction t) {
		Query q = transform();
		if (q.optimize(noFields, ImmutableSet.copyOf(q.columns()), noNeeds,
				is_cursor, true) >= IMPOSSIBLE)
			throw new SuException("invalid query " + q);
		q = q.addindex(t);
		return q;
	}

	public abstract void setTransaction(Transaction tran);

	// iteration
	public abstract Header header();
	public List<String> ordering() { // overridden by QSort
		return noFields;
	}
	abstract void select(List<String> index, Record from, Record to);
	void select(List<String> index, Record key) {
		RecordBuilder rb = dbpkg.recordBuilder();
		Record key_to = rb.addAll(key).addMax().build();
		select(index, key, key_to);
	}
	public abstract void rewind();

	public abstract Row get(Dir dir);

	/** Originate from {@link Select} and {@link Extend} */
	List<Fixed> fixed() {
		return Collections.emptyList();
	}

	// updating
	public boolean updateable() {
		return false;
	}
	public int tblnum() {
		throw new UnsupportedOperationException();
	}
	public void output(Record record) {
		throw new SuException("can't output to this query");
	}

	@Override
	public abstract String toString();

	abstract List<String> columns();

	public abstract List<List<String>> keys();

	abstract List<List<String>> indexes();

	Query transform() {
		return this;
	}
	double optimize(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		//System.out.println("\noptimize START " + this);
		//System.out.println("    index=" + index
		//		+ " needs=" + needs	+ " firstneeds=" + firstneeds
		//		+ (is_cursor ? " CURSOR" : "")
		//		+ (freeze ? " FREEZE" : ""));
		if (is_cursor || nil(index)) {
			double cost = optimize1(index, needs, firstneeds, is_cursor, freeze);
			//System.out.println("optimize END " + this);
			//System.out.println("\tcost " + cost);
			return cost;
		}
		if (!columns().containsAll(index))
			return IMPOSSIBLE;

		// use existing index
		double cost1 = optimize1(index, needs, firstneeds, is_cursor, false);

		// tempindex
		double cost2 = IMPOSSIBLE;
		int keysize = index.size() * columnsize() * 2; // *2 for index overhead
		cost2 = optimize1(noFields, needs,
				nil(firstneeds) ? firstneeds : setUnion(firstneeds, index),
				is_cursor, false)
				+ nrecords() * keysize * WRITE_FACTOR // write index
				+ nrecords() * keysize // read index
				+ 4000; // minimum fixed cost
		verify(cost2 >= 0);
		//System.out.println("optimize END " + this);
		//System.out.println("\twith " + index + " cost1 " + cost1);
		//System.out.println("\twith tempindex cost2 " + cost2
		//	+ " nrecords " + nrecords() + " keysize " + keysize);

		double cost = Math.min(cost1, cost2);
		if (!freeze)
			return cost;

		if (cost >= IMPOSSIBLE)
			cost = IMPOSSIBLE;
		else if (cost1 <= cost2)
			optimize1(index, needs, firstneeds, is_cursor, true);
		else { // cost2 < cost1
			tempindex = index;
			optimize1(noFields, needs, ImmutableSet.copyOf(index), is_cursor, true);
		}
		return cost;
	}

	// caching
	double optimize1(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		double cost;
		if (!freeze && 0 <= (cost = cache.get(index, needs, firstneeds, is_cursor)))
			return cost;

		cost = optimize2(index, needs, firstneeds, is_cursor, freeze);
		verify(cost >= 0);

		if (!freeze)
			cache.add(index, needs, firstneeds, is_cursor, cost);
		return cost;
	}

	abstract double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze);

	protected List<String> key_index(Set<String> needs) {
		List<String> best_index = Collections.emptyList();
		double best_cost = IMPOSSIBLE;
		for (List<String> key : keys()) {
			double cost = optimize(key, needs, noNeeds, false, false);
			if (cost < best_cost) {
				best_cost = cost;
				best_index = key;
			}
		}
		return best_index;
	}

	/** @return The estimated number of records resulting from this query */
	abstract double nrecords();

	/** @return The estimated average size of a record in this query */
	abstract int recordsize();

	/** @return The estimated average size of the data for a column */
	abstract int columnsize();

	// used to insert TempIndex nodes
	Query addindex(Transaction t) { // redefined by Query1 and Query2
		return nil(tempindex) ? this
				: new TempIndex(this, t, tempindex, isUnique(tempindex));
	}

	private boolean isUnique(List<String> tempindex) {
		for (List<String> k : keys())
			if (tempindex.containsAll(k))
				return true;
		return false;
	}

	/** Used by TempIndex to optimize sorting a single database table */
	abstract boolean singleDbTable();

	/** used for trace, e.g. slow queries in Select */
	public abstract void close();

}
