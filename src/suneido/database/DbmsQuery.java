package suneido.database;

import java.util.List;

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

	void setTransaction(Transaction tn);

}
