/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Suneido.dbpkg;
import static suneido.Trace.trace;
import static suneido.Trace.tracing;
import static suneido.Trace.Type.CLIENTSERVER;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import suneido.SuContainer;
import suneido.SuDate;
import suneido.SuException;
import suneido.SuInternalError;
import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.intfc.database.Record;
import suneido.runtime.Pack;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Client end of client-server connection.
 * ({@link DbmsServer} is the server side.)
 */
public class DbmsRemote extends Dbms {
	public final Thread owner = Thread.currentThread();
	public volatile long idleSince = 0; // used by TheDbms.closeIfIdle
	DbmsChannel io;
	private String sessionid;

	public DbmsRemote(String ip, int port) {
		io = new DbmsChannel(ip, port);
		String msg = io.readLine();
		if (! msg.startsWith("Suneido Database Server"))
			throw new SuException("invalid connect response: " + msg);
		writeLine("BINARY");
		ok();
		writeLine("SESSIONID");
		sessionid = io.readLine();
	}

	public void close() {
		io.close();
	}

	private void writeLine(String cmd) {
		io.writeLine(cmd);
		idleSince = 0;
	}
	private void writeLine(String cmd, String s) {
		io.writeLine(cmd, s);
		idleSince = 0;
	}
	private void writeLineBuf(String cmd, String s) {
		io.writeLineBuf(cmd, s);
		idleSince = 0;
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		writeLine("TRANSACTION", (readwrite ? "update" : "read"));
		int tn = readInt('T');
		return new DbmsTranRemote(tn, !readwrite);
	}

	@Override
	public void admin(String s) {
		writeLine("ADMIN", s);
		assert io.readLine().equals("t");
	}

	@Override
	public DbmsQuery cursor(String s) {
		writeLineBuf("CURSOR", "Q" + s.length());
		if (tracing(CLIENTSERVER))
			trace(CLIENTSERVER, " => " + s);
		io.write(s);
		int cn = readInt('C');
		return new DbmsCursorRemote(cn);
	}

	@Override
	public List<LibGet> libget(String name) {
		writeLine("LIBGET", name);
		String s = io.readLine();
		Scanner scan = new Scanner(s);
		int n;
		ImmutableList.Builder<LibGet> builder = ImmutableList.builder();
		while (ERR != (n = getInt(scan, 'L'))) {
			String library = io.readLine();
			ByteBuffer code = io.readNew(n);
			builder.add(new LibGet(library, code));
		}
		return builder.build();
	}

	@Override
	public List<String> libraries() {
		return ImmutableList.copyOf(readList("LIBRARIES"));
	}

	private Iterable<String> readList(String cmd) {
		writeLine(cmd);
		String s = io.readLine();
		return splitList(s);
	}

	private static Iterable<String> splitList(String s) {
		return splitList(s, Splitter.on(",").omitEmptyStrings().trimResults());
	}
	private static Iterable<String> splitList(String s, Splitter splitter) {
		if (! s.startsWith("(") || ! s.endsWith(")"))
			throw new SuException("expected (...)\r\ngot: " + s);
		s = s.substring(1, s.length() - 1);
		return splitter.split(s);
	}

	@Override
	public List<Integer> tranlist() {
		Iterable<String> iter = readList("TRANLIST");
		return ImmutableList.copyOf(Iterables.transform(iter, Integer::parseInt));
	}

	@Override
	public SuDate timestamp() {
		writeLine("TIMESTAMP");
		String s = io.readLine();
		SuDate date = SuDate.fromLiteral(s);
		if (date == null)
			throw new SuException("bad timestamp from server: " + s);
		return date;
	}

	@Override
	public String check() {
		writeLine("CHECK");
		return (String) readValue();
	}

	@Override
	public void dump(String filename) {
		writeLine("DUMP", filename);
		ok();
	}

	@Override
	public void copy(String filename) {
		writeLine("COPY", filename);
		ok();
	}

	@Override
	public Object run(String s) {
		writeLine("RUN", s);
		return readValue();
	}

	@Override
	public long size() {
		writeLine("SIZE");
		return readLong('S') << 2;
	}

	@Override
	public SuContainer connections() {
		writeLine("CONNECTIONS");
		return (SuContainer) readValue();
	}

	@Override
	public int cursors() {
		writeLine("CURSORS");
		return readInt('N');
	}

