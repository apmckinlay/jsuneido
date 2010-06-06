package suneido;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import suneido.database.server.*;
import suneido.database.tools.*;

/**
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Suneido {
	public static CommandLineOptions cmdlineoptions;

	public static void main(String[] args) throws Exception {
		if (! System.getProperty("java.vm.name").contains("Server VM"))
			System.out.println("WARNING: Server VM is recommended");
		cmdlineoptions = CommandLineOptions.parse(args);
		try {
			doAction();
		} catch (Exception e) {
			fatal(cmdlineoptions.action + " FAILED", e);
		}
	}

	public static void fatal(String s) {
		errlog("FATAL: " + s);
		System.exit(-1);
	}

	public static void fatal(String s, Exception e) {
		errlog("FATAL: " + s + ": " + e);
		e.printStackTrace();
		System.exit(-1);
	}

	public synchronized static void errlog(String s) {
		System.out.println(s);
		try {
			FileWriter fw = new FileWriter("error.log", true);
			fw.append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
			fw.append(" ");
			fw.append(s);
			fw.append("\n");
			fw.close();
		} catch (IOException e) {
			System.out.println("can't write to error.log " + e);
		}
	}

	private static void doAction() throws Exception {
		switch (cmdlineoptions.action) {
		case REPL:
			Repl.main(null);
			break;
		case SERVER:
			HttpServerMonitor.run(cmdlineoptions.server_port + 1);
			DbmsServerBySelect.run(cmdlineoptions.server_port);
			break;
		case DUMP:
			if (cmdlineoptions.action_arg == null)
				DbDump.dumpDatabasePrint("suneido.db", "database.su");
			else
				DbDump.dumpTablePrint(cmdlineoptions.action_arg);
			break;
		case LOAD:
			if (cmdlineoptions.action_arg == null)
				DbLoad.loadPrint("database.su");
			else
				DbLoad.loadTablePrint(cmdlineoptions.action_arg);
			break;
		case CHECK:
			DbCheck.checkPrintExit("suneido.db");
			break;
		case REBUILD:
			DbRebuild.rebuildOrExit("suneido.db");
			break;
		case COMPACT:
			DbCompact.compactPrint("suneido.db");
			break;
		case TEST:
			RunAllTests.run("jsuneido.jar");
			break;
		case VERSION:
			System.out.println("jSuneido " + WhenBuilt.when());
			System.out.println("Java " + System.getProperty("java.version")
					+ System.getProperty("java.vm.name").replace("Java", ""));
			break;
		case TESTCLIENT:
			TestClient.main(cmdlineoptions.action_arg);
			break;
		case TESTSERVER:
			TestServer.main(new String[0]);
			break;
		case ERROR:
			System.out.println(cmdlineoptions.action_arg);
			System.out.println();
			// fall through
		case HELP:
			printHelp();
			break;
		default:
			throw SuException.unreachable();
		}
	}

	private static void printHelp() {
		System.out.println("usage: [options] [--] [arguments]");
		System.out.println("options:");
		System.out.println("-s[erver]               start the server (this is the default option)");
		System.out.println("-p[ort] #               the TCP/IP port to run the server on (default 3147)");
		System.out.println("-repl                   interactive read-eval-print-loop command line interface");
		System.out.println("-d[ump] [table]         dump to database.su or <table> to <table>.su");
		System.out.println("-l[oad] [table]         load from database.su or <table> from <table>.su");
		System.out.println("-check                  check the database integrity");
		System.out.println("-rebuild                check and rebuild the database, i.e. for crash recovery");
		System.out.println("-compact                remove deleted records");
		System.out.println("-t[ests[                run the built-in JUnit tests");
		System.out.println("-v[ersion]              print the version");
		System.out.println("-i[mpersonate] version  tell clients this version");
		System.out.println("-h[elp] or -?           print this message");
		System.out.println("--                      end the options, useful if arguments start with '-'");
	}
}
