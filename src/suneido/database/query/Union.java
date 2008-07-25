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
		String s = "(" + source + " UNION";
		if (disjoint != null)
			s += "-DISJOINT (" + disjoint + ")";
		else if (strategy != null)
			s += (strategy == Strategy.MERGE ? "-MERGE" : "-LOOKUP");
		if (ki != null)
			s += "^" + ki;
		return s + " " + source2 + ")";
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
		// TODO Auto-generated method stub
		return null;
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
		if (disjoint != "") {
			List<List<String>> kin = intersect_prefix(source.keys(), source2
					.keys());
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
