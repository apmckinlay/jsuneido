/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Suneido.dbpkg;
import static suneido.util.Util.bufferToString;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.stringToBuffer;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import suneido.SuContainer;
import suneido.SuException;
import suneido.Suneido;
import suneido.TheDbms;
import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.database.server.Dbms.LibGet;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.language.Ops;
import suneido.language.Pack;
import suneido.language.builtin.ServerEval;
import suneido.util.NetworkOutput;

/**
 * Implements the server protocol commands.
 */
public enum Command {
	BADCMD {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			outputQueue.add(badcmd());
			return line;
		}
	},
	NILCMD {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return null;
		}

	},
	ADMIN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			TheDbms.dbms().admin(bufferToString(line));
			return t();
		}
	},
	ABORT {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			ServerData.forThread().endTransaction(tn);
			tran.abort();
			return ok();
		}
	},
	BINARY {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			ServerData.forThread().textmode = false;
			return ok();
		}
	},
	CLOSE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			int n;
			if (-1 != (n = getnum('Q', line)))
				ServerData.forThread().endQuery(n);
			else if (-1 != (n = getnum('C', line)))
				ServerData.forThread().endCursor(n);
			else
				throw new SuException("CLOSE expects Q# or C#");
			return ok();
		}
	},
	COMMIT {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			ServerData.forThread().endTransaction(tn);
			String conflict = tran.complete();
			return conflict == null ? ok() : stringToBuffer(conflict + "\r\n");
		}
	},
	CONNECTIONS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return valueResult(outputQueue, TheDbms.dbms().connections());
		}
	},
	COPY {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			TheDbms.dbms().copy(bufferToString(line).trim());
			return ok();
		}
	},
	CURSOR {
		@Override
		public int extra(ByteBuffer buf) {
			return ck_getnum('Q', buf);
		}

		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsQuery dq = TheDbms.dbms().cursor(bufferToString(extra));
			int cn = ServerData.forThread().addCursor(dq);
			return stringToBuffer("C" + cn + "\r\n");
		}
	},
	CURSORS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer("N" + TheDbms.dbms().cursors() + "\r\n");
		}
	},
	DUMP {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			TheDbms.dbms().dump(bufferToString(line).trim());
			return ok();
		}
	},
	EOF { // for testing
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return eof();
		}
	},
	ERASE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsTran tran = getTran(line);
			int recadr = ck_getnum('A', line);
			tran.erase(recadr);
			return ok();
		}
	},
	EXEC {
		@Override
		public int extra(ByteBuffer buf) {
			return ck_getnum('P', buf);
		}
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			SuContainer c = (SuContainer) Pack.unpack(extra);
			Object result = ServerEval.exec(c);
			return valueResult(outputQueue, result);
		}

	},
	EXPLAIN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsQuery q = q_or_c(line);
			return stringToBuffer(q.toString() + "\r\n");
		}
	},
	FINAL {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer("N" + TheDbms.dbms().finalSize() + "\r\n");
		}
	},
	GET {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			Dir dir = getDir(line);
			DbmsQuery q = q_or_tc(line);
			get(q, dir, outputQueue);
			return null;
		}
	},
	GET1 {
		@Override
		public int extra(ByteBuffer buf) {
			buf.get();
			buf.get();
			ck_getnum('T', buf);
			return ck_getnum('Q', buf);
		}
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			Dir dir = Dir.NEXT;
			boolean one = false;
			switch (line.get()) {
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
			DbmsTran tran = getTran(line);
			String query = bufferToString(extra);
			HeaderAndRow hr = (tran == null)
				? TheDbms.dbms().get(dir, query, one)
				: tran.get(dir,	query, one);
			if (hr == null)
				outputQueue.add(eof());
			else
				row_result(hr.row, hr.header, true, outputQueue);
			return null;
		}
	},
	HEADER {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsQuery q = q_or_c(line);
			return stringToBuffer(listToParens(q.header().schema()) + "\r\n");
		}
	},
	KEYS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsQuery q = q_or_c(line);
			return stringToBuffer(listToParens(q.keys()) + "\r\n");
		}
	},
	KILL {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			int nkilled = 0;
			String sessionId = bufferToString(line).trim();
			synchronized(DbmsServer.serverDataSet) {
				Iterator<ServerData> iter = DbmsServer.serverDataSet.iterator();
				while (iter.hasNext()) {
					ServerData serverData = iter.next();
					if (sessionId.equals(serverData.getSessionId())) {
						++nkilled;
						serverData.end();
						serverData.outputQueue.close();
						iter.remove();
					}
				}
			}
			return stringToBuffer("N" + nkilled + "\r\n");
		}
	},
	LIBGET {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			List<LibGet> srcs = TheDbms.dbms().libget(bufferToString(line).trim());
			StringBuilder resp = new StringBuilder();
			for (LibGet src : srcs)
				resp.append("L").append(src.text.limit()).append(" ");
			resp.append("\r\n");
			outputQueue.add(stringToBuffer(resp.toString()));

			for (LibGet src : srcs) {
				outputQueue.add(stringToBuffer(src.library + "\r\n"));
				outputQueue.add(src.text);
			}
			return null;
		}
	},
	LIBRARIES {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer(listToParens(TheDbms.dbms().libraries()) + "\r\n");
		}
	},
	LOG {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			TheDbms.dbms().log(bufferToString(line).trim());
			return ok();
		}
	},
	ORDER {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsQuery q = q_or_c(line);
			return stringToBuffer(listToParens(q.ordering()) + "\r\n");
		}
	},
	OUTPUT {
		@Override
		public int extra(ByteBuffer buf) {
			if (-1 == getnum('T', buf) || -1 == getnum('C', buf))
				ck_getnum('Q', buf);
			return ck_getnum('R', buf);
		}

		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsQuery q = q_or_tc(line);
			q.output(makeRecord(extra));
			return t();
		}
	},
	QUERY {
		@Override
		public int extra(ByteBuffer buf) {
			ck_getnum('T', buf);
			return ck_getnum('Q', buf);
		}

		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			DbmsQuery dq = tran.query(bufferToString(extra));
			int qn = ServerData.forThread().addQuery(tn, dq);
			return stringToBuffer("Q" + qn + "\r\n");
		}
	},
	REQUEST {
		@Override
		public int extra(ByteBuffer buf) {
			ck_getnum('T', buf);
			return ck_getnum('Q', buf);
		}

		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsTran tran = getTran(line);
			int n = tran.request(bufferToString(extra));
			return stringToBuffer("R" + n + "\r\n");
		}
	},
	REWIND {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsQuery q = q_or_c(line);
			q.rewind();
			return ok();
		}
	},
	RUN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			Object result = TheDbms.dbms().run(bufferToString(line));
			if (result == null)
				return stringToBuffer("\r\n");
			return ServerData.forThread().textmode
					? stringToBuffer(Ops.display(result) + "\r\n")
					: valueResult(outputQueue, result);
		}
	},
	SESSIONID {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer(
					TheDbms.dbms().sessionid(bufferToString(line).trim()) + "\r\n");
		}
	},
	SIZE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer("S" + TheDbms.dbms().size() + "\r\n");
		}
	},
	TEMPDEST {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer("D0\r\n");
		}
	},
	TEXT {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			ServerData.forThread().textmode = true;
			return ok();
		}
	},
	TIMESTAMP {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer(Ops.display(TheDbms.dbms().timestamp()) + "\r\n");
		}
	},
	TRANLIST {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer(listToParens(TheDbms.dbms().tranlist()) + "\r\n");
		}
	},
	TRANSACTION {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			String s = bufferToString(line).trim().toLowerCase();
			boolean readwrite = false;
			if (match(s, "update"))
				readwrite = true;
			else if (!match(s, "read"))
				return stringToBuffer("ERR invalid transaction mode: " + s
						+ "\r\n");
			// MAYBE associate session id with transaction
			int tn = ServerData.forThread().addTransaction(
					TheDbms.dbms().transaction(readwrite));
			return stringToBuffer("T" + tn + "\r\n");
		}
	},
	UPDATE {
		@Override
		public int extra(ByteBuffer buf) {
			ck_getnum('T', buf);
			ck_getnum('A', buf);
			return ck_getnum('R', buf);
		}

		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			DbmsTran tran = getTran(line);
			int recadr = ck_getnum('A', line);
			recadr = tran.update(recadr, makeRecord(extra));
			return stringToBuffer("U" + recadr + "\r\n");
		}
	};

	//==========================================================================

	/**
	 * @param buf A ByteBuffer containing the command line.
	 * @return The amount of "extra" data required by the command in the buffer.
	 */
	public int extra(ByteBuffer buf) {
		return 0;
	}

	/**
	 * @param line Current position is past the first (command) word.
	 * @param extra
	 * @param outputQueue
	 * @return null or a result buffer to be output
	 */
	public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
			NetworkOutput outputQueue) {
		outputQueue.add(notimp());
		return line;
	}

	//==========================================================================

	private static final ByteBuffer BADCMD_ = stringToBuffer("ERR bad command: ");
	private static final ByteBuffer NOTIMP_ = stringToBuffer("ERR not implemented: ");
	private static final ByteBuffer OK_ = stringToBuffer("OK\r\n");
	private static final ByteBuffer EOF_ = stringToBuffer("EOF\r\n");
	private static final ByteBuffer TRUE_ = stringToBuffer("t\r\n");

	static ByteBuffer badcmd() {
		return BADCMD_.duplicate();
	}
	static ByteBuffer notimp() {
		return NOTIMP_.duplicate();
	}
	static ByteBuffer ok() {
		return OK_.duplicate();
	}
	static ByteBuffer eof() {
		return EOF_.duplicate();
	}
	static ByteBuffer t() {
		return TRUE_.duplicate();
	}

	/**
	 * Skips whitespace then looks for 'type' char followed by digits, starting
	 * at buf's current position. If successful, advances buf's position to past
	 * digits and following whitespace
	 *
	 * @param type
	 * @param buf
	 * @return The digits converted to an int, or -1 if unsuccessful.
	 */
	static int getnum(char type, ByteBuffer buf) {
		int i = buf.position();
		while (i < buf.limit() && Character.isWhitespace(buf.get(i)))
			++i;
		if (i >= buf.limit()
				|| Character.toUpperCase(buf.get(i)) != type
				|| ! (Character.isDigit(buf.get(i + 1)) ||
					((char) buf.get(i + 1) == '-' && Character.isDigit(buf.get(i + 2)))))
			return -1;
		++i;
		StringBuilder sb = new StringBuilder();
		while (i < buf.limit() &&
				(Character.isDigit(buf.get(i)) || buf.get(i) == '-'))
			sb.append((char) buf.get(i++));
		int n = Integer.valueOf(sb.toString());
		while (i < buf.limit() && Character.isWhitespace(buf.get(i)))
			++i;
		buf.position(i);
		return n;
	}

	private static int ck_getnum(char type, ByteBuffer buf) {
		int num = getnum(type, buf);
		if (num == -1)
			throw new SuException("expecting: " + type + "#");
		return num;
	}

	private static DbmsTran getTran(ByteBuffer line) {
		int tn = ck_getnum('T', line);
		return ServerData.forThread().getTransaction(tn);
	}

	private static DbmsQuery q_or_c(ByteBuffer buf) {
		DbmsQuery q = null;
		int n;
		if (-1 != (n = getnum('Q', buf)))
			q = ServerData.forThread().getQuery(n);
		else if (-1 != (n = getnum('C', buf)))
			q = ServerData.forThread().getCursor(n);
		else
			throw new SuException("expecting Q# or C#");
		if (q == null)
			throw new SuException("valid query or cursor required");
		return q;
	}

	private static DbmsQuery q_or_tc(ByteBuffer buf) {
		DbmsQuery q = null;
		int n, tn;
		if (-1 != (n = getnum('Q', buf)))
			q = ServerData.forThread().getQuery(n);
		else if (-1 != (tn = getnum('T', buf)) && -1 != (n = getnum('C', buf))) {
			q = ServerData.forThread().getCursor(n);
			q.setTransaction(ServerData.forThread().getTransaction(tn));
		} else
			throw new SuException("expecting Q# or T# C#");
		if (q == null)
			throw new SuException("valid query or cursor required");
		return q;
	}

	private static Dir getDir(ByteBuffer line) {
		Dir dir;
		switch (line.get()) {
		case '+':
			dir = Dir.NEXT;
			break;
		case '-':
			dir = Dir.PREV;
			break;
		default:
			throw new SuException("get expects + or -");
		}
		line.get(); // skip space
		return dir;
	}

	private static void get(DbmsQuery q, Dir dir, NetworkOutput outputQueue) {
		Row row = q.get(dir);
		if (row == null)
			outputQueue.add(eof());
		else
			row_result(row, q.header(), false, outputQueue);
	}

	private static void row_result(Row row, Header hdr, boolean sendhdr,
			NetworkOutput outputQueue) {
		Record rec = rowToRecord(row, hdr);
		String s = "A" + row.address() + " R" + rec.bufSize();
		if (sendhdr)
			s += ' ' + listToParens(hdr.schema());
		s += "\r\n";
		outputQueue.add(stringToBuffer(s));
		outputQueue.add(rec.getBuffer());
	}

	static Record rowToRecord(Row row, Header hdr) {
		Record rec = row.firstData();
		if (row.size() > 2) {
			RecordBuilder rb = dbpkg.recordBuilder();
			int nFields = 0;
			int nonEmpty = 0;
			for (String f : hdr.fields()) {
				ByteBuffer x = row.getraw(hdr, f);
				nFields++;
				if (x.remaining() != 0)
					nonEmpty = nFields;
				rb.add(x);
			}
			// strip trailing empty fields
			rb.truncate(nonEmpty);
			rec = rb.build();
		}
		return rec.squeeze();
	}

	private static Record makeRecord(ByteBuffer extra) {
		if (Suneido.dbpkg.dbFilename().contains("immu")) {
			ByteBuffer buf = ByteBuffer.allocate(extra.remaining());
			buf.put(extra);
			buf.rewind();
			return dbpkg.record(buf);
		} else
			return dbpkg.record(extra);
	}

	private static ByteBuffer valueResult(NetworkOutput outputQueue, Object result) {
		if (result == null)
			return stringToBuffer("\r\n");
		ByteBuffer buf = Pack.pack(result);
		outputQueue.add(stringToBuffer("P" + buf.remaining() + "\r\n"));
		return buf;
	}

	private static boolean match(String line, String string) {
		if (!line.startsWith(string))
			return false;
		int n = string.length();
		return line.length() == n || line.charAt(n) == ' ';
	}
}
