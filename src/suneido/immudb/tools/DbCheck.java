/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

import suneido.immudb.*;
import suneido.immudb.schema.Table;

/**
 * check the consistency of a database
 * e.g. after finding it was not shutdown properly
 */
public class DbCheck {
	public enum Status { OK, CORRUPTED, UNRECOVERABLE };
	final Storage stor;
	final String filename;
	Date last_good_commit;
	String details = "";
	protected final boolean print;

	public DbCheck(Storage stor, boolean print) {
		this.stor = stor;
		this.filename = "";
		this.print = print;
	}

	public DbCheck(String filename, boolean print) {
		this.stor = new MmapFile(filename, "r");
		this.filename = filename;
		this.print = print;
	}

	public static Status check(String filename) {
		return new DbCheck(filename, false).check();
	}

	public static Status checkPrint(String filename) {
		return new DbCheck(filename, true).check();
	}

	public static void checkPrintExit(String filename) {
		Status status = new DbCheck(filename, true).check();
		System.exit(status == Status.OK ? 0 : -1);
	}

	public Status check() {
		println("Checking " + filename);
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
		println(filename + " " + status + " " + lastCommit(status));
		return status;
	}

	public String lastCommit(Status status) {
		return status == Status.UNRECOVERABLE
			? "Unrecoverable"
			: "Last " + (status == Status.CORRUPTED ? "good " : "")	+ "commit "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm").format(last_good_commit);
	}

	private final static int BAD_LIMIT = 10;
	private final static int N_THREADS = 8;

	protected boolean check_data_and_indexes() {
		ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
		ExecutorCompletionService<String> ecs = new ExecutorCompletionService<String>(executor);
		Database db = Database.open(filename, "r");
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

	private int submitTasks(ExecutorCompletionService<String> ecs, Database db) {
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
				details += errors;
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
		if (print)
			System.out.print(s);
	}
	void println() {
		if (print)
			System.out.println();
	}
	void println(String s) {
		if (print)
			System.out.println(s);
	}

	public static void main(String[] args) {
		checkPrintExit("immu.db");
	}

}
