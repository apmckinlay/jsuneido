package suneido.database.server;

import static suneido.Util.bufferToString;
import static suneido.Util.stringToBuffer;

import java.nio.ByteBuffer;

import org.ronsoft.nioserver.OutputQueue;

import suneido.SuException;

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
	QUERY, CURSOR, CLOSE, HEADER, GET, GET1, OUTPUT, UPDATE, ERASE, ORDER, KEYS, LIBGET,
	LIBRARIES,
	EXPLAIN,
	REWIND,
	TIMESTAMP,
	DUMP,
	COPY,
	RUN,
	TEXT,
	BINARY,
	TRANLIST,
	SIZE,
	CONNECTIONS,
	RECORDOK,
	TEMPDEST,
	CURSORS,
	SESSIONID,
	REFRESH,
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

	static int ck_getnum(char type, ByteBuffer buf) {
		int num = getnum(type, buf);
		if (num == -1)
			throw new SuException("expecting: " + type + "#");
		return num;
	}
}
