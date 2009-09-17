package suneido.database.query;

import java.util.List;

import suneido.SuException;
import suneido.database.Record;
import suneido.database.Transaction;

// TODO history
public class History extends Query {
	String tablename;

	History(String tablename) {
		this.tablename = tablename;
	}

	@Override
	public String toString() {
		return "history(" + tablename + ")";
	}

	@Override
	List<String> columns() {
		throw new SuException("database history not implemented");
	}

	@Override
	int columnsize() {
		throw new SuException("database history not implemented");
	}

	@Override
	public Row get(Dir dir) {
		throw new SuException("database history not implemented");
	}

	@Override
	public Header header() {
		throw new SuException("database history not implemented");
	}

	@Override
	List<List<String>> indexes() {
		throw new SuException("database history not implemented");
	}

	@Override
	public List<List<String>> keys() {
		throw new SuException("database history not implemented");
	}

	@Override
	double nrecords() {
		throw new SuException("database history not implemented");
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		return 0;
	}

	@Override
	int recordsize() {
		return 0;
	}

	@Override
	public void rewind() {

	}

	@Override
	void select(List<String> index, Record from, Record to) {

	}

	@Override
	public void setTransaction(Transaction tran) {
	}

}
