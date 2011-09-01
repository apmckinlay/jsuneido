/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static suneido.util.Verify.verify;

import java.text.SimpleDateFormat;
import java.util.Date;

import suneido.util.ByteBuf;

class Session {
	static final int SIZE = 1 + 8;
	static final byte STARTUP = 0;
	static final byte SHUTDOWN = 1;

	static void startup(Destination mmf) {
		output(mmf, STARTUP);
	}

	static void shutdown(Destination mmf) {
		output(mmf, SHUTDOWN);
		verify(check_shutdown(mmf));
	}

	private static void output(Destination mmf, byte type) {
		ByteBuf buf = mmf.adr(mmf.alloc(SIZE, Mmfile.SESSION));
		buf.put(0, type);
		buf.putLong(1, new Date().getTime());
	}

	static boolean check_shutdown(Destination mmf) {
		return mmf.checkEnd(Mmfile.SESSION, Session.SHUTDOWN);
	}

	private final ByteBuf buf;

	Session(ByteBuf buf) {
		this.buf = buf;
	}

	long getType() {
		return buf.get(0);
	}

	long getDate() {
		return buf.getLong(1);
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append("Session ")
				.append(getType() == STARTUP ? "startup "
						: getType() == SHUTDOWN ? "shutdown " : "unknown-type ")
				.append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(getDate()))
				.toString();
	}
}
