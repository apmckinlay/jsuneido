package suneido.database.query;

import static suneido.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.database.Record;

public class Union extends Compatible {
	Strategy strategy;
	boolean first = true;
	Row empty1;
	Row empty2;
	Keyrange sel;
	// for LOOKUP
	boolean in1; // true while processing first source
	// for MERGE
	boolean rewound = true;
	boolean src1;
	boolean src2;
	Row row1;
	Row row2;
	Record key1;
	Record key2;
	Record curkey;
	boolean fixdone = false;
	List<Fixed> fix;
	enum Strategy {
		NONE, MERGE, LOOKUP
	};


	Union(Query source1, Query source2) {
		super(source1, source2);
	}

	@Override
	public String toString() {
		return toString("UNION", strategy == null ? ""
				: strategy == Strategy.MERGE ? "-MERGE" : "-LOOKUP");
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		List<String> cols1 = source.columns();
		List<String> needs1 = intersect(needs, cols1);
		List<String> cols2 = source2.columns();
		List<String> needs2 = intersect(needs, cols2);
		if (!nil(index)) {
			// if not disjoint then index must also be a key
			if (disjoint == null &&
				(! source.keys().contains(index) || ! source2.keys().contains(index)))
				return IMPOSSIBLE;
			if (freeze) {
				ki = index;
				strategy = Strategy.MERGE;
			}
			return source.optimize(index, needs1, noFields, is_cursor, freeze) +
				source2.optimize(index, needs2, noFields, is_cursor, freeze);
		} else if (disjoint != null) {
			if (freeze)
				strategy = Strategy.LOOKUP;
			return source.optimize(noFields, needs1, noFields, is_cursor, freeze) +
				source2.optimize(noFields, needs2, noFields, is_cursor, freeze);
		} else {
			// merge if you can read both sources by common key index
			List<List<String>> keyidxs = intersect(
				intersect(source.keys(), source.indexes()),
				intersect(source2.keys(), source2.indexes()));
			List<String> merge_key = null;
			double merge_cost = IMPOSSIBLE;
			for (List<String> k : keyidxs) {
				// NOTE: optimize1 to avoid tempindex
				double cost = source.optimize1(k, needs1, noFields, is_cursor, false) +
					source2.optimize1(k, needs2, noFields, is_cursor, false);
				if (cost < merge_cost) {
					merge_key = k;
					merge_cost = cost;
				}
			}
			// lookup on source2
			List<String> ki2 = null;
			double cost1 = IMPOSSIBLE;
			for (List<String> k : source2.keys()) {
				List<String> needs1_k = union(needs1, intersect(cols1, k));
				double cost =
					2 * source.optimize(noFields, needs1, needs1_k, is_cursor, false) +
					source2.optimize(k, needs2, noFields, is_cursor, false);
				if (cost < cost1) {
					ki2 = k;
					cost1 = cost;
				}
			}
			// lookup on source1
			List<String> ki1 = null;
			double cost2 = IMPOSSIBLE;
			for (List<String> k : source.keys()) {
				List<String> needs2_k = union(needs2, intersect(cols2, k));
				double cost =
					2 * source2.optimize(noFields, needs2, needs2_k, is_cursor, false) +
					source.optimize(k, needs1, noFields, is_cursor, false) + OUT_OF_ORDER;
				if (cost < cost2) {
					ki1 = k;
					cost2 = cost;
				}
			}

			double cost = Math.min(merge_cost, Math.min(cost1, cost2));
			if (cost >= IMPOSSIBLE)
				return IMPOSSIBLE;
			if (freeze) {
				if (merge_cost <= cost1 && merge_cost <= cost2) {
					strategy = Strategy.MERGE;
					ki = merge_key;
					// NOTE: optimize1 to bypass tempindex
					source.optimize1(ki, needs1, noFields, is_cursor, true);
					source2.optimize1(ki, needs2, noFields, is_cursor, true);
				} else {
					strategy = Strategy.LOOKUP;
					if (cost2 < cost1) {
						Query t1 = source; source = source2; source2 = t1;
						List<String> t2 = needs1; needs1 = needs2; needs2 = t2;
						t2 = ki1; ki1 = ki2; ki2 = t2;
						t2 = cols1; cols1 = cols2; cols2 = t2;
					}
					ki = ki2;
					List<String> needs1_k = union(needs1, intersect(cols1, ki));
					// NOTE: optimize1 to bypass tempindex
					source.optimize1(noFields, needs1, needs1_k, is_cursor, true);
					source2.optimize(ki, needs2, noFields, is_cursor, true);
				}
			}
			return cost;
		}
	}

	@Override
	List<String> columns() {
		return allcols;
	}

	@Override
	Row get(Dir dir) {
		// TODO get
		return null;
	}

	@Override
	Header header() {
		return Header.add(source.header(), source2.header());
	}

	@Override
	List<List<String>> indexes() {
		// TODO: there are more possible indexes
		return intersect(
			intersect(source.keys(), source.indexes()),
			intersect(source2.keys(), source2.indexes()));
	}

	@Override
	List<List<String>> keys() {
		if (disjoint != null) {
			List<List<String>> kin = intersect_prefix(source.keys(), source2.keys());
			if (!nil(kin)) {
				List<List<String>> kout = new ArrayList<List<String>>();
				for (List<String> k : kin)
					kout.add(k.contains(disjoint) ? k : concat(k,
							list(disjoint)));
				return kout;
			}
		}
		return list(source.columns());
	}

	private List<List<String>> intersect_prefix(List<List<String>> keys1,
			List<List<String>> keys2) {
		List<List<String>> kout = new ArrayList<List<String>>();
		for (List<String> k1 : keys1)
			for (List<String> k2 : keys2)
				if (prefix(k1, k2))
					addUnique(kout, k1);
				else if (prefix(k2, k1))
					addUnique(kout, k2);
		return kout;
	}

	@Override
	void rewind() {
		// TODO rewind

	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// TODO select

	}

	@Override
	double nrecords() {
		return (source.nrecords() + source2.nrecords()) / 2;
	}

}
