package suneido.database.server;

import static suneido.Util.*;

import java.nio.ByteBuffer;

import org.ronsoft.nioserver.OutputQueue;

import suneido.SuException;
import suneido.database.*;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;

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
			badcmd.position(0);
			outputQueue.enqueue(badcmd);
			return line;
		}
	},
	ADMIN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			theDbms.admin(bufferToString(line));
			return OK;
		}
	},
	TRANSACTION {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			boolean readwrite = false; // TODO
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
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue, ServerData serverData) {
			DbmsTran tran = serverData.getTransaction(ck_getnum('T', line));
			theDbms.request(tran, bufferToString(line));
			return OK;
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
			line.rewind();
			int tn = ck_getnum('T', line);
			DbmsTran tran = serverData.getTransaction(tn);
			DbmsQuery dq = theDbms.query(tran, bufferToString(extra));
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
			DbmsQuery dq = theDbms.cursor(bufferToString(extra));
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
			DbmsQuery q = q_or_tc(line, serverData);
			return get(q, dir, outputQueue);
		}
	},
	GET1,


	OUTPUT,
	UPDATE,
	ERASE,

	LIBGET,
	LIBRARIES,
	TIMESTAMP,
	DUMP,
	COPY,
	RUN,
	TEXT,
	BINARY,
	TRANLIST,
	SIZE,
	CONNECTIONS,
	CURSORS,
	SESSIONID,
	FINAL,
	LOG,
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
		return line; //TODO just echo for now
	}
	private final static ByteBuffer badcmd = stringToBuffer("ERR bad command: ");
	private final static ByteBuffer OK = stringToBuffer("OK\r\n");

	static Dbms theDbms = new DbmsLocal();

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

	private static DbmsQuery q_or_c(ByteBuffer buf, ServerData serverData) {
		DbmsQuery q = null;
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

	private static DbmsQuery q_or_tc(ByteBuffer buf, ServerData serverData) {
		DbmsQuery q = null;
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

	private static ByteBuffer get(DbmsQuery q, Dir dir, OutputQueue outputQueue) {
		Row row = q.get(dir);
		Header hdr = q.header();
		row_result(row, hdr, false, outputQueue);
		return null;
	}

	private static void row_result(Row row, Header hdr, boolean sendhdr, OutputQueue outputQueue) {
		Record rec;
		if (row.size() <= 2)
			rec = row.getFirstData();
		else
			{
			rec = new Record(1000);
			for (String f : hdr.fields())
				rec.add(row.getraw(hdr, f));

			// strip trailing empty fields
			int n = rec.size();
			while (rec.getraw(n - 1).remaining() == 0)
				--n;
			rec.truncate(n);
			}

		String s = "A" + Mmfile.offsetToInt(row.recadr) + " R" + rec.packSize();
		if (sendhdr)
			s += ' ' + listToParens(hdr.schema());
		s += "\r\n";
		outputQueue.enqueue(stringToBuffer(s));

		rec = rec.dup(); // compact
		outputQueue.enqueue(rec.getBuf());
	}
}
