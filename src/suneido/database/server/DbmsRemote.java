package suneido.database.server;

import static suneido.Trace.CLIENTSERVER;
import static suneido.Trace.trace;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

import suneido.SuContainer;
import suneido.SuException;
import suneido.database.Mmfile;
import suneido.database.Record;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;
import suneido.language.Ops;
import suneido.language.Pack;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Client end of client-server connection.
 * ({@link DbmsServer} is the server side.)
 *
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class DbmsRemote extends Dbms {
	DbmsChannel io;

	public DbmsRemote(String ip, int port) {
		io = new DbmsChannel(ip, port);
		String msg = io.readLine();
		if (! msg.startsWith("Suneido Database Server"))
			throw new SuException("invalid connect response: " + msg);
		io.writeLine("BINARY");
		ok();
	}

	public void close() {
		io.close();
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		io.writeLine("TRANSACTION", (readwrite ? "update" : "read"));
		int tn = readInt('T');
		return new DbmsTranRemote(tn, !readwrite);
	}

	@Override
	public void admin(String s) {
		io.writeLine("ADMIN", s);
		assert io.readLine().equals("t");
	}

	@Override
	public DbmsQuery cursor(String s) {
		io.writeLineBuf("CURSOR", "Q" + s.length());
		trace(CLIENTSERVER, " => " + s);
		io.write(s);
		int cn = readInt('C');
		return new DbmsCursorRemote(cn);
	}

	@Override
	public List<LibGet> libget(String name) {
		io.writeLine("LIBGET", name);
		String s = io.readLine();
		Scanner scan = new Scanner(s);
		int n;
		ImmutableList.Builder<LibGet> builder = ImmutableList.builder();
		while (ERR != (n = getnum(scan, 'L'))) {
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
		io.writeLine(cmd);
		String s = io.readLine();
		return splitList(s);
	}

	private Iterable<String> splitList(String s) {
		return splitList(s, Splitter.on(",").omitEmptyStrings().trimResults());
	}
	private Iterable<String> splitList(String s, Splitter splitter) {
		if (! s.startsWith("(") || ! s.endsWith(")"))
			throw new SuException("expected (...)\r\ngot: " + s);
		s = s.substring(1, s.length() - 1);
		return splitter.split(s);
	}

	@Override
	public List<Integer> tranlist() {
		Iterable<String> iter = readList("TRANLIST");
		return ImmutableList.copyOf(Iterables.transform(iter, toInteger));
	}

	private static class ToInteger implements Function<String,Integer> {
		@Override
		public Integer apply(String s) {
			return Integer.parseInt(s);
		}
	}
	private static ToInteger toInteger = new ToInteger();

	private static class StringToList implements Function<String,List<String>> {
		@Override
		public List<String> apply(String s) {
			Iterable<String> fields = Splitter.on(',').split(s);
			return ImmutableList.copyOf(fields);
		}
	}
	private static StringToList stringToList = new StringToList();

	@Override
	public Date timestamp() {
		io.writeLine("TIMESTAMP");
		String s = io.readLine();
		Date date = Ops.stringToDate(s);
		if (date == null)
			throw new SuException("bad timestamp from server: " + s);
		return date;
	}

	@Override
	public void dump(String filename) {
		io.writeLine("DUMP", filename);
		ok();
	}

	@Override
	public void copy(String filename) {
		io.writeLine("COPY", filename);
		ok();
	}

	@Override
	public Object run(String s) {
		io.writeLine("RUN", s);
		return readValue();
	}

	@Override
	public long size() {
		io.writeLine("SIZE");
		int n = readInt('S');
		return Mmfile.intToOffset(n);
	}

	@Override
	public SuContainer connections() {
		io.writeLine("CONNECTIONS");
		return (SuContainer) readValue();
	}

	@Override
	public int cursors() {
		io.writeLine("CURSORS");
		return readInt('N');
	}

	@Override
	public String sessionid(String s) {
		io.writeLine("SESSIONID", s);
		return io.readLine();
	}

	@Override
	public int finalSize() {
		io.writeLine("FINAL");
		return readInt('N');
	}

	@Override
	public void log(String s) {
		io.writeLine("LOG", s);
		ok();
	}

	@Override
	public int kill(String s) {
		io.writeLine("KILL", s);
		return readInt('N');
	}

	private void ok() {
		String s = io.readLine();
		if (! s.equals("OK"))
			throw new SuException("expected 'OK', got: " + s);
	}

	private int readInt(char c) {
		String s = io.readLine();
		return ck_getnum(s, c);
	}

	private static final int ERR = -1;

	private static int ck_getnum(String s, char type) {
		return ck_getnum(new Scanner(s), type);
	}

	private static int ck_getnum(Scanner scan, char type) {
		int num = getnum(scan, type);
		if (num == ERR)
			throw new SuException("expecting: " + type + "#");
		return num;
	}

	private static int getnum(Scanner scan, char type) {
		try {
			scan.skip("\\s*" + type);
			return scan.nextInt();
		} catch (NoSuchElementException e) {
			return ERR;
		}
	}

	private Object readValue() {
		String s = io.readLine();
		if (s.equals(""))
			return null;
		int n = ck_getnum(s, 'P');
		ByteBuffer buf = io.read(n);
		return Pack.unpack(buf);
	}

	private HeaderAndRow readRecord(boolean withHeader) {
		String s = io.readLine();
		if (s.equals("EOF"))
			return null;
		Scanner scan = new Scanner(s);
		long recadr = Mmfile.intToOffset(ck_getnum(scan, 'A'));
		int reclen = ck_getnum(scan, 'R');
		Header header = null;
		if (withHeader) {
			s = s.substring(s.indexOf('('));
			header = parseHeader(splitList(s));
		}
		ByteBuffer buf = io.readNew(reclen);
		Record record = new Record(buf);
		Row row = new Row(record, recadr);
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
			io.writeLine("COMMIT", "T" + tn);
			String s = io.readLine();
			return s.equals("OK") ? null : s;
		}

		@Override
		public void abort() {
			isEnded = true;
			io.writeLine("ABORT", "T" + tn);
			ok();
		}

		@Override
		public int request(String s) {
			io.writeLineBuf("REQUEST", "T" + tn + " Q" + s.length());
			trace(CLIENTSERVER, "    " + s);
			io.write(s);
			return readInt('R');
		}

		@Override
		public DbmsQuery query(String s) {
			io.writeLineBuf("QUERY", "T" + tn + " Q" + s.length());
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
		public void erase(long recadr) {
			io.writeLine("ERASE", "T" + tn + " A" + Mmfile.offsetToInt(recadr));
			ok();
		}

		@Override
		public long update(long recadr, Record rec) {
			writeRecord("UPDATE T" + tn + " A" + Mmfile.offsetToInt(recadr), rec);
			return Mmfile.intToOffset(readInt('U'));
		}

		@Override
		public HeaderAndRow get(Dir dir, String query, boolean one) {
			io.writeLineBuf("GET1",
					(dir == Dir.PREV ? "- " : (one ? "1 " : "+ ")) +
					"T" + tn + " Q" + query.length());
			trace(CLIENTSERVER, "    " + query);
			io.write(query);
			return readRecord(true);
		}

	}

	private void writeRecord(String cmd, Record rec) {
		if (rec.bufSize() > rec.packSize())
			rec = rec.dup();
		io.writeLineBuf(cmd, " R" + rec.bufSize());
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
				io.writeLine("KEYS", toString());
				String s = io.readLine();
				s = s.substring(1, s.length() - 1); // remove outer parens
				Iterable<String> list = splitList(s, Splitter.on("),("));
				keys = ImmutableList.copyOf(
						Iterables.transform(list, stringToList));
			}
			return keys;
		}

		@Override
		public Row get(Dir dir) {
			io.writeLine("GET", (dir == Dir.NEXT ? "+ " : "- ") + " " + toString());
			HeaderAndRow hr = readRecord(false);
			return hr == null ? null : hr.row;
		}

		@Override
		public void rewind() {
			io.writeLine("REWIND", toString());
			ok();
		}

		@Override
		public void close() {
			io.writeLine("CLOSE", toString());
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
			throw SuException.unreachable();
		}

		@Override
		public String explain() {
			io.writeLine("EXPLAIN", toString());
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

}
