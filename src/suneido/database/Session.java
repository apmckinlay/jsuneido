package suneido.database;

import java.nio.ByteBuffer;
import java.util.Date;

public class Session {
	public final static int SIZE = 1 + 4;
	public final static byte STARTUP = 0;
	public final static byte SHUTDOWN = 1;

	public static void startup(Mmfile mmf) {
		output(mmf, STARTUP);
	}

	public static void shutdown(Mmfile mmf) {
		output(mmf, SHUTDOWN);
	}

	private static void output(Mmfile mmf, byte type) {
		ByteBuffer buf = mmf.adr(mmf.alloc(SIZE, Mmfile.SESSION));
		buf.put(type);
		buf.putLong(new Date().getTime());
	}
}
