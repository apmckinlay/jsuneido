package suneido.database.server;

import java.nio.ByteBuffer;
import java.util.Date;
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
	DbmsTran transaction(boolean readwrite);

	void admin(ServerData serverData, String s);
	int request(ServerData serverData, DbmsTran tran, String s);

	DbmsQuery cursor(ServerData serverData, String s);

	DbmsQuery query(ServerData serverData, DbmsTran tran, String s);
	List<LibGet> libget(String name);
	List<String> libraries();
	List<Integer> tranlist();
	Date timestamp();
	void dump(String filename);
	void copy(String filename);
	SuValue run(String s);
	long size();
	SuValue connections();
	void erase(DbmsTran tran, long recadr);

	long update(DbmsTran tran, long recadr, Record rec);

	HeaderAndRow get(ServerData serverData, Dir dir, String query, boolean one, DbmsTran tran);
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
	public static class LibGet {
		public String library;
		public ByteBuffer text;

		public LibGet(String library, ByteBuffer text) {
			this.library = library;
			this.text = text;
		}
	}


}
