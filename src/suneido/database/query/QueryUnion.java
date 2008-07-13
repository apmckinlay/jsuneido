package suneido.database.query;

import java.util.List;

import suneido.database.Record;

public class QueryUnion extends QueryCompatible {
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


	QueryUnion(Query source1, Query source2) {
		super(source1, source2);
	}

	@Override
	public String toString() {
		String s = "(" + source + ") UNION";
		if (disjoint != null)
			s += "-DISJOINT (" + disjoint + ")";
		else if (strategy != null)
			s += (strategy == Strategy.MERGE ? "-MERGE" : "-LOOKUP");
		if (ki != null)
			s += "^" + ki;
		return s + " (" + source2 + ")";
	}

	@Override
	List<String> columns() {
		return allcols;
	}

	@Override
	Row get(Dir dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Header header() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<List<String>> indexes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<List<String>> keys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void rewind() {
		// TODO Auto-generated method stub

	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// TODO Auto-generated method stub

	}

	@Override
	double nrecords() {
		return (source.nrecords() + source2.nrecords()) / 2;
	}

}
