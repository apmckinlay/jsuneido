/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import suneido.compiler.Compiler;
import suneido.database.immudb.Database;
import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.Dump;
import suneido.database.server.DbmsServer;
import suneido.runtime.ContextLayered;
import suneido.runtime.Contexts;
import suneido.util.Errlog;
import suneido.util.Print;

public class Suneido {
	private static final ThreadFactory threadFactory =
		new ThreadFactoryBuilder()
			.setDaemon(true)
			.setNameFormat("suneido-thread-%d")
			.build();
	private static final ScheduledExecutorService scheduler =
			Executors.newSingleThreadScheduledExecutor(threadFactory);
	public static CommandLineOptions cmdlineoptions =
			CommandLineOptions.parse(); // for tests
	public static Contexts contexts = new Contexts();
	public static ContextLayered context = new ContextLayered(contexts);
	public static ThreadGroup threadGroup = new ThreadGroup("Suneido");
	public static DbmsServer server;
	public static volatile boolean exiting = false;
	public static final String built = built();

	public static void main(String[] args) {
		ClassLoader.getSystemClassLoader().setPackageAssertionStatus("suneido", true);
		cmdlineoptions = CommandLineOptions.parse(args);
		if (cmdlineoptions.unattended) {
			try {
				System.setOut(new PrintStream(new FileOutputStream("output.log", true)));
				System.setErr(new PrintStream(new FileOutputStream("error.log", true)));
			} catch (FileNotFoundException e) {
				Errlog.fatal("failed to redirect stdout or stderr", e);
			}
		}
		if (cmdlineoptions.max_update_tran_sec != 0)
			Dbpkg.setOption("max_update_tran_sec", cmdlineoptions.max_update_tran_sec);
		if (cmdlineoptions.max_writes_per_tran != 0)
			Dbpkg.setOption("max_writes_per_tran", cmdlineoptions.max_writes_per_tran);
		try {
			doAction();
		} catch (Throwable e) {
			Errlog.fatal(cmdlineoptions.action + " FAILED", e);
		}
	}

	private static void doAction() throws Throwable {
		String dbFilename = Dbpkg.DB_FILENAME;
		switch (cmdlineoptions.action) {
		case REPL:
			Repl.repl();
			break;
		case SERVER:
			TheDbms.setPort(cmdlineoptions.serverPort);
			Print.timestamped("starting server");
			startServer(); // does not return
			break;
		case CLIENT:
			TheDbms.remote(cmdlineoptions.actionArg, cmdlineoptions.serverPort);
			scheduleAtFixedRate(TheDbms.closer, 30, TimeUnit.SECONDS);
			Runtime.getRuntime().addShutdownHook(
					new Thread(() -> TheDbms.closeAll()));
			Errlog.setExtra(TheDbms::sessionid);
			if ("".equals(cmdlineoptions.remainder))
				Repl.repl2();
			else
				Compiler.eval("Init()");
			break;
		case DUMP:
			String dumptablename = cmdlineoptions.actionArg;
			if (dumptablename == null)
				DbTools.dumpPrintExit(dbFilename, "database.su");
			else
				DbTools.dumpTablePrint(dbFilename, dumptablename);
			break;
		case LOAD:
			String loadtablename = cmdlineoptions.actionArg;
			if (loadtablename != null)
				DbTools.loadTablePrint(dbFilename, loadtablename);
			else
				DbTools.loadDatabasePrint(dbFilename, "database.su");
			break;
		case LOAD2:
			DbTools.load2(cmdlineoptions.actionArg);
			break;
		case CHECK:
			DbTools.checkPrintExit(cmdlineoptions.actionArg == null
					? dbFilename : cmdlineoptions.actionArg);
			break;
		case REBUILD:
			DbTools.rebuildOrExit(dbFilename);
			break;
		case REBUILD2:
			DbTools.rebuild2(cmdlineoptions.actionArg);
			break;
		case REBUILD3:
			DbTools.rebuild3(cmdlineoptions.actionArg);
			break;
		case COMPACT:
			DbTools.compactPrintExit(dbFilename);
			break;
		case COMPACT2:
			DbTools.compact2(cmdlineoptions.actionArg);
			break;
		case DBDUMP:
			Dump.dump();
			break;
		case VERSION:
			System.out.println("jSuneido " + built);
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
			throw SuInternalError.unreachable();
		}
	}

