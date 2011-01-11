package suneido.database.query;

import static suneido.SuException.unreachable;

import java.util.List;

import suneido.database.Record;

public abstract class QueryAction extends Query1 {

	public QueryAction(Query source) {
		super(source);
	}

	public abstract int execute();

	@Override
	List<String> columns() {
		return super.columns();
	}
	@Override
	public Row get(Dir dir) {
		throw unreachable();
	}
	@Override
	public Header header() {
		throw unreachable();
	}
	@Override
	List<List<String>> indexes() {
		throw unreachable();
	}
	@Override
	public List<List<String>> keys() {
		throw unreachable();
	}
	@Override
	public void rewind() {
		throw unreachable();
	}
	@Override
	void select(List<String> index, Record from, Record to) {
		throw unreachable();
	}
}
