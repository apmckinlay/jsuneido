/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;

public class Errlog {
	private static Supplier<String> extra = () -> "";

	public static void setExtra(Supplier<String> extra) {
		Errlog.extra = extra;
	}

	public static synchronized void errlog(String s) {
		Errlog.errlog(s, null);
	}

	public static synchronized void errlog(String s, Throwable err) {
		System.out.println(s);
		try (FileWriter fw = new FileWriter("error.log", true)) {
			fw.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			fw.append(" ");
			fw.append(extra.get());
			fw.append(s);
			fw.append("\n");
			if (err != null)
				err.printStackTrace(new PrintWriter(fw));
		} catch (IOException e) {
			System.out.println("can't write to error.log " + e);
		}
	}

	public static void fatal(String s) {
		errlog("FATAL: " + s);
		System.exit(-1);
	}

	public static void fatal(String s, Throwable e) {
		errlog("FATAL: " + s + ": " + e, e);
		System.exit(-1);
	}

	public static void uncaught(String s, Throwable e) {
		errlog("UNCAUGHT: " + s + ": " + e, e);
	}

}
