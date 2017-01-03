/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Suneido.dbpkg;
import static suneido.database.server.CommandBinary.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.List;

import com.google.common.collect.ImmutableList;

import suneido.SuContainer;
import suneido.SuDate;
import suneido.SuException;
import suneido.SuInternalError;
import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.intfc.database.Record;

/**
 * Client side of the *binary* client-server protocol.
 * {@link DbmsServerBinary} and {@link CommandBinary} are the server side.
 * Uses {@link SuChannel} which uses {@link Serializer}
*/
public class DbmsClientBinary extends Dbms {
	public final Thread owner = Thread.currentThread();
	public volatile long idleSince = 0; // used by TheDbms.closeIfIdle
	private String sessionid = "";
	private SuChannel io;

	public DbmsClientBinary(String ip, int port) {
		this(open(ip, port));
	}

	private static Channel open(String ip, int port) {
		try {
			return SocketChannel.open(new InetSocketAddress(ip, port));
		} catch (Exception e) {
			throw new SuException("can't connect to " + ip + ":" + port, e);
		}
	}

	public DbmsClientBinary(Channel channel) {
		this.io = new SuChannel(channel);
		sessionid = sessionid("");
	}

	public void close() {
		io.close();
	}

	private void doRequest() {
		io.write();
		getResponse();
	}

	private void getResponse() {
		if (! io.getBool()) {
			String err = io.getString();
			throw new SuException(err + " (from server)");
		}
	}

	// requests ----------------------------------------------------------------

	private Serializer putCmd(CommandBinary cmd) {
		return io.putByte((byte) cmd.ordinal());
	}

	private void send(CommandBinary cmd) {
		putCmd(cmd);
		doRequest();
	}

	private void send(CommandBinary cmd, boolean b) {
		putCmd(cmd).put(b);
		doRequest();
	}

	private void send(CommandBinary cmd, String s) {
		putCmd(cmd).put(s);
		doRequest();
	}

	private void send(CommandBinary cmd, int n) {
		putCmd(cmd).put(n);
		doRequest();
	}

	private void send(CommandBinary cmd, int n, char q_or_c) {
		putCmd(cmd).put(n).putByte((byte) q_or_c);
		doRequest();
	}

	private void send(CommandBinary cmd, int n, int m) {
		putCmd(cmd).put(n).put(m);
		doRequest();
	}

	private void send(CommandBinary cmd, int tn, String request) {
		putCmd(cmd).put(tn).put(request);
		doRequest();
	}

	private void sendValue(CommandBinary cmd, Object value) {
		putCmd(cmd).putPacked(value);
		doRequest();
	}

	private void send(CommandBinary cmd, char c, int tn, String query) {
		putCmd(cmd).putByte((byte) c).put(tn).put(query);
		doRequest();
	}

	private void send(CommandBinary cmd, char c, int tn, int qn) {
		putCmd(cmd).putByte((byte) c).put(tn).put(qn);
		doRequest();
	}

	public void send(CommandBinary cmd, int qn, ByteBuffer buffer) {
		putCmd(cmd).put(qn).put(buffer);
		doRequest();
	}

	private void send(CommandBinary cmd, int tn, int recadr, Record rec) {
		putCmd(cmd).put(tn).put(recadr).put(rec.getBuffer());
		doRequest();
	}

	// response results --------------------------------------------------------

	public Object valueResult() {
		return io.getBool() ? io.getPacked() : null;
	}

	public List<List<String>> stringListListResult() {
		int n = io.getInt();
		ImmutableList.Builder<List<String>> builder = ImmutableList.builder();
		for (int i = 0; i < n; ++i)
			builder.add(io.getStrings());
		return builder.build();
	}

