package suneido;

import suneido.CommandLineOptions.Action;
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
cmdlineoptions.action = Action.TEST;
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
			DbCompact.compact("suneido.db");
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
