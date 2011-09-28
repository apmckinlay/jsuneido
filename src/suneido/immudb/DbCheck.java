/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.intfc.database.DatabasePackage.nullObserver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import suneido.DbTools;
import suneido.intfc.database.DatabasePackage.Observer;
import suneido.intfc.database.DatabasePackage.Status;

/**
 * check the consistency of a database
 * e.g. after finding it was not shutdown properly
 */
class DbCheck {
	final Storage stor;
	final Observer ob;
	Date last_good_commit;
	String details = "";

	static Status check(String dbFilename, Observer ob) {
		Storage stor = new MmapFile(dbFilename, "r");
		try {
			return check(stor, ob);
		} finally {
			stor.close();
		}
	}

	static Status check(Storage stor) {
		return check(stor, nullObserver);
	}
	static Status check(Storage stor, Observer ob) {
		return new DbCheck(stor, ob).check();
	}

	DbCheck(Storage stor, Observer ob) {
		this.stor = stor;
		this.ob = ob;
	}

	Status check() {
		println("checksums...");
		Check check = new Check(stor);
		Status status = Status.CORRUPTED;
		boolean ok = check.fullcheck();
		last_good_commit = check.lastOkDatetime();
		if (ok) {
			println("tables...");
			if (check_data_and_indexes())
				status = Status.OK;
		}
		print(details);
		println(status + " " + lastCommit(status));
		return status;
	}

	String lastCommit(Status status) {
		return status == Status.UNRECOVERABLE
			? "Unrecoverable"
			: "Last " + (status == Status.CORRUPTED ? "good " : "")	+ "commit "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm").format(last_good_commit);
	}

	private static final int BAD_LIMIT = 10;
	private static final int N_THREADS = Runtime.getRuntime().availableProcessors();

	protected boolean check_data_and_indexes() {
		ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
		ExecutorCompletionService<String> ecs = new ExecutorCompletionService<String>(executor);
		Database db = Database.open(stor);
		try {
			int ntables = submitTasks(ecs, db);
			int nbad = getResults(executor, ecs, ntables);
			return nbad == 0;
		} catch (Throwable e) {
			details += e + "\n";
			return false;
		} finally {
			executor.shutdown();
			db.close();
		}
	}

	private static int submitTasks(ExecutorCompletionService<String> ecs, Database db) {
		int ntables = 0;
		int maxTblnum = db.nextTableNum();
		for (int tblnum = 0; tblnum < maxTblnum; ++tblnum) {
			Table table = db.schema().get(tblnum);
			if (table == null)
				continue;
			ecs.submit(new CheckTable(db, table.name));
			++ntables;
		}
		return ntables;
	}

	private int getResults(ExecutorService executor,
			ExecutorCompletionService<String> ecs, int ntables) {
		int nbad = 0;
		for (int i = 0; i < ntables; ++i) {
			String errors;
			try {
				errors = ecs.take().get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				errors = "checkTable interrruped\n";
			} catch (ExecutionException e) {
				errors = "checkTable " + e;
			}
			if (! errors.isEmpty()) {
				details += errors + "\n";
				if (++nbad > BAD_LIMIT) {
					executor.shutdownNow();
					details += "TOO MANY ERRORS, GIVING UP\n";
					break;
				}
			}
		}
		return nbad;
	}

	void print(String s) {
		ob.print(s);
	}
	void println(String s) {
		ob.print(s + "\n");
	}

	public static void main(String[] args) {
		DbTools.checkPrintExit(DatabasePackage.dbpkg, "immu.db");
	}

}
