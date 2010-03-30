package suneido.database.server;

import static suneido.Suneido.errlog;
import static suneido.util.Util.*;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import suneido.SuContainer;
import suneido.SuException;
import suneido.database.*;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.database.server.Dbms.LibGet;
import suneido.language.*;
import suneido.language.Compiler;
import suneido.util.SocketServer.OutputQueue;

/**
 * Implements the server protocol commands.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright (c) 2008 Suneido Software Corp.
 * All rights reserved. Licensed under GPLv2.</small></p>
 */
public enum Command {
	BADCMD {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			outputQueue.add(badcmd());
			return line;
		}
	},
	NILCMD {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			return null;
		}

	},
	ADMIN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			theDbms.admin(ServerData.forThread(), bufferToString(line));
			return t();
		}
	},
	TRANSACTION {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			String s = bufferToString(line).trim().toLowerCase();
			boolean readwrite = false;
			if (match(s, "update"))
				readwrite = true;
			else if (!match(s, "read"))
				return stringToBuffer("ERR invalid transaction mode: " + s
						+ "\r\n");
			// MAYBE associate session id with transaction
			int tn = ServerData.forThread().addTransaction(
					theDbms.transaction(readwrite));
			return stringToBuffer("T" + tn + "\r\n");
		}
	},
	COMMIT {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			ServerData.forThread().endTransaction(tn);
			String conflict = tran.complete();
			return conflict == null ? ok() : stringToBuffer(conflict + "\r\n");
		}
	},
	ABORT {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			ServerData.forThread().endTransaction(tn);
			tran.abort();
			return ok();
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
				OutputQueue outputQueue) {
			DbmsTran tran =
					ServerData.forThread().getTransaction(ck_getnum('T', line));
			int n =
					theDbms.request(ServerData.forThread(), tran,
							bufferToString(extra));
			return stringToBuffer("R" + n + "\r\n");
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
				OutputQueue outputQueue) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = ServerData.forThread().getTransaction(tn);
			DbmsQuery dq = theDbms.query(ServerData.forThread(), tran,
					bufferToString(extra));
			int qn = ServerData.forThread().addQuery(tn, dq);
			return stringToBuffer("Q" + qn + "\r\n");
		}
	},
	CURSOR {
		@Override
		public int extra(ByteBuffer buf) {
			return ck_getnum('Q', buf);
		}

		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			DbmsQuery dq = theDbms.cursor(ServerData.forThread(),
					bufferToString(extra));
			int cn = ServerData.forThread().addCursor(dq);
			return stringToBuffer("C" + cn + "\r\n");
		}
	},
	CLOSE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
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

	HEADER {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			DbmsQuery q = q_or_c(line);
			return stringToBuffer(listToParens(q.header().schema()) + "\r\n");
		}
	},
	ORDER {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			DbmsQuery q = q_or_c(line);
			return stringToBuffer(listToParens(q.ordering()) + "\r\n");
		}
	},
	KEYS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			DbmsQuery q = q_or_c(line);
			return stringToBuffer(listToParens(q.keys()) + "\r\n");
		}
	},
	EXPLAIN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			DbmsQuery q = q_or_c(line);
			return stringToBuffer(q.toString() + "\r\n");
		}
	},
	REWIND {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			DbmsQuery q = q_or_c(line);
			q.rewind();
			return ok();
		}
	},

	GET {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
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
				OutputQueue outputQueue) {
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
			line.get(); // skip space
			int tn = ck_getnum('T', line);
			HeaderAndRow hr = theDbms.get(ServerData.forThread(), dir,
					bufferToString(extra), one,
					ServerData.forThread().getTransaction(tn));
			row_result(hr.row, hr.header, true, outputQueue);
			return null;
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
				OutputQueue outputQueue) {
			DbmsQuery q = q_or_tc(line);
			// System.out.println("\t" + new Record(extra));
			q.output(new Record(extra));
			return t();
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
				OutputQueue outputQueue) {
			DbmsTran tran =
					ServerData.forThread().getTransaction(ck_getnum('T', line));
			long recadr = Mmfile.intToOffset(ck_getnum('A', line));
			// System.out.println("\t" + new Record(extra));
			recadr = theDbms.update(tran, recadr, new Record(extra));
			return stringToBuffer("U" + Mmfile.offsetToInt(recadr) + "\r\n");
		}
	},
	ERASE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			DbmsTran tran =
					ServerData.forThread().getTransaction(ck_getnum('T', line));
			long recadr = Mmfile.intToOffset(ck_getnum('A', line));
			theDbms.erase(tran, recadr);
			return ok();
		}
	},

	LIBGET {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			List<LibGet> srcs = theDbms.libget(bufferToString(line).trim());
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
				OutputQueue outputQueue) {
			return stringToBuffer(listToParens(theDbms.libraries()) + "\r\n");
		}
	},
	TIMESTAMP {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			return stringToBuffer(Ops.display(theDbms.timestamp()) + "\r\n");
		}
	},
	DUMP {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			theDbms.dump(bufferToString(line).trim());
			return ok();
		}
	},
	COPY, // TODO COPY
	TEXT, // not supported
	RUN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			Object result = Compiler.eval(bufferToString(line));
			if (result == null)
				return stringToBuffer("\r\n");
			return valueResult(outputQueue, result);
		}
	},
	BINARY {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			// jSuneido only supports binary
			return ok();
		}
	},
	TRANLIST {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			return stringToBuffer(listToParens(theDbms.tranlist()) + "\r\n");
		}
	},
	SIZE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			return stringToBuffer("S" + Mmfile.offsetToInt(theDbms.size())
					+ "\r\n");
		}
	},
	TEMPDEST {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			return stringToBuffer("D0\r\n");
		}
	},
	CONNECTIONS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			SuContainer connections = new SuContainer();
			synchronized(DbmsServer.serverDataSet) {
				for (ServerData serverData : DbmsServer.serverDataSet)
					connections.append(serverData.getSessionId());
			}
			return valueResult(outputQueue, connections);
		}
	},
	CURSORS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			return stringToBuffer("N" + theDbms.cursors() + "\r\n");
		}
	},
	SESSIONID {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			String sessionId = bufferToString(line).trim();
			ServerData serverData = ServerData.forThread();
			if (!sessionId.equals(""))
				serverData.setSessionId(sessionId);
			return stringToBuffer(serverData.getSessionId() + "\r\n");
		}
	},
	FINAL,
	LOG {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			String sessionId = ServerData.forThread().getSessionId();
			errlog(sessionId + ": " + bufferToString(line).trim());
			return ok();
		}
	},
	KILL {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			int nkilled = 0;
			String sessionId = bufferToString(line).trim();
			synchronized(DbmsServer.serverDataSet) {
				Iterator<ServerData> iter = DbmsServer.serverDataSet.iterator();
				while (iter.hasNext()) {
					ServerData serverData = iter.next();
					if (sessionId.equals(serverData.getSessionId())) {
						++nkilled;
						serverData.end();
						serverData.outputQueue.closeChannel();
						iter.remove();
					}
				}
			}
			return stringToBuffer("N" + nkilled + "\r\n");
		}
	};

	/**
	 * @param buf A ByteBuffer containing the command line.
	 * @return The amount of "extra" data required by the command in the buffer.
	 */
	public int extra(ByteBuffer buf) {
		return 0;
	}

	/**
	 * @param line
	 *            Current position is past the first (command) word.
	 * @param extra
	 * @param outputQueue
	 * @param TheServerData
	 *            .get()
	 * @return
	 */
	public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
			OutputQueue outputQueue) {
		outputQueue.add(notimp());
		return line;
	}
	private final static ByteBuffer BADCMD_ = stringToBuffer("ERR bad command: ");
	private final static ByteBuffer NOTIMP_ = stringToBuffer("ERR not implemented: ");
	private final static ByteBuffer OK_ = stringToBuffer("OK\r\n");
	private final static ByteBuffer EOF_ = stringToBuffer("EOF\r\n");
	private final static ByteBuffer TRUE_ = stringToBuffer("t\r\n");

	public final static Dbms theDbms = new DbmsLocal();

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
				|| !Character.isDigit(buf.get(i + 1)))
			return -1;
		++i;
		StringBuilder sb = new StringBuilder();
		while (i < buf.limit() && Character.isDigit(buf.get(i)))
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
			q.setTransaction((Transaction) ServerData.forThread().getTransaction(tn));
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

	private static void get(DbmsQuery q, Dir dir, OutputQueue outputQueue) {
		Row row = q.get(dir);
		if (row != null && q.updateable())
			row.recadr = row.getFirstData().off();
		Header hdr = q.header();
		row_result(row, hdr, false, outputQueue);
	}

	private static void row_result(Row row, Header hdr, boolean sendhdr,
			OutputQueue outputQueue) {
		if (row == null) {
			outputQueue.add(eof());
			return;
		}
		Record rec = row.getFirstData();
		if (row.size() > 2) {
			rec = new Record(1000);
			for (String f : hdr.fields())
				rec.add(row.getraw(hdr, f));

			// strip trailing empty fields
			int n = rec.size();
			while (rec.getraw(n - 1).remaining() == 0)
				--n;
			rec.truncate(n);
		}

		rec = rec.dup();
		String s = "A" + Mmfile.offsetToInt(row.recadr) + " R" + rec.bufSize();
		if (sendhdr)
			s += ' ' + listToParens(hdr.schema());
		s += "\r\n";
		outputQueue.add(stringToBuffer(s));

		outputQueue.add(rec.getBuffer());
	}

	private static ByteBuffer valueResult(OutputQueue outputQueue, Object result) {
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
