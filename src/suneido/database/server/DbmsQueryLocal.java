package suneido.database.server;

import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;

public class DbmsQueryLocal implements DbmsQuery {
	private final Query q;

	public DbmsQueryLocal(Query q) {
		this.q = q;
	}

	@Override
	public Row get(Dir dir) {
		Row row = q.get(dir);
		if (q.updateable() && row != null) {
			row.recadr = row.getFirstData().off(); // [1] to skip key
			assert row.recadr != 0;
		}
		return row;
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
		q.setTransaction((Transaction) tran);
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
