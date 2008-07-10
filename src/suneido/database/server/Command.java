package suneido.database.server;

import static suneido.Util.bufferToString;

import java.nio.ByteBuffer;

import org.ronsoft.nioserver.OutputQueue;

import suneido.database.query.Request;

/**
 * Implements the server protocol commands.
 * @author Andrew McKinlay
 * <p><small>Copyright (c) 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public enum Command {
	BADCMD {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			badcmd.position(0);
			outputQueue.enqueue(badcmd);
			return line;
		}
	},
	LIBGET, GET,
	GET1,
	OUTPUT,
	UPDATE,
	HEADER,
	ORDER,
	KEYS,
	TRANSACTION,
	CURSOR,
	CLOSE,
	QUERY,
	REQUEST,
	ADMIN {
		@Override
		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				OutputQueue outputQueue) {
			Request.execute(bufferToString(line));
			return OK;
		}
	},
	LIBRARIES,
	EXPLAIN,
	REWIND,
	ERASE,
	COMMIT,
	ABORT,
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
	public ByteBuffer execute(ByteBuffer line, ByteBuffer extra, OutputQueue outputQueue) {
		return line; //TODO just echo for now
	}
	private final static ByteBuffer badcmd =
		ByteBuffer.wrap("ERR bad command: ".getBytes());
	private final static ByteBuffer OK = ByteBuffer.wrap("OK\r\n".getBytes());
}
