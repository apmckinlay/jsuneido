/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.database.immudb.Dbpkg.nullObserver;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import suneido.Suneido;
import suneido.database.immudb.Dbpkg.Observer;
import suneido.database.immudb.Dbpkg.Status;

/**
 * Check the consistency of a database.
 * Verifies sizes and checksums within data store and index store.
 * Verifies that index store matches data store.
 * Verifies that all fields of data records can be unpacked.
 * Uses {@link CheckTable}
 */
class DbCheck {
	final String filename;
	final Storage dstor;
	final Storage istor;
	final Observer ob;
	Date last_good_commit;
	String details = "";

	static Status check(String filename, Observer ob) {
		Storage dstor = new MmapFile(filename + "d", "r");
		Storage istor = new MmapFile(filename + "i", "r");
		try {
			return check(filename, dstor, istor, ob);
		} finally {
			dstor.close();
			istor.close();
		}
	}

	static Status check(String filename, Storage dstor, Storage istor) {
		return check(filename, dstor, istor, nullObserver);
	}
	static Status check(String filename, Storage dstor, Storage istor, Observer ob) {
		return new DbCheck(filename, dstor, istor, ob).check();
	}

	// from Database.check, with active database
	static Status check(String filename, Database db,
			int dUpTo, int iUpTo, Observer ob) {
		Check check = new Check(db.dstor, db.istor).upTo(dUpTo, iUpTo);
		return new DbCheck(filename, db.dstor, db.istor, ob).check(check, db);
	}

	DbCheck(String filename, Storage dstor, Storage istor, Observer ob) {
		this.filename = filename;
		this.dstor = dstor;
		this.istor = istor;
		this.ob = ob;
	}

	Status check() {
		Database db = Database.openWithoutCheck("", dstor, istor);
		return check(new Check(dstor, istor), db);
	}

	private Status check(Check check, Database db) {
		println("checksums...");
		Status status = Status.CORRUPTED;
		boolean ok = check.fullcheck();
		if (Suneido.cmdlineoptions.asof != null) {
			// reopen with truncated dstor and istor
			db = Database.openWithoutCheck("", dstor, istor);
		}
		last_good_commit = check.lastOkDate();
		if (ok) {
			if (check_data_and_indexes(db))
				status = Status.OK;
		} else if (check.lastOkType() == 'b') {
			status = Status.UNRECOVERABLE;
			print("BULK ");
		} else
			print(check.status());
		print(details);
		println(status + " " + lastCommit(status) +
				" ok sizes " + fmt(check.dOkSize()) + ", " + fmt(check.iOkSize()));
		if (! filename.equals(""))
			if (status == Status.OK)
				DbGood.create(filename + "c", dstor.sizeFrom(0));
			else
				new File(filename + "c").delete(); // force full check
		return status;
	}

	String lastCommit(Status status) {
		String lgc = last_good_commit == null
				? "not found"
				: new SimpleDateFormat("yyyy-MM-dd HH:mm").format(last_good_commit);
		return "Last " + (status == Status.OK ? "good " : "")	+ "commit " + lgc;
	}

	String fmt(long n) {
		return String.format("%,d", n);
	}

	private static final int BAD_LIMIT = 10;
	private static final int N_THREADS = Runtime.getRuntime().availableProcessors();

	protected boolean check_data_and_indexes(Database db) {
		println("indexes & data...");
		ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
		ExecutorCompletionService<String> ecs = new ExecutorCompletionService<>(executor);
		try {
			int ntables = submitTasks(ecs, db);
			int nbad = getResults(executor, ecs, ntables);
			return nbad == 0;
		} catch (Throwable e) {
			details += e + "\n";
			return false;
		} finally {
			executor.shutdown();
		}
	}

	private static int submitTasks(ExecutorCompletionService<String> ecs,
			Database db) {
		ReadTransaction t = db.readTransaction();
		try {
			int ntables = 0;
			int maxTblnum = t.nextTableNum();
			for (int tblnum = 0; tblnum < maxTblnum; ++tblnum) {
				Table table = t.getTable(tblnum);
				if (table == null)
					continue;
				ecs.submit(new CheckTable(db, table.name));
				++ntables;
			}
			return ntables;
		} finally {
			t.complete();
		}
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
				errors = "checkTable interrupted\n";
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

	// public static void main(String[] args) {
	// 	DbTools.checkPrint("suneido.db");
	// }

}
