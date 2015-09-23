/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import suneido.debug.CallstackProvider;

public class Errlog {
	private static Supplier<String> extra = () -> "";
	private static AtomicInteger count = new AtomicInteger(); // for tests

	public static void setExtra(Supplier<String> extra) {
		Errlog.extra = extra;
	}

	public static void bare(String s) {
		log("", s, null);
	}

	public static void info(String s) {
		log("INFO", s, null);
	}

	public static void warn(String s) {
		log("WARNING", s, null);
	}

	public static void error(String s) {
		error(s, null);
	}

	public static void error(String s, Throwable e) {
		log("ERROR", s, e);
	}

	public static void fatal(String s) {
		fatal(s, null);
	}

	public static void fatal(String s, Throwable e) {
		log("FATAL ERROR", s, e);
		System.exit(-1);
	}

	/** like assert but just logs, doesn't throw */
	public static void verify(boolean arg, String msg) {
		if (! arg)
			Errlog.error(msg);
	}

	/** run the given function, catching and logging any errors */
	public static void run(Runnable fn) {
		try {
			fn.run();
		} catch (Throwable e) {
			Errlog.error("", e);
		}
	}

	private static synchronized void log(String prefix, String s, Throwable e) {
		if (! prefix.isEmpty())
			prefix = prefix + ": ";
		count.incrementAndGet();
		System.out.println(prefix +
				s + (s.isEmpty() ? "" : " ") +
				(e == null ? "" : e));
		try (FileWriter fw = new FileWriter("error.log", true)) {
			fw.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
				.append(" ")
				.append(extra.get())
				.append(prefix + s)
				.append("\n");
			if (e != null) {
				PrintWriter out = new PrintWriter(fw);
				if (e instanceof CallstackProvider)
					((CallstackProvider)e).printCallstack(out);
				else
					e.printStackTrace(out);
			}
		} catch (IOException e2) {
			System.err.println("can't write to error.log " + e2);
		}
	}

	/** for tests */
	public static int count() {
		return count.get();
	}

}