	/** Does NOT clone the buffers, they will be invalid after the request  */
	private List<LibGet> libgetResult() {
		int n = io.getInt();
		String[] libs = new String[n];
		int[] sizes = new int[n];
		int totalSize = 0;
		for (int i = 0; i < n; ++i) {
			libs[i] = io.getString();
			sizes[i] = io.getInt();
			totalSize += sizes[i];
		}
		ByteBuffer buf = io.getBuffer(totalSize);
		ImmutableList.Builder<LibGet> builder = ImmutableList.builder();
		for (int i = 0; i < n; ++i) {
			String lib = libs[i];
			buf.limit(buf.position() + sizes[i]);
			ByteBuffer text = buf.slice();
			builder.add(new LibGet(lib, text));
			buf.position(buf.limit());
		}
		return builder.build();
	}

	public HeaderAndRow rowResult(boolean withHeader) {
		if (! io.getBool())
			return null;
		int recadr = io.getInt();
		Header header = withHeader ? headerResult() : null;
		ByteBuffer buf = io.getOwnedBuffer();
		Record record = dbpkg.record(recadr, buf);
		Row row = new Row(record);
		return new HeaderAndRow(header, row);
	}

	private Header headerResult() {
		int n = io.getInt();
		ImmutableList.Builder<String> fields = ImmutableList.builder();
		ImmutableList.Builder<String> columns = ImmutableList.builder();
		for (int i = 0; i < n; ++i) {
			String s = io.getString();
			if (Character.isUpperCase(s.charAt(0)))
				s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
			else
				fields.add(s);
			if (! s.equals("-"))
				columns.add(s);
		}
		return new Header(ImmutableList.of(fields.build()), columns.build());
	}

	// commands ----------------------------------------------------------------

	@Override
	public void admin(String s) {
		send(ADMIN, s);
	}

	@Override
	public boolean auth(String data) {
		if ("".equals(data))
			return false;
		send(AUTH, data);
		return io.getBool();
	}

	@Override
	public String check() {
		send(CHECK);
		return io.getString();
	}

	@Override
	public SuContainer connections() {
		send(CONNECTIONS);
		return (SuContainer) io.getPacked();
	}

	@Override
	public DbmsQuery cursor(String s) {
		send(CURSOR, s);
		int cn = io.getInt();
		return new DbmsClientCursor(cn);
	}

	@Override
	public int cursors() {
		send(CURSORS);
		return io.getInt();
	}

	@Override
	public String dump(String table) {
		send(DUMP, table);
		return io.getString();
	}

	@Override
	public Object exec(SuContainer c) {
		sendValue(EXEC, c);
		return valueResult();
	}

	@Override
	public int finalSize() {
		send(FINAL);
		return io.getInt();
	}

	@Override
	public HeaderAndRow get(Dir dir, String query, boolean one) {
		send(GET1, one ? '1' : (dir == Dir.PREV ? '-' : '+'), -1, query);
		return rowResult(true);
	}

	@Override
	public int kill(String s) {
		send(KILL, s);
		return io.getInt();
	}

	@Override
	public List<LibGet> libget(String name) {
		send(LIBGET, name);
		return libgetResult();
	}

	@Override
	public List<String> libraries() {
		send(LIBRARIES);
		return io.getStrings();
	}

	@Override
	public int load(String filename) {
		send(LOAD, filename);
		return io.getInt();
	}

	@Override
	public void log(String s) {
		send(LOG, s);
	}

	@Override
	public byte[] nonce() {
		send(NONCE);
		return io.getBytes();
	}

	@Override
	public Object run(String s) {
		send(RUN, s);
		return valueResult();
	}

	@Override
	public String sessionid(String s) {
		if ("".equals(s) && sessionid != null)
			return sessionid; // use cached value
		send(SESSIONID, s);
		return sessionid = io.getString();
	}

	@Override
	public long size() {
		send(SIZE);
		return io.getLong();
	}

	@Override
	public SuDate timestamp() {
		send(TIMESTAMP);
		return (SuDate) io.getPacked();
	}

	@Override
	public byte[] token() {
		send(TOKEN);
		return io.getBytes();
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		send(TRANSACTION, readwrite);
		int tn = io.getInt();
		return new DbmsClientTran(tn, !readwrite);
	}

	@Override
	public List<Integer> transactions() {
		send(TRANSACTIONS);
		return io.getInts();
	}

