package suneido.database.query;

import java.util.List;

import suneido.database.Record;

public class Extend extends Query2 {
	List<String> flds;
	List<Expr> exprs;

	Extend(Query source1, Query source2) {
		super(source1, source2);
	}

	@Override
	List<String> columns() {
		// TODO Auto-generated method stub
		return null;
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
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}
}
