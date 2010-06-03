package suneido.database.server;

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
import suneido.language.Ops;
import suneido.language.Pack;
import suneido.util.NetworkOutput;

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
			theDbms.admin(ServerData.forThread(), bufferToString(line));
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
			SuContainer connections = new SuContainer();
			synchronized(DbmsServerBySocket.serverDataSet) {
				for (ServerData serverData : DbmsServerBySocket.serverDataSet)
					connections.append(serverData.getSessionId());
			}
			return valueResult(outputQueue, connections);
		}
	},
	COPY {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			theDbms.copy(bufferToString(line).trim());
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
			DbmsQuery dq = theDbms.cursor(ServerData.forThread(),
					bufferToString(extra));
			int cn = ServerData.forThread().addCursor(dq);
			return stringToBuffer("C" + cn + "\r\n");
		}
	},
	CURSORS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer("N" + theDbms.cursors() + "\r\n");
		}
	},
	DUMP {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			theDbms.dump(bufferToString(line).trim());
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
			DbmsTran tran =
					ServerData.forThread().getTransaction(ck_getnum('T', line));
			long recadr = Mmfile.intToOffset(ck_getnum('A', line));
			theDbms.erase(tran, recadr);
			return ok();
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
			return stringToBuffer("N" + theDbms.finalSize() + "\r\n");
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
			int tn = ck_getnum('T', line);
			HeaderAndRow hr = theDbms.get(ServerData.forThread(), dir,
					bufferToString(extra), one,
					ServerData.forThread().getTransaction(tn));
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
			synchronized(DbmsServerBySocket.serverDataSet) {
				Iterator<ServerData> iter = DbmsServerBySocket.serverDataSet.iterator();
				while (iter.hasNext()) {
					ServerData serverData = iter.next();
					if (sessionId.equals(serverData.getSessionId())) {
						++nkilled;
						serverData.end();
//						serverData.outputQueue.closeChannel();
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
				NetworkOutput outputQueue) {
			return stringToBuffer(listToParens(theDbms.libraries()) + "\r\n");
		}
	},
	LOG {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			theDbms.log(bufferToString(line).trim());
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
			// System.out.println("\t" + new Record(extra));
			q.output(new Record(extra));
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
			DbmsQuery dq = theDbms.query(ServerData.forThread(), tran,
					bufferToString(extra));
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
			DbmsTran tran =
					ServerData.forThread().getTransaction(ck_getnum('T', line));
			int n = theDbms.request(ServerData.forThread(), tran,
					bufferToString(extra));
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
			Object result = theDbms.run(bufferToString(line));
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
					theDbms.sessionid(bufferToString(line).trim()) + "\r\n");
		}
	},
	SIZE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer("S" + Mmfile.offsetToInt(theDbms.size())
					+ "\r\n");
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
			return stringToBuffer(Ops.display(theDbms.timestamp()) + "\r\n");
		}
	},
	TRANLIST {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				NetworkOutput outputQueue) {
			return stringToBuffer(listToParens(theDbms.tranlist()) + "\r\n");
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
					theDbms.transaction(readwrite));
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
			DbmsTran tran =
					ServerData.forThread().getTransaction(ck_getnum('T', line));
			long recadr = Mmfile.intToOffset(ck_getnum('A', line));
			// System.out.println("\t" + new Record(extra));
			recadr = theDbms.update(tran, recadr, new Record(extra));
			return stringToBuffer("U" + Mmfile.offsetToInt(recadr) + "\r\n");
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

	private static void get(DbmsQuery q, Dir dir, NetworkOutput outputQueue) {
		Row row = q.get(dir);
		if (row != null && q.updateable())
			row.recadr = row.getFirstData().off();
		Header hdr = q.header();
		row_result(row, hdr, false, outputQueue);
	}

	private static void row_result(Row row, Header hdr, boolean sendhdr,
			NetworkOutput outputQueue) {
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

	private static ByteBuffer valueResult(NetworkOutput outputQueue, Object result) {
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
