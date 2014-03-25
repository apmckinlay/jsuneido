/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.FileWriter;
import java.io.IOException;

public class Trace {
	public enum Type {
		NONE(0), FUNCTIONS(1 << 0), STATEMENTS(1 << 1), OPCODES(1 << 2),
		STACK(1 << 3), LIBLOAD(1 << 4), SLOWQUERY(1 << 5), QUERY(1 << 6),
		SYMBOL(1 << 7), ALLINDEX(1 << 8), TABLE(1 << 9), SELECT(1 << 10),
		TEMPINDEX(1 << 11), QUERYOPT(1 << 12), CONSOLE(1 << 13), LOGFILE(1 << 14),
		CLIENTSERVER(1 << 15), EXCEPTIONS(1 << 16), GLOBALS(1 << 17);
		public final int bit;
		Type(int bit) {
			this.bit = bit;
		}
	}
	public static int flags = 0;
	private static FileWriter fw;

	public static void trace(Type type, String s) {
//		if ((flags & type.bit) != 0)
//			println(type + " " + s);
	}

	public static void println(String s) {
		if ((flags & Type.CONSOLE.bit) != 0)
			System.out.println(s);
		if ((flags & Type.LOGFILE.bit) != 0) {
			try {
				if (fw == null)
					fw = new FileWriter("trace.log");
				fw.append(s);
				fw.append('\n');
				fw.flush();
			} catch (IOException e) {
				System.out.println("Trace: " + e);
			}
		}
	}
}
