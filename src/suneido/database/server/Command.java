package suneido.database.server;

import static suneido.util.Util.*;

import java.nio.ByteBuffer;
import java.util.List;

import org.ronsoft.nioserver.OutputQueue;

import suneido.SuException;
import suneido.database.*;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.database.server.Dbms.LibGet;

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
				OutputQueue outputQueue, ServerData serverData) {
			badcmd.rewind();
			outputQueue.enqueue(badcmd);
			return line;
		}
	},
	NILCMD {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			return null;
		}

	},
	ADMIN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			theDbms.admin(serverData, bufferToString(line));
			return TRUE;
		}
	},
	TRANSACTION {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			String s = bufferToString(line).trim().toLowerCase();
			boolean readwrite = false;
			if (match(s, "update"))
				readwrite = true;
			else if (!match(s, "read"))
				return stringToBuffer("ERR invalid transaction mode: " + s
						+ "\r\n");
			String session_id = ""; // TODO
			int tn = serverData.addTransaction(
					theDbms.transaction(readwrite, session_id));
			return stringToBuffer("T" + tn + "\r\n");
		}
	},
	COMMIT {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = serverData.getTransaction(tn);
			serverData.endTransaction(tn);
			String conflict = tran.complete();
			return conflict == null ? OK : stringToBuffer(conflict + "\r\n");
		}
	},
	ABORT {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = serverData.getTransaction(tn);
			serverData.endTransaction(tn);
			tran.abort();
			return OK;
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
				OutputQueue outputQueue, ServerData serverData) {
			DbmsTran tran = serverData.getTransaction(ck_getnum('T', line));
			int n = theDbms.request(serverData, tran, bufferToString(extra));
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
				OutputQueue outputQueue, ServerData serverData) {
			int tn = ck_getnum('T', line);
			DbmsTran tran = serverData.getTransaction(tn);
			Query dq = (Query) theDbms.query(serverData, tran, bufferToString(extra));
			int qn = serverData.addQuery(tn, dq);
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
				OutputQueue outputQueue, ServerData serverData) {
			Query dq = (Query) theDbms.cursor(serverData, bufferToString(extra));
			int cn = serverData.addCursor(dq);
			return stringToBuffer("C" + cn + "\r\n");
		}
	},
	CLOSE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			int n;
			if (-1 != (n = getnum('Q', line)))
				serverData.endQuery(n);
			else if (-1 != (n = getnum('C', line)))
				serverData.endCursor(n);
			else
				throw new SuException("CLOSE expects Q# or C#");
			return OK;
		}
	},

	HEADER {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			DbmsQuery q = q_or_c(line, serverData);
			return stringToBuffer(listToParens(q.header().schema()) + "\r\n");
		}
	},
	ORDER {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			DbmsQuery q = q_or_c(line, serverData);
			return stringToBuffer(listToParens(q.ordering()) + "\r\n");
		}
	},
	KEYS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			DbmsQuery q = q_or_c(line, serverData);
			return stringToBuffer(listToParens(q.keys()) + "\r\n");
		}
	},
	EXPLAIN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			DbmsQuery q = q_or_c(line, serverData);
			return stringToBuffer(q.toString() + "\r\n");
		}
	},
	REWIND {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			DbmsQuery q = q_or_c(line, serverData);
			q.rewind();
			return OK;
		}
	},

	GET {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			Dir dir = getDir(line);
			Query q = q_or_tc(line, serverData);
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
				OutputQueue outputQueue, ServerData serverData) {
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
			HeaderAndRow hr = theDbms.get(serverData, dir, bufferToString(extra), one,
					serverData.getTransaction(tn));
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
				OutputQueue outputQueue, ServerData serverData) {
			Query q = q_or_tc(line, serverData);
			// System.out.println("\t" + new Record(extra));
			q.output(new Record(extra));
			return TRUE;
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
				OutputQueue outputQueue, ServerData serverData) {
			DbmsTran tran = serverData.getTransaction(ck_getnum('T', line));
			long recadr = Mmfile.intToOffset(ck_getnum('A', line));
			// System.out.println("\t" + new Record(extra));
			recadr = theDbms.update(tran, recadr, new Record(extra));
			return stringToBuffer("U" + Mmfile.offsetToInt(recadr) + "\r\n");
		}
	},
	ERASE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			DbmsTran tran = serverData.getTransaction(ck_getnum('T', line));
			long recadr = Mmfile.intToOffset(ck_getnum('A', line));
			theDbms.erase(tran, recadr);
			return OK;
		}
	},

	LIBGET {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			List<LibGet> srcs = theDbms.libget(bufferToString(line).trim());

			String resp = "";
			for (LibGet src : srcs)
				resp += "L" + (src.text.limit()) + " ";
			resp += "\r\n";
			outputQueue.enqueue(stringToBuffer(resp));

			for (LibGet src : srcs) {
				outputQueue.enqueue(stringToBuffer(src.library + "\r\n"));
				outputQueue.enqueue(src.text);
			}
			return null;
		}
	},
	LIBRARIES {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			return stringToBuffer(listToParens(theDbms.libraries()) + "\r\n");
		}
	},
	TIMESTAMP {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			return stringToBuffer(theDbms.timestamp().toString() + "\r\n");
		}
	},
	DUMP,
	COPY,
	RUN,
	TEXT,
	BINARY {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			// TODO BINARY
			return OK;
		}
	},
	TRANLIST {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			return stringToBuffer(listToParens(theDbms.tranlist()) + "\r\n");
		}
	},
	SIZE {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			return stringToBuffer("S" + Mmfile.offsetToInt(theDbms.size())
					+ "\r\n");
		}
	},
	TEMPDEST {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			return stringToBuffer("D0\r\n");
		}
	},
	CONNECTIONS,
	CURSORS {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			return stringToBuffer("N" + theDbms.cursors() + "\r\n");
		}
	},
	SESSIONID {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			// TODO SESSIONID
			// outputQueue.enqueue(line);
			return line;
		}
	},
	FINAL,
	LOG {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			// TODO LOG
			return OK;
		}
	},
	KILL;

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
	 * @param serverData
	 * @return
	 */
	public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
			OutputQueue outputQueue, ServerData serverData) {
		notimp.rewind();
		outputQueue.enqueue(notimp);
		return line;
	}
	private final static ByteBuffer badcmd = stringToBuffer("ERR bad command: ");
	private final static ByteBuffer notimp = stringToBuffer("ERR not implemented: ");
	private final static ByteBuffer OK = stringToBuffer("OK\r\n");
	private final static ByteBuffer EOF = stringToBuffer("EOF\r\n");
	private final static ByteBuffer TRUE = stringToBuffer("t\r\n");

	public final static Dbms theDbms = new DbmsLocal();

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
		String s = "";
		while (i < buf.limit() && Character.isDigit(buf.get(i)))
			s += (char) buf.get(i++);
		int n = Integer.valueOf(s);
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

	private static Query q_or_c(ByteBuffer buf, ServerData serverData) {
		Query q = null;
		int n;
		if (-1 != (n = getnum('Q', buf)))
			q = serverData.getQuery(n);
		else if (-1 != (n = getnum('C', buf)))
			q = serverData.getCursor(n);
		else
			throw new SuException("expecting Q# or C#");
		if (q == null)
			throw new SuException("valid query or cursor required");
		return q;
	}

	private static Query q_or_tc(ByteBuffer buf, ServerData serverData) {
		Query q = null;
		int n, tn;
		if (-1 != (n = getnum('Q', buf)))
			q = serverData.getQuery(n);
		else if (-1 != (tn = getnum('T', buf)) && -1 != (n = getnum('C', buf))) {
			q = serverData.getCursor(n);
			q.setTransaction((Transaction) serverData.getTransaction(tn));
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

	private static void get(Query q, Dir dir, OutputQueue outputQueue) {
		Row row = q.get(dir);
		if (row != null && q.updateable())
			row.recadr = row.getFirstData().off();
		Header hdr = q.header();
		row_result(row, hdr, false, outputQueue);
	}

	private static void row_result(Row row, Header hdr, boolean sendhdr, OutputQueue outputQueue) {
		if (row == null) {
			EOF.rewind();
			outputQueue.enqueue(EOF);
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
		outputQueue.enqueue(stringToBuffer(s));

		outputQueue.enqueue(rec.getBuf());
	}


	private static boolean match(String line, String string) {
		if (!line.startsWith(string))
			return false;
		int n = string.length();
		return line.length() == n || line.charAt(n) == ' ';
		// TODO advance line position
	}
}
