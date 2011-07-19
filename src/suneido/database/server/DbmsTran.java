package suneido.database.server;

import suneido.database.query.Query.Dir;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.intfc.database.Record;

public interface DbmsTran {
	String complete();

	void abort();

	int request(String s);

	DbmsQuery query(String s);

	void erase(long recadr);

	long update(long recadr, Record rec);

	HeaderAndRow get(Dir dir, String query, boolean one);

	boolean isReadonly();

	boolean isEnded();
}
