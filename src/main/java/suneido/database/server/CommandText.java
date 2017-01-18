/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static java.lang.Character.isDigit;
import static java.lang.Character.isWhitespace;
import static java.lang.Character.toUpperCase;
import static suneido.Suneido.dbpkg;
import static suneido.util.ByteBuffers.bufferToString;
import static suneido.util.ByteBuffers.stringToBuffer;
import static suneido.util.Util.listToParens;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

import suneido.SuContainer;
import suneido.SuException;
import suneido.TheDbms;
import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.database.server.Dbms.LibGet;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.runtime.Ops;
import suneido.runtime.Pack;

/**
 * Server side of the *text* client-server protocol.
 * See {@link DbmsServerText} for the actual server.
 * {@link DbmsClientText} is the client side.
 */
public enum CommandText {
	BADCMD {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer(badcmd + line + "\r\n");
		}
	},
	NILCMD {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return null;
		}

	},
	ADMIN {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			dbms().admin(line);
			return t();
		}
	},
	ABORT {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			int tn = new Reader(line).ck_getnum('T');
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			ServerData.forThread().endTransaction(tn);
			tran.abort();
			return ok();
		}
	},
	AUTH {
		@Override
		public int extra(String line) {
			return new Reader(line).ck_getnum('D');
		}

		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return Auth.auth(bufferToString(extra)) ? t() : f();
		}
	},
	BINARY {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			ServerData.forThread().textmode = false;
			return ok();
		}
	},
	CHECK {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return valueResult(output, dbms().check());
		}
	},
	CLOSE {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			Reader rdr = new Reader(line);
			int n;
			if (-1 != (n = rdr.getnum('Q')))
				ServerData.forThread().endQuery(n);
			else if (-1 != (n = rdr.getnum('C')))
				ServerData.forThread().endCursor(n);
			else
				throw new SuException("CLOSE expects Q# or C# got: " + line);
			return ok();
		}
	},
	COMMIT {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			int tn = new Reader(line).ck_getnum('T');
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			ServerData.forThread().endTransaction(tn);
			String conflict = tran.complete();
			return conflict == null ? ok() : stringToBuffer(conflict + "\r\n");
		}
	},
	CONNECTIONS {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return valueResult(output, dbms().connections());
		}
	},
	CURSOR {
		@Override
		public int extra(String line) {
			return new Reader(line).ck_getnum('Q');
		}

		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsQuery dq = dbms().cursor(bufferToString(extra));
			int cn = ServerData.forThread().addCursor(dq);
			return stringToBuffer("C" + cn + "\r\n");
		}
	},
	CURSORS {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer("N" + dbms().cursors() + "\r\n");
		}
	},
	DUMP {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return valueResult(output,
					dbms().dump(line.trim()));
		}
	},
	LOAD {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			int nrecs = dbms().load(line.trim());
			return stringToBuffer("N" +	nrecs + "\r\n");
		}
	},
	EOF { // for testing
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return eof();
		}
	},
	ERASE {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			Reader rdr = new Reader(line);
			DbmsTran tran = rdr.getTran();
			int recadr = rdr.ck_getnum('A');
			tran.erase(recadr);
			return ok();
		}
	},
	EXEC {
		@Override
		public int extra(String line) {
			return new Reader(line).ck_getnum('P');
		}
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			SuContainer c = (SuContainer) Pack.unpack(extra);
			Object result = dbms().exec(c);
			return valueResult(output, result);
		}

	},
	EXPLAIN {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsQuery q = new Reader(line).q_or_c();
			return stringToBuffer(q.toString() + "\r\n");
		}
	},
	FINAL {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer("N" + dbms().finalSize() + "\r\n");
		}
	},
	GET {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			Reader rdr = new Reader(line);
			Dir dir = rdr.getDir();
			DbmsQuery q = rdr.q_or_tc();
			get(q, dir, output);
			return null;
		}
	},
	GET1 {
		@Override
		public int extra(String line) {
			Reader rdr = new Reader(line);
			rdr.get();
			rdr.get();
			rdr.ck_getnum('T');
			return rdr.ck_getnum('Q');
		}
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			Reader rdr = new Reader(line);
			Dir dir = Dir.NEXT;
			boolean one = false;
			switch (rdr.get()) {
			case '+':
				dir = Dir.NEXT;
				break;
			case '-':
				dir = Dir.PREV;
				break;
			case '1':
				one = true;
				break;
			default:
				throw new SuException("get1 expects + or - or 1");
			}
			DbmsTran tran = rdr.getTran();
			String query = bufferToString(extra);
			HeaderAndRow hr = (tran == null)
				? dbms().get(dir, query, one)
				: tran.get(dir,	query, one);
			if (hr == null)
				output.accept(eof());
			else
				row_result(hr.row, hr.header, true, output);
			return null;
		}
	},
	HEADER {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsQuery q = new Reader(line).q_or_c();
			return stringToBuffer(listToParens(q.header().schema()) + "\r\n");
		}
	},
	KEYS {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsQuery q = new Reader(line).q_or_c();
			return stringToBuffer(listToParens(q.keys()) + "\r\n");
		}
	},
	KILL {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			String sessionId = line.trim();
			int nkilled = dbms().kill(sessionId);
			return stringToBuffer("N" + nkilled + "\r\n");
		}
	},
	LIBGET {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			List<LibGet> srcs = dbms().libget(line.trim());
			StringBuilder resp = new StringBuilder();
			for (LibGet src : srcs)
				resp.append("L").append(src.text.limit()).append(" ");
			resp.append("\r\n");
			output.accept(stringToBuffer(resp.toString()));

			for (LibGet src : srcs) {
				output.accept(stringToBuffer(src.library + "\r\n"));
				output.accept(src.text);
			}
			return null;
		}
	},
	LIBRARIES {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer(listToParens(dbms().libraries()) + "\r\n");
		}
	},
	LOG {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			dbms().log(line.trim());
			return ok();
		}
	},
	/** return a random string to hash with password for authorization */
	NONCE {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return ByteBuffer.wrap(Auth.nonce());
		}
	},
	ORDER {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsQuery q = new Reader(line).q_or_c();
			return stringToBuffer(listToParens(q.ordering()) + "\r\n");
		}
	},
	OUTPUT {
		@Override
		public int extra(String line) {
			Reader rdr = new Reader(line);
			if (-1 == rdr.getnum('T') || -1 == rdr.getnum('C'))
				rdr.ck_getnum('Q');
			return rdr.ck_getnum('R');
		}

		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsQuery q = new Reader(line).q_or_tc();
			q.output(makeRecord(extra));
			return t();
		}
	},
	QUERY {
		@Override
		public int extra(String line) {
			Reader rdr = new Reader(line);
			rdr.ck_getnum('T');
			return rdr.ck_getnum('Q');
		}

		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			int tn = new Reader(line).ck_getnum('T');
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			DbmsQuery dq = tran.query(bufferToString(extra));
			int qn = ServerData.forThread().addQuery(tn, dq);
			return stringToBuffer("Q" + qn + "\r\n");
		}
	},
	READCOUNT {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsTran tran = new Reader(line).getTran();
			return stringToBuffer("C" + (tran.readCount()) + "\r\n");
		}
	},
	REQUEST {
		@Override
		public int extra(String line) {
			Reader rdr = new Reader(line);
			rdr.ck_getnum('T');
			return rdr.ck_getnum('Q');
		}

		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsTran tran = new Reader(line).getTran();
			int n = tran.request(bufferToString(extra));
			return stringToBuffer("R" + n + "\r\n");
		}
	},
	REWIND {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			Reader rdr = new Reader(line);
			rdr.getnum('T'); // ignore
			DbmsQuery q = rdr.q_or_c();
			q.rewind();
			return ok();
		}
	},
	RUN {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			Object result = dbms().run(line);
			if (result == null)
				return stringToBuffer("\r\n");
			return ServerData.forThread().textmode
					? stringToBuffer(Ops.display(result) + "\r\n")
					: valueResult(output, result);
		}
	},
	SESSIONID {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer(
					dbms().sessionid(line.trim()) + "\r\n");
		}
	},
	SIZE {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer("S" + (dbms().size() >> 2) + "\r\n");
		}
	},
	TEMPDEST {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer("D0\r\n");
		}
	},
	TEXT {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			ServerData.forThread().textmode = true;
			return ok();
		}
	},
	TIMESTAMP {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer(Ops.display(dbms().timestamp()) + "\r\n");
		}
	},
	/** return a random string for one-time authorization */
	TOKEN {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return ByteBuffer.wrap(Auth.token());
		}
	},
	TRANLIST {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			return stringToBuffer(listToParens(dbms().transactions()) + "\r\n");
		}
	},
	TRANSACTION {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			String s = line.trim().toLowerCase();
			boolean readwrite = false;
			if (match(s, "update"))
				readwrite = true;
			else if (!match(s, "read"))
				return stringToBuffer("ERR invalid transaction mode: " + s
						+ "\r\n");
			// MAYBE associate session id with transaction
			int tn = ServerData.forThread().addTransaction(
					dbms().transaction(readwrite));
			return stringToBuffer("T" + tn + "\r\n");
		}
	},
	UPDATE {
		@Override
		public int extra(String line) {
			Reader rdr = new Reader(line);
			rdr.ck_getnum('T');
			rdr.ck_getnum('A');
			return rdr.ck_getnum('R');
		}

		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			Reader rdr = new Reader(line);
			DbmsTran tran = rdr.getTran();
			int recadr = rdr.ck_getnum('A');
			recadr = tran.update(recadr, makeRecord(extra));
			return stringToBuffer("U" + recadr + "\r\n");
		}
	},
	WRITECOUNT {
		@Override
		public ByteBuffer execute(String line, ByteBuffer extra,
				Consumer<ByteBuffer> output) {
			DbmsTran tran = new Reader(line).getTran();
			return stringToBuffer("C" + (tran.writeCount()) + "\r\n");
		}
	};

	//==========================================================================

	/**
	 * @param buf A ByteBuffer containing the command line.
	 * @return The amount of "extra" data required by the command in the buffer.
	 */
	public int extra(String line) {
		return 0;
	}

	/**
	 * @param line Current position is past the first (command) word.
	 * @return null or a result buffer to be output
	 */
	public abstract ByteBuffer execute(String line, ByteBuffer extra,
			Consumer<ByteBuffer> output);

	//==========================================================================

	private static final String badcmd = "ERR bad command: ";
	private static final ByteBuffer OK_ = stringToBuffer("OK\r\n");
	private static final ByteBuffer EOF_ = stringToBuffer("EOF\r\n");
	private static final ByteBuffer TRUE_ = stringToBuffer("t\r\n");
	private static final ByteBuffer FALSE_ = stringToBuffer("f\r\n");

	static ByteBuffer ok() {
		return OK_.duplicate();
	}
	static ByteBuffer eof() {
		return EOF_.duplicate();
	}
	static ByteBuffer t() {
		return TRUE_.duplicate();
	}
	static ByteBuffer f() {
		return FALSE_.duplicate();
	}

	public static class Reader {
		String str;
		int pos = 0;

		public Reader(String s) {
			str = s;
		}

		char get() {
			return str.charAt(pos++);
		}

		char get(int i) {
			return i < str.length() ? str.charAt(i) : 0;
		}

		/**
		 * Skips whitespace then looks for 'type' char followed by digits, starting
		 * at buf's current position. If successful, advances buf's position to past
		 * digits and following whitespace
		 * @return The digits converted to an int, or -1 if unsuccessful.
		 */
		int getnum(char type) {
			int i = pos;
			while (isWhitespace(get(i)))
				++i;
			if (toUpperCase(get(i)) != type ||
					! (isDigit(get(i + 1)) ||
						(get(i + 1) == '-' && isDigit(get(i + 2)))))
				return -1;
			++i; // skip type
			int j = i;
			while (isDigit(get(i)) || get(i) == '-')
				++i;
			int n = Integer.valueOf(str.substring(j, i));
			while (isWhitespace(get(i)))
				++i;
			pos = i;
			return n;
		}

		private int ck_getnum(char type) {
			int num = getnum(type);
			if (num == -1)
				throw new SuException("expecting: " + type + "#");
			return num;
		}

		private DbmsTran getTran() {
			int tn = ck_getnum('T');
			return ServerData.forThread().getTransaction(tn);
		}

		private DbmsQuery q_or_c() {
			DbmsQuery q = null;
			int n;
			if (-1 != (n = getnum('Q')))
				q = ServerData.forThread().getQuery(n);
			else if (-1 != (n = getnum('C')))
				q = ServerData.forThread().getCursor(n);
			else
				throw new SuException("expecting Q# or C# got: " + str);
			if (q == null)
				throw new SuException("valid query or cursor required");
			return q;
		}

		private DbmsQuery q_or_tc() {
			DbmsQuery q = null;
			int n, tn;
			if (-1 != (n = getnum('Q')))
				q = ServerData.forThread().getQuery(n);
			else if (-1 != (tn = getnum('T')) && -1 != (n = getnum('C'))) {
				q = ServerData.forThread().getCursor(n);
				q.setTransaction(ServerData.forThread().getTransaction(tn));
			} else
				throw new SuException("expecting Q# or T# C#");
			if (q == null)
				throw new SuException("valid query or cursor required");
			return q;
		}

		private Dir getDir() {
			Dir dir;
			switch (get()) {
			case '+':
				dir = Dir.NEXT;
				break;
			case '-':
				dir = Dir.PREV;
				break;
			default:
				throw new SuException("get expects + or -");
			}
			get(); // skip space
			return dir;
		}

	}

	private static void get(DbmsQuery q, Dir dir, Consumer<ByteBuffer> output) {
		Row row = q.get(dir);
		if (row == null)
			output.accept(eof());
		else
			row_result(row, q.header(), false, output);
	}

	private static void row_result(Row row, Header hdr, boolean sendhdr,
			Consumer<ByteBuffer> output) {
		Record rec = rowToRecord(row, hdr);
		String s = "A" + row.address() + " R" + rec.bufSize();
		if (sendhdr)
			s += ' ' + listToParens(hdr.schema());
		s += "\r\n";
		output.accept(stringToBuffer(s));
		output.accept(rec.getBuffer());
	}

	static Record rowToRecord(Row row, Header hdr) {
		Record rec = row.firstData();
		if (row.size() > 2) {
			RecordBuilder rb = dbpkg.recordBuilder();
			for (String f : hdr.fields())
				rb.add(row.getraw(hdr, f));
			rec = rb.trim().build();
		}
		return rec;
	}

	/** WARNING: does not copy, the buffer must not be reused */
	private static Record makeRecord(ByteBuffer extra) {
		return dbpkg.record(0, extra);
	}

	private static ByteBuffer valueResult(Consumer<ByteBuffer> output, Object result) {
		if (result == null)
			return stringToBuffer("\r\n");
		ByteBuffer buf = Pack.pack(result);
		output.accept(stringToBuffer("P" + buf.remaining() + "\r\n"));
		return buf;
	}

	private static boolean match(String line, String string) {
		if (!line.startsWith(string))
			return false;
		int n = string.length();
		return line.length() == n || line.charAt(n) == ' ';
	}

	private static Dbms dbms() {
		Dbms dbms = TheDbms.dbms();
		if (! ServerData.forThread().auth)
			dbms = new DbmsUnauth(dbms);
		return dbms;
	}
}