	@Override
	public boolean use(String library) {
		if (libraries().contains(library))
			return false;
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
	}

	@Override
	public void enableTrigger(String table) {
	}

	//--------------------------------------------------------------------------

	private class DbmsClientTran implements DbmsTran {
		private final int tn;
		private final boolean readonly;
		private boolean isEnded = false;

		public DbmsClientTran(int tn, boolean readonly) {
			this.tn = tn;
			this.readonly = readonly;
		}

		@Override
		public void abort() {
			isEnded = true;
			send(ABORT, tn);
		}

		@Override
		public String complete() {
			isEnded = true;
			send(COMMIT, tn);
			return io.getBool() ? io.getString() : null;
		}

		@Override
		public void erase(int recadr) {
			send(ERASE, tn, recadr);
		}

		@Override
		public HeaderAndRow get(Dir dir, String query, boolean one) {
			send(GET1, one ? '1' : (dir == Dir.PREV ? '-' : '+'), tn, query);
			return rowResult(true);
		}

		@Override
		public DbmsQuery query(String s) {
			send(QUERY, tn, s);
			return new DbmsClientQuery(io.getInt());
		}

		@Override
		public int readCount() {
			send(READCOUNT, tn);
			return io.getInt();
		}

		@Override
		public int request(String s) {
			send(REQUEST, tn, s);
			return io.getInt();
		}

		@Override
		public int update(int recadr, Record rec) {
			send(UPDATE, tn, recadr, rec);
			return io.getInt();
		}

		@Override
		public int writeCount() {
			send(WRITECOUNT, tn);
			return io.getInt();
		}

		@Override
		public boolean isReadonly() {
			return readonly;
		}

		@Override
		public boolean isEnded() {
			return isEnded;
		}
	}

	//--------------------------------------------------------------------------

	private class DbmsClientQuery implements DbmsQuery {
		protected final int qn;
		private List<List<String>> keys; // cache
		private Header header; // cache

		DbmsClientQuery(int qn) {
			this.qn = qn;
		}

		@Override
		public Header header() {
			if (header == null) {
				send(HEADER, qn, c_or_q());
				header = parseHeader(io.getStrings());
			}
			return header;
		}

		@Override
		public List<String> ordering() {
			send(ORDER, qn, c_or_q());
			return io.getStrings();
		}

		@Override
		public List<List<String>> keys() {
			if (keys == null) {
				send(KEYS, qn, c_or_q());
				keys = stringListListResult();
			}
			return keys;
		}

		@Override
		public Row get(Dir dir) {
			send(GET, (dir == Dir.NEXT ? '+' : '-'), getTran(), qn);
			HeaderAndRow hr = rowResult(false);
			return hr == null ? null : hr.row;
		}

		@Override
		public void rewind() {
			send(REWIND, qn, c_or_q());
		}

		@Override
		public void output(Record rec) {
			send(OUTPUT, qn, rec.getBuffer());
		}

		@Override
		public void setTransaction(DbmsTran tran) {
			// used by DbmsClientCursor
			throw SuInternalError.unreachable();
		}

		@Override
		public boolean updateable() {
			// only used with DbmsLocal
			throw SuInternalError.unreachable();
		}

		@Override
		public String explain() {
			send(EXPLAIN, qn, c_or_q());
			return io.getString();
		}

		@Override
		public void close() {
			send(CLOSE, qn, c_or_q());
		}

		protected char c_or_q() {
			return 'q';
		}

		protected int getTran() {
			return -1;
		}
	}

	private class DbmsClientCursor extends DbmsClientQuery {
		protected int tn = -1;

		DbmsClientCursor(int qn) {
			super(qn);
		}

		@Override
		public void output(Record rec) {
			throw SuInternalError.unreachable();
		}

		@Override
		public void setTransaction(DbmsTran tran) {
			tn = ((DbmsClientTran) tran).tn;
		}

		@Override
		protected char c_or_q() {
			return 'c';
		}

		@Override
		protected int getTran() {
			assert tn != -1;
			int tn = this.tn;
			this.tn = -1;
			return tn;
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

}