	/** does not return */
	private static void startServer() {
		scheduleAtFixedRate(Deadlock::check, 5, TimeUnit.MINUTES);
		HttpServerMonitor.run(cmdlineoptions.serverPort + 1);
		openDbms();
		server = new DbmsServer(cmdlineoptions.timeoutMin);
		server.open(cmdlineoptions.serverPort);
		try {
			Compiler.eval("Init()");
		} catch (Throwable e) {
			Errlog.fatal("error during init", e);
		}
		Errlog.setExtra(TheDbms::sessionid);
		HttpServerMonitor.running();
		server.serve();
	}

	private static Database db;

	public static void openDbms() {
		db = Dbpkg.open(Dbpkg.DB_FILENAME);
		if (db == null) {
			Errlog.error("database corrupt, rebuilding");
			HttpServerMonitor.rebuilding();
			tryToCloseMemoryMappings();
			DbTools.rebuildOrExit(Dbpkg.DB_FILENAME);
			db = Dbpkg.open(Dbpkg.DB_FILENAME);
			if (db == null)
				Errlog.fatal("could not open database after rebuild");
		}
		TheDbms.set(db);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			exiting = true;
			Suneido.db.close();
		}));
		scheduleAtFixedRate(db::limitOutstandingTransactions, 1, TimeUnit.SECONDS);
		scheduleAtFixedRate(db::force, 1, TimeUnit.MINUTES);
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
		System.out.println("-s[erver]                 start the server");
		System.out.println("-c[lient]                 run as client");
		System.out.println("-p[ort] #                 the TCP/IP port for server or client (default 3147)");
		System.out.println("-repl                     (default) interactive read-eval-print-loop command line");
		System.out.println("-d[ump] [table]           dump to database.su or <table> to <table>.su");
		System.out.println("-l[oad] [table]           load from database.su or <table> from <table>.su");
		System.out.println("-check                    check the database integrity");
		System.out.println("-rebuild                  check and rebuild the database, i.e. for crash recovery");
		System.out.println("-compact                  remove deleted records");
		System.out.println("-v[ersion]                print the version");
		System.out.println("-i[mpersonate] <version>  tell clients this version");
		System.out.println("-t[ime]o[ut] #            time out in minutes for idle clients (default is 240)");
		System.out.println("-ut #                     set max update tran duration in seconds (default 10)");
		System.out.println("-mw #                     set max writes per update transaction (default 10000)");
		System.out.println("-u[nattended]             redirect stdout and stderr to output.log and error.log");
		System.out.println("-dbdump                   output database structure (for debugging)");
		System.out.println("-h[elp] or -?             print this message");
		System.out.println("--                        end the options, useful if arguments start with '-'");
	}

	public static void schedule(Runnable fn, long delay, TimeUnit unit) {
		scheduler.schedule(fn, delay, unit);
	}

	public static void scheduleAtFixedRate(Runnable fn, long delay, TimeUnit unit) {
		// need to catch exceptions else scheduler will stop running task
		scheduler.scheduleAtFixedRate(() -> Errlog.run(fn), delay, delay, unit);
	}

	public static void exit(int status) {
		exiting = true;
		System.exit(status);
	}

	private static String built() {
		try {
			Date d = new Date(Suneido.class
					.getResource("Suneido.class")
					.openConnection()
					.getLastModified());
			return new SimpleDateFormat("MMM d yyyy HH:mm").format(d) + " (Java)";
		} catch (IOException e) {
			return "??? (Java)";
		}
	}

}
