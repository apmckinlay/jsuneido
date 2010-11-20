package suneido.database.server;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

import suneido.SuContainer;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;

/**
 * The interface between Suneido and the database. Used to hide the difference
 * between a local database ({@link DbmsLocal})
 * and a remote database ({@link DbmsRemote}).
 *
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class Dbms {
	public abstract DbmsTran transaction(boolean readwrite);

	public abstract void admin(String s);

	public abstract DbmsQuery cursor(String s);

	public abstract List<String> libraries();
	public abstract List<Integer> tranlist();
	public abstract Date timestamp();
	public abstract void dump(String filename);
	public abstract void copy(String filename);
	public abstract Object run(String s);
	public abstract long size();
	public abstract SuContainer connections();

	public abstract int cursors();
	public abstract String sessionid(String s);
	public abstract int finalSize();
	public abstract void log(String s);
	public abstract int kill(String s);

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

	public abstract InetAddress getInetAddress();

}