	@Override
	public String sessionid(String s) {
		if ("".equals(s))
			return sessionid;
		writeLine("SESSIONID", s);
		return sessionid = io.readLine();
	}

	@Override
	public int finalSize() {
		writeLine("FINAL");
		return readInt('N');
	}

	@Override
	public void log(String s) {
		writeLine("LOG", s);
		ok();
	}

	@Override
	public int kill(String s) {
		writeLine("KILL", s);
		return readInt('N');
	}

	@Override
	public Object exec(SuContainer c) {
		int n = c.packSize();
		writeLineBuf("EXEC", "P" + n);
		io.write(Pack.pack(c));
		return readValue();
	}

	private void ok() {
		String s = io.readLine();
		if (! s.equals("OK"))
			throw new SuException("expected 'OK', got: " + s);
	}

	private int readInt(char c) {
		String s = io.readLine();
		return ck_getInt(s, c);
	}

	private long readLong(char c) {
		String s = io.readLine();
		return ck_getLong(s, c);
	}

	private static final int ERR = -1;

	private static int ck_getInt(String s, char type) {
		return ck_getInt(new Scanner(s), type);
	}

	private static int ck_getInt(Scanner scan, char type) {
		int num = getInt(scan, type);
		if (num == ERR)
			throw new SuException("expecting: " + type + "#");
		return num;
	}

	private static int getInt(Scanner scan, char type) {
		try {
			scan.skip("\\s*" + type);
			return scan.nextInt();
		} catch (NoSuchElementException e) {
			return ERR;
		}
	}

	private static long ck_getLong(String s, char type) {
		long num = getLong(new Scanner(s), type);
		if (num == ERR)
			throw new SuException("expecting: " + type + "#");
		return num;
	}

	private static long getLong(Scanner scan, char type) {
		try {
			scan.skip("\\s*" + type);
			return scan.nextLong();
		} catch (NoSuchElementException e) {
			return ERR;
		}
	}

	private Object readValue() {
		String s = io.readLine();
		if (s.equals(""))
			return null;
		int n = ck_getInt(s, 'P');
		ByteBuffer buf = io.read(n);
		return Pack.unpack(buf);
	}

	private HeaderAndRow readRecord(boolean withHeader) {
		String s = io.readLine();
		if (s.equals("EOF"))
			return null;
		Scanner scan = new Scanner(s);
		int recadr = ck_getInt(scan, 'A');
		int reclen = ck_getInt(scan, 'R');
		Header header = null;
		if (withHeader) {
			s = s.substring(s.indexOf('('));
			header = parseHeader(splitList(s));
		}
		ByteBuffer buf = io.readNew(reclen);
		Record record = dbpkg.record(recadr, buf);
		Row row = new Row(record);
		return new HeaderAndRow(header, row);
	}

	private class DbmsTranRemote implements DbmsTran {
		private final int tn;
		private final boolean readonly;
		private boolean isEnded = false;

		public DbmsTranRemote(int tn, boolean readonly) {
			this.tn = tn;
			this.readonly = readonly;
		}

		@Override
		public String complete() {
			isEnded = true;
			writeLine("COMMIT", "T" + tn);
			String s = io.readLine();
			return s.equals("OK") ? null : s;
		}

		@Override
		public void abort() {
			isEnded = true;
			writeLine("ABORT", "T" + tn);
			ok();
		}

		@Override
		public int request(String s) {
			writeLineBuf("REQUEST", "T" + tn + " Q" + s.length());
			if (tracing(CLIENTSERVER))
				trace(CLIENTSERVER, "    " + s);
			io.write(s);
			return readInt('R');
		}

		@Override
		public DbmsQuery query(String s) {
			writeLineBuf("QUERY", "T" + tn + " Q" + s.length());
			if (tracing(CLIENTSERVER))
				trace(CLIENTSERVER, "    " + s);
			io.write(s);
			return new DbmsQueryRemote(readInt('Q'));
		}

		@Override
		public boolean isReadonly() {
			return readonly;
		}

		@Override
		public boolean isEnded() {
			return isEnded;
		}

		@Override
		public void erase(int recadr) {
			writeLine("ERASE", "T" + tn + " A" + recadr);
			ok();
		}

