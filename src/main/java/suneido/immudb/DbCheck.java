/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.intfc.database.DatabasePackage.nullObserver;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import suneido.intfc.database.DatabasePackage.Observer;
import suneido.intfc.database.DatabasePackage.Status;

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
	static Status check(String filename, Storage dstor, Storage istor,
			int dUpTo, int iUpTo, Observer ob) {
		Check check = new Check(dstor, istor).upTo(dUpTo, iUpTo);
		return new DbCheck(filename, dstor, istor, ob).check(check);
	}

	DbCheck(String filename, Storage dstor, Storage istor, Observer ob) {
		this.filename = filename;
		this.dstor = dstor;
		this.istor = istor;
		this.ob = ob;
	}

	Status check() {
		return check(new Check(dstor, istor));
	}

	private Status check(Check check) {
		println("checksums...");
		Status status = Status.CORRUPTED;
		boolean ok = check.fullcheck();
		last_good_commit = check.lastOkDate();
		if (ok) {
			println("tables...");
			if (check_data_and_indexes())
				status = Status.OK;
		} else
			print(check.status());
		print(details);
		println(status + " " + lastCommit(status));
		if (! filename.equals(""))
			if (status == Status.OK)
				DbGood.create(filename + "c", dstor.sizeFrom(0));
			else
				new File(filename + "c").delete(); // force full check
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
		ExecutorCompletionService<String> ecs = new ExecutorCompletionService<>(executor);
		Database db = Database.openWithoutCheck("", dstor, istor);
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

//	public static void main(String[] args) {
//		DbTools.checkPrint(DatabasePackage.dbpkg, "suneido.db");
//	}

}
