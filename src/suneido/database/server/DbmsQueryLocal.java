package suneido.database.server;

import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Query;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.intfc.database.Record;

public class DbmsQueryLocal implements DbmsQuery {
	private final Query q;

	public DbmsQueryLocal(Query q) {
		this.q = q;
	}

	@Override
	public Row get(Dir dir) {
		return q.get(dir);
	}

	@Override
	public Header header() {
		return q.header();
	}

	@Override
	public List<List<String>> keys() {
		return q.keys();
	}

	@Override
	public List<String> ordering() {
		return q.ordering();
	}

	@Override
	public void output(Record rec) {
		q.output(rec);
	}

	@Override
	public void rewind() {
		q.rewind();
	}

	@Override
	public void setTransaction(DbmsTran tran) {
		q.setTransaction(((DbmsTranLocal) tran).t);
	}

	@Override
	public boolean updateable() {
		return q.updateable();
	}

	@Override
	public String explain() {
		return toString();
	}

	@Override
	public String toString() {
		return q.toString();
	}

	@Override
	public void close() {
	}

}
