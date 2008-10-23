package suneido.database.server;

import static suneido.Suneido.verify;

import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;

public class DbmsQueryLocal implements DbmsQuery {
	private Query q;

	public Row get(Dir dir) {
		Row row = q.get(dir);
		if (q.updateable() && row != null)
			row.recadr = row.getFirstData().off(); // [1] to skip key
		verify(row.recadr >= 0);
		return row;
	}

	public Header header() {
		return q.header();
	}

	public List<List<String>> keys() {
		return q.keys();
	}

	public List<String> ordering() {
		return q.ordering();
	}

	public void output(Record rec) {
		q.output(rec);
	}

	public void rewind() {
		q.rewind();
	}

	public void setTransaction(Transaction tran) {
		q.setTransaction(tran);
	}

}
