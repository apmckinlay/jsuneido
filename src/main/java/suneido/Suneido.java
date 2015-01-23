/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import suneido.compiler.Compiler;
import suneido.database.server.DbmsServer;
import suneido.debug.DebugManager;
import suneido.immudb.Dump;
import suneido.intfc.database.Database;
import suneido.intfc.database.DatabasePackage;
import suneido.jsdi.JSDI;
import suneido.runtime.ContextLayered;
import suneido.runtime.Contexts;
import suneido.util.Errlog;
import suneido.util.Print;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class Suneido {
	public static DatabasePackage dbpkg = suneido.immudb.DatabasePackage.dbpkg;
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
			dbpkg.setOption("max_update_tran_sec", cmdlineoptions.max_update_tran_sec);
		if (cmdlineoptions.max_writes_per_tran != 0)
			dbpkg.setOption("max_writes_per_tran", cmdlineoptions.max_writes_per_tran);
		DebugManager.getInstance().setDebugModel(cmdlineoptions.debugModel);
		try {
			doAction();
		} catch (Throwable e) {
			Errlog.fatal(cmdlineoptions.action + " FAILED", e);
		}
	}

	private static void doAction() throws Throwable {
		String dbFilename = dbpkg.dbFilename();
		switch (cmdlineoptions.action) {
		case REPL:
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
				Repl.repl2();
			else
				Compiler.eval("Init()");
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
		case DBDUMP:
			Dump.dump();
			break;
		case VERSION:
			System.out.println("jSuneido " + WhenBuilt.when());
			if (JSDI.isInitialized()) {
				System.out.println("JSDI " + JSDI.getInstance().whenBuilt());
			}
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

	private static void startServer() {
		HttpServerMonitor.run(cmdlineoptions.serverPort + 1);
		openDbms();
		try {
			Compiler.eval("Init()");
		} catch (Throwable e) {
			Errlog.fatal("error during init", e);
		}
		DbmsServer.run(cmdlineoptions.serverPort, cmdlineoptions.timeoutMin);
	}

	private static Database db;

	public static void openDbms() {
		db = dbpkg.open(dbpkg.dbFilename());
		if (db == null) {
			Errlog.errlog("database corrupt, rebuilding");
			tryToCloseMemoryMappings();
			DbTools.rebuildOrExit(dbpkg, dbpkg.dbFilename());
			db = dbpkg.open(dbpkg.dbFilename());
			if (db == null)
				Errlog.fatal("could not open database after rebuild");
		}
		TheDbms.set(db);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> Suneido.db.close()));
		scheduleAtFixedRate(db::limitOutstandingTransactions, 1, TimeUnit.SECONDS);
		scheduleAtFixedRate(db::force, 1, TimeUnit.MINUTES);
		Errlog.setExtra(TheDbms::sessionid);
	}

	public static String sessionid() {
		String sessionid = TheDbms.sessionid();
		return ("".equals(sessionid) || "127.0.0.1".equals(sessionid)) ? ""
			: sessionid + " ";
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
		System.out.println("-nojsdi                   disable JSDI dll interface (default for server)");
		System.out.println("-debug on|off             default is 'on' for repl & client, 'off' otherwise");
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
		scheduler.scheduleAtFixedRate(fn, delay, delay, unit);
	}

}
