package suneido.database.server;

import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;

public interface DbmsQuery {
	Header header();

	List<String> ordering();

	List<List<String>> keys();

	Row get(Dir dir);

	void rewind();

	String toString();

	void output(Record rec);

	void setTransaction(Transaction tran);

	boolean updateable();

}
