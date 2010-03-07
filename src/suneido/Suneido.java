package suneido;

import suneido.database.*;
import suneido.database.server.DbmsServer;

/**
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Suneido {
	public static CommandLineOptions cmdlineoptions;

	public static void main(String[] args) throws Exception {
		cmdlineoptions = CommandLineOptions.parse(args);
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
		case VERSION:
			System.out.println("jSuneido " + WhenBuilt.when());
			break;
		default:
			throw SuException.unreachable();
		}
	}
}
