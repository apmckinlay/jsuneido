package suneido.database.server;

import java.util.List;

import suneido.SuValue;
import suneido.database.Record;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;

/**
 * The interface between Suneido and the database. Used to hide the difference
 * between a local database ({@link DbmsLocal}) and one accessed client server.
 *
 * @author Andrew McKinlay
 *         <p>
 *         <small>Copyright 2008 Suneido Software Corp. All rights reserved.
 *         Licensed under GPLv2.</small>
 *         </p>
 */
public interface Dbms {
	DbmsTran transaction(boolean readwrite, String session_id);

	void admin(String s);
	int request(DbmsTran tran, String s);

	DbmsQuery cursor(String s);
	DbmsQuery query(DbmsTran tran, String s);
	List<String> libget(String name);
	List<String> libraries();
	List<Integer> tranlist();
	SuValue timestamp();
	void dump(String filename);
	void copy(String filename);
	SuValue run(String s);
	long size();
	SuValue connections();
	void erase(int tn, long recadr);

	long update(int tn, long recadr, Record rec);

	HeaderAndRow get(Dir dir, String query, boolean one, DbmsTran tran);
	int cursors();
	SuValue sessionid(String s);
	int finalSize();
	void log(String s);
	int kill(String s);

	static class HeaderAndRow {
		public final Header header;
		public final Row row;

		public HeaderAndRow(Header header, Row row) {
			this.header = header;
			this.row = row;
		}
	}

}
