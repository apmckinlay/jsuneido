package suneido.database.query;

import static suneido.SuException.unreachable;
import static suneido.database.Database.theDB;

import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;

public abstract class QueryAction extends Query1 {

	public QueryAction(Query source) {
		super(source);
	}

	public int execute() {
		Transaction tran = theDB.readwriteTran();
		try {
			int n = execute(tran);
			tran.complete();
			return n;
		} finally {
			tran.abortIfNotComplete();
		}
	}

	abstract int execute(Transaction tran);

	@Override
	List<String> columns() {
		throw unreachable();
	}
	@Override
	Row get(Dir dir) {
		throw unreachable();
	}
	@Override
	Header header() {
		throw unreachable();
	}
	@Override
	List<List<String>> indexes() {
		throw unreachable();
	}
	@Override
	List<List<String>> keys() {
		throw unreachable();
	}
	@Override
	void rewind() {
		throw unreachable();
	}
	@Override
	void select(List<String> index, Record from, Record to) {
		throw unreachable();
	}
}
