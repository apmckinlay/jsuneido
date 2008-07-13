package suneido.database.query;

import java.util.List;

import suneido.database.Record;

public class QueryDifference extends QueryCompatible {
	QueryDifference(Query source1, Query source2) {
		super(source1, source2);
	}

	@Override
	public String toString() {
		String s = "(" + source + ") MINUS";
		if (disjoint != null)
			s += "-DISJOINT";
		if (ki != null)
			s += "^" + ki;
		return s + " (" + source2 + ")";
	}

	@Override
	List<String> columns() {
		return source.columns();
	}

	@Override
	Row get(Dir dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Header header() {
		return source.header();
	}

	@Override
	List<List<String>> indexes() {
		return source.indexes();
	}

	@Override
	List<List<String>> keys() {
		return source.keys();
	}

	@Override
	void rewind() {
		source.rewind();
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
	}

}
