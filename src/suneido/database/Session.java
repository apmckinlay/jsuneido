package suneido.database;

import java.util.Date;

import suneido.util.ByteBuf;

public class Session {
	public final static int SIZE = 1 + 8;
	public final static byte STARTUP = 0;
	public final static byte SHUTDOWN = 1;

	public static void startup(Destination mmf) {
		output(mmf, STARTUP);
	}

	public static void shutdown(Destination mmf) {
		output(mmf, SHUTDOWN);
	}

	private static void output(Destination mmf, byte type) {
		ByteBuf buf = mmf.adr(mmf.alloc(SIZE, Mmfile.SESSION));
		buf.put(0, type);
		buf.putLong(1, new Date().getTime());
	}
}