		@Override
		public int update(int recadr, Record rec) {
			writeRecord("UPDATE T" + tn + " A" + recadr, rec);
			return readInt('U');
		}

		@Override
		public HeaderAndRow get(Dir dir, String query, boolean one) {
			writeLineBuf("GET1",
					(dir == Dir.PREV ? "- " : (one ? "1 " : "+ ")) +
					"T" + tn + " Q" + query.length());
			if (tracing(CLIENTSERVER))
				trace(CLIENTSERVER, "    " + query);
			io.write(query);
			return readRecord(true);
		}

	}

	private void writeRecord(String cmd, Record rec) {
		rec = rec.squeeze();
		writeLineBuf(cmd, " R" + rec.bufSize());
		io.write(rec.getBuffer());
	}

	private class DbmsQueryRemote implements DbmsQuery {
		protected final int qn;
		private List<List<String>> keys;
		private Header header;

		DbmsQueryRemote(int qn) {
			this.qn = qn;
		}

		@Override
		public Header header() {
			if (header == null) {
				Iterable<String> list = readList("HEADER " + toString());
				header = parseHeader(list);
			}
			return header;
		}

		@Override
		public List<String> ordering() {
			return ImmutableList.copyOf(
					readList("ORDER " + toString()));
		}

		@Override
		public List<List<String>> keys() {
			if (keys == null) {
				writeLine("KEYS", toString());
				String s = io.readLine();
				s = s.substring(1, s.length() - 1); // remove outer parens
				Iterable<String> list = splitList(s, Splitter.on("),("));
				keys = ImmutableList.copyOf(
						Iterables.transform(list, (String t) -> {
							Iterable<String> fields = Splitter.on(',').split(t);
							return ImmutableList.copyOf(fields);
						}));
			}
			return keys;
		}

		@Override
		public Row get(Dir dir) {
			writeLine("GET", (dir == Dir.NEXT ? "+ " : "- ") + " " + toString());
			HeaderAndRow hr = readRecord(false);
			return hr == null ? null : hr.row;
		}

		@Override
		public void rewind() {
			writeLine("REWIND", toString());
			ok();
		}

		@Override
		public void close() {
			writeLine("CLOSE", toString());
			ok();
		}

		@Override
		public void output(Record rec) {
			writeRecord("OUTPUT " + this, rec);
			assert io.readLine().equals("t");
		}

		@Override
		public void setTransaction(DbmsTran tran) {
			// used by DbmsCursorRemote
		}

		@Override
		public boolean updateable() {
			// only used with DbmsLocal
			throw SuInternalError.unreachable();
		}

		@Override
		public String explain() {
			writeLine("EXPLAIN", toString());
			return io.readLine();
		}

		@Override
		public String toString() {
			return "Q" + qn;
		}

	}

	private static Header parseHeader(Iterable<String> list) {
		ImmutableList.Builder<String> fields = ImmutableList.builder();
		ImmutableList.Builder<String> columns = ImmutableList.builder();
		for (String s : list) {
			if (Character.isUpperCase(s.charAt(0)))
				s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
			else
				fields.add(s);
			if (! s.equals("-"))
				columns.add(s);
		}
		List<String> f = fields.build();
		return new Header(ImmutableList.of(f), columns.build());
	}

	private class DbmsCursorRemote extends DbmsQueryRemote {
		private int tn = -1;

		DbmsCursorRemote(int qn) {
			super(qn);
		}

		@Override
		public void setTransaction(DbmsTran tran) {
			tn = ((DbmsTranRemote) tran).tn;
		}

		@Override
		public String toString() {
			String s = "";
			if (tn >= 0) {
				s = "T" + tn + " ";
				tn = -1;
			}
			return s + "C" + qn;
		}

	}

	@Override
	public InetAddress getInetAddress() {
		return io.getInetAddress();
	}

	@Override
	public boolean use(String library) {
		throw new SuException("can't Use('" + library + "')\n" +
				"When client-server, only the server can Use");
	}

	@Override
	public boolean unuse(String library) {
		throw new SuException("can't Unuse('" + library + "')\n" +
				"When client-server, only the server can Unuse");
	}

	@Override
	public void disableTrigger(String table) {
//		throw new SuException("When client-server, only the server can DoWithoutTriggers");
	}

	@Override
	public void enableTrigger(String table) {
	}

}
