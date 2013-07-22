/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import suneido.database.server.DbmsServer;
import suneido.intfc.database.Database;
import suneido.intfc.database.DatabasePackage;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.Contexts;
import suneido.util.Print;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class Suneido {
	public static DatabasePackage dbpkg = suneido.immudb.DatabasePackage.dbpkg;
	private static final ThreadFactory threadFactory =
		new ThreadFactoryBuilder()
			.setDaemon(true)
			.setNameFormat("suneido-thread-%d")
			.build();
	private static final ScheduledExecutorService scheduler
			= Executors.newSingleThreadScheduledExecutor(threadFactory);
	public static CommandLineOptions cmdlineoptions =
			CommandLineOptions.parse(); // for tests
	public static Contexts contexts = new Contexts();
	public static ContextLayered context = new ContextLayered(contexts);

	public static void main(String[] args) {
		ClassLoader.getSystemClassLoader().setPackageAssertionStatus("suneido", true);
		cmdlineoptions = CommandLineOptions.parse(args);
		if (cmdlineoptions.max_update_tran_sec != 0)
			dbpkg.setOption("max_update_tran_sec", cmdlineoptions.max_update_tran_sec);
		if (cmdlineoptions.max_writes_per_tran != 0)
			dbpkg.setOption("max_writes_per_tran", cmdlineoptions.max_writes_per_tran);
		try {
			doAction();
		} catch (Throwable e) {
			fatal(cmdlineoptions.action + " FAILED", e);
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

	public static synchronized void errlog(String s) {
		errlog(s, null);
	}

	public static synchronized void errlog(String s, Throwable err) {
		System.out.println(s);
		try {
			FileWriter fw = new FileWriter("error.log", true);
			fw.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			fw.append(" ");
			if (TheDbms.dbms() != null) {
				String sessionid = TheDbms.dbms().sessionid();
				if (! "127.0.0.1".equals(sessionid))
					fw.append(sessionid).append(" ");
			}
			fw.append(s);
			fw.append("\n");
			if (err != null)
				err.printStackTrace(new PrintWriter(fw));
			fw.close();
		} catch (IOException e) {
			System.out.println("can't write to error.log " + e);
		}
	}

	private static void doAction() throws Throwable {
		String dbFilename = dbpkg.dbFilename();
		switch (cmdlineoptions.action) {
		case REPL:
			Suneido.openDbms();
			Repl.repl();
			break;
		case SERVER:
			if (! System.getProperty("java.vm.name").contains("Server VM"))
				System.out.println("WARNING: Server VM is recommended");
			Print.timestamped("starting server");
			startServer();
			break;
		case CLIENT:
			TheDbms.remote(cmdlineoptions.actionArg, cmdlineoptions.serverPort);
			scheduleAtFixedRate(TheDbms.closer, 30, TimeUnit.SECONDS);
			if ("".equals(cmdlineoptions.remainder))
				Repl.repl();
			else
				Compiler.eval("JInit()");
			break;
		case DUMP:
			String dumptablename = cmdlineoptions.actionArg;
			if (dumptablename == null)
				DbTools.dumpDatabasePrint(dbpkg, dbFilename, "database.su");
			else
				DbTools.dumpTablePrint(dbpkg, dbFilename, dumptablename);
			break;
		case LOAD:
			String loadtablename = cmdlineoptions.actionArg;
			if (loadtablename != null)
				DbTools.loadTablePrint(dbpkg, dbFilename, loadtablename);
			else
				DbTools.loadDatabasePrint(dbpkg, dbFilename, "database.su");
			break;
		case LOAD2:
			DbTools.load2(dbpkg, cmdlineoptions.actionArg);
			break;
		case CHECK:
			DbTools.checkPrintExit(dbpkg, cmdlineoptions.actionArg == null
					? dbFilename : cmdlineoptions.actionArg);
			break;
		case REBUILD:
			DbTools.rebuildOrExit(dbpkg, dbFilename);
			break;
		case REBUILD2:
			DbTools.rebuild2(dbpkg, cmdlineoptions.actionArg);
			break;
		case COMPACT:
			DbTools.compactPrintExit(dbpkg, dbFilename);
			break;
		case COMPACT2:
			DbTools.compact2(dbpkg, cmdlineoptions.actionArg);
			break;
		case VERSION:
			System.out.println("jSuneido " + WhenBuilt.when());
			System.out.println("Java " + System.getProperty("java.version")
					+ System.getProperty("java.vm.name").replace("Java", ""));
			break;
		case ERROR:
			System.out.println(cmdlineoptions.actionArg);
			System.out.println();
			printHelp();
			break;
		case HELP:
			printHelp();
			break;
		default:
			throw SuException.unreachable();
		}
	}

	private static void startServer() {
		HttpServerMonitor.run(cmdlineoptions.serverPort + 1);
		openDbms();
		try {
			Compiler.eval("JInit()");
		} catch (Throwable e) {
			fatal("error during init", e);
		}
		DbmsServer.run(cmdlineoptions.serverPort, cmdlineoptions.timeoutMin);
	}

	private static Database db;

	public static void openDbms() {
		db = dbpkg.open(dbpkg.dbFilename());
		if (db == null) {
			errlog("database corrupt, rebuilding");
			tryToCloseMemoryMappings();
			DbTools.rebuildOrExit(dbpkg, dbpkg.dbFilename());
			db = dbpkg.open(dbpkg.dbFilename());
			if (db == null)
				fatal("could not open database after rebuild");
		}
		TheDbms.set(db);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// db.check();
				db.close();
			}
		});
		scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						db.limitOutstandingTransactions();
					}
				}, 1, TimeUnit.SECONDS);
		scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						db.force();
					}
				}, 1, TimeUnit.MINUTES);
	}

	private static void tryToCloseMemoryMappings() {
		System.gc();
		System.runFinalization();
		System.gc();
		System.runFinalization();
	}

	private static void printHelp() {
		System.out.println("usage: [options] [--] [arguments]");
		System.out.println("options:");
		System.out.println("-s[erver]               start the server");
		System.out.println("-c[lient]               run as client");
		System.out.println("-p[ort] #               the TCP/IP port for server or client (default 3147)");
		System.out.println("-repl                   (default) interactive read-eval-print-loop command line");
		System.out.println("-d[ump] [table]         dump to database.su or <table> to <table>.su");
		System.out.println("-l[oad] [table]         load from database.su or <table> from <table>.su");
		System.out.println("-check                  check the database integrity");
		System.out.println("-rebuild                check and rebuild the database, i.e. for crash recovery");
		System.out.println("-compact                remove deleted records");
		System.out.println("-v[ersion]              print the version");
		System.out.println("-i[mpersonate] version  tell clients this version");
		System.out.println("-ut #                   set max update tran duration in seconds (default 10)");
		System.out.println("-mw #                   set max writes per update transaction (default 10000)");
		System.out.println("-h[elp] or -?           print this message");
		System.out.println("--                      end the options, useful if arguments start with '-'");
	}

	public static void schedule(Runnable fn, long delay, TimeUnit unit) {
		scheduler.schedule(fn, delay, unit);
	}

	public static void scheduleAtFixedRate(Runnable fn, long delay, TimeUnit unit) {
		scheduler.scheduleAtFixedRate(fn, delay, delay, unit);
	}

}
