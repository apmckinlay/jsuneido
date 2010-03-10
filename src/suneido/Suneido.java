package suneido;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import suneido.database.*;
import suneido.database.server.DbmsServer;

// TODO log errors/warnings

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
			fatal(cmdlineoptions.action + " FAILED " + e);
		}
	}

	public static void fatal(String s) {
		errlog("FATAL: " + s);
		System.exit(-1);
	}

	public static void errlog(String s) {
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
			DbmsServer.main(null);
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
		default:
			throw SuException.unreachable();
		}
	}
}
