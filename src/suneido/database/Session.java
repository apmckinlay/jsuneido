package suneido.database;

import static suneido.SuException.verify;

import java.text.SimpleDateFormat;
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
		verify(check_shutdown(mmf));
	}

	private static void output(Destination mmf, byte type) {
		ByteBuf buf = mmf.adr(mmf.alloc(SIZE, Mmfile.SESSION));
		buf.put(0, type);
		buf.putLong(1, new Date().getTime());
	}

	public static boolean check_shutdown(Destination mmf) {
		final long last = mmf.last();
		if (mmf.type(last) != Mmfile.SESSION)
			return false;
		ByteBuf buf = mmf.adr(last);
		if (buf.get(0) != Session.SHUTDOWN)
			return false;
		return true;
	}

	private final ByteBuf buf;

	public Session(ByteBuf buf) {
		this.buf = buf;
	}

	public long getType() {
		return buf.get(0);
	}

	public long getDate() {
		return buf.getLong(1);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Session ")
				.append(getType() == STARTUP ? "startup "
						: getType() == SHUTDOWN ? "shutdown " : "unknown-type ")
				.append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(getDate()));
		return sb.toString();
	}
}
