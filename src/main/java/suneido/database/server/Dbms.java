/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;

import suneido.SuContainer;
import suneido.SuDate;
import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;

/**
 * The interface between Suneido and the database. Used to hide the difference
 * between a local database ({@link DbmsLocal})
 * and a remote database ({@link DbmsRemote}).
 */
public abstract class Dbms {
	public abstract DbmsTran transaction(boolean readwrite);

	public abstract void admin(String s);

	public abstract DbmsQuery cursor(String s);

	public abstract List<Integer> tranlist();
	public abstract SuDate timestamp();
	public abstract String check();
	public abstract void dump(String filename);
	public abstract void copy(String filename);
	public abstract Object run(String s);
	public abstract long size();
	public abstract SuContainer connections();

	public abstract int cursors();
	public abstract String sessionid(String s);
	public String sessionid() {
		return sessionid("");
	}
	public abstract int finalSize();
	public abstract void log(String s);
	public abstract int kill(String s);
	public abstract Object exec(SuContainer c);

	public static class HeaderAndRow {
		public final Header header;
		public final Row row;

		public HeaderAndRow(Header header, Row row) {
			this.header = header;
			this.row = row;
		}
	}
	public final HeaderAndRow get(Dir dir, String query, boolean one) {
		DbmsTran tran = transaction(false);
		try {
			return tran.get(dir, query, one);
		} finally {
			tran.complete();
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
	public abstract List<LibGet> libget(String name);
	public abstract boolean use(String library);
	public abstract boolean unuse(String library);
	public abstract List<String> libraries();

	public abstract InetAddress getInetAddress();

	public abstract void disableTrigger(String table);
	public abstract void enableTrigger(String table);

}
