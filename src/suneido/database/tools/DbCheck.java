/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

import suneido.database.*;
import suneido.database.Database.TN;
import suneido.util.ByteBuf;
import suneido.util.Checksum;

/**
 * check the consistency of a database
 * e.g. after finding it was not shutdown properly
 */
public class DbCheck {
	public enum Status { OK, CORRUPTED, UNRECOVERABLE };
	private final String filename;
	final Mmfile mmf;
	long last_good_commit = 0; // offset
	String details = "";
	protected final boolean print;

	public DbCheck(String filename, boolean print) {
		this.filename = filename;
		this.print = print;
		mmf = new Mmfile(filename, Mode.READ_ONLY);
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
		println("Checking commits and shutdowns");
		Status status = check_commits_and_shutdowns();
		if (status == Status.OK) {
			println("Checking data and indexes");
			if (!check_data_and_indexes())
				status = Status.CORRUPTED;
		}
		print(details);
		println(filename + " " + status + " " + lastCommit(status));
		return status;
	}

	public String lastCommit(Status status) {
		Date d = new Date(last_good_commit);
		return status == Status.UNRECOVERABLE
			? "Unrecoverable"
			: "Last " + (status == Status.CORRUPTED ? "good " : "")	+ "commit "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm").format(d);
	}

	private Status check_commits_and_shutdowns() {
		if (mmf.first() == 0) {
			details = "no data\n";
			return Status.UNRECOVERABLE;
		}
		boolean ok = false;
		boolean has_a_shutdown = false;
		Checksum cksum = new Checksum();

		Mmfile.Iter iter = mmf.iterator();
		loop: while (iter.next()) {
			ok = false;
			ByteBuf buf = iter.current();
			switch (iter.type()) {
			case Mmfile.DATA:
				int tblnum = buf.getInt(0);
				if (tblnum != TN.TABLES && tblnum != TN.INDEXES) {
					Record r = new Record(buf.slice(4));
					cksum.update(buf.getByteBuffer(), r.bufSize() + 4);
					// + 4 to skip tblnum
				}
				break;
			case Mmfile.COMMIT:
				Commit commit = new Commit(buf);
				cksum.update(buf.getByteBuffer(), commit.sizeWithoutChecksum());
				if (commit.getChecksum() != cksum.getValue()) {
					details += "checksum mismatch\n";
					break loop;
				}
				last_good_commit = commit.getDate();
				cksum.reset();
				process_deletes(commit);
				break;
			case Mmfile.SESSION:
				if (buf.get(0) == Session.SHUTDOWN) {
					has_a_shutdown = true;
					ok = true;
				}
				break;
			case Mmfile.OTHER:
				// ignore
				break;
			default:
				details += "invalid block type\n";
				break loop;
			}
		}
		if (! has_a_shutdown) {
			details += "no valid shutdowns\n";
			return Status.UNRECOVERABLE;
		}
		if (last_good_commit == 0) {
			details += "no valid commits\n";
			return Status.UNRECOVERABLE;
		}
		if (iter.corrupt()) {
			details += "iteration failed\n";
			return Status.CORRUPTED;
		}
		if (!ok) {
			details += "missing last shutdown\n";
			return Status.CORRUPTED;
		}
		return Status.OK;
	}

	protected void process_deletes(Commit commit) {
		// empty stub, overridden by DbRebuild
	}

	private final static int BAD_LIMIT = 10;
	private final static int N_THREADS = 8;

	protected boolean check_data_and_indexes() {
		ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
		ExecutorCompletionService<String> ecs = new ExecutorCompletionService<String>(executor);
		TheDb.set(new Database(filename, Mode.READ_ONLY));
		Transaction t = TheDb.db().readonlyTran();
		try {
			BtreeIndex bti = t.getBtreeIndex(Database.TN.TABLES, "tablename");
			BtreeIndex.Iter iter = bti.iter(t).next();
			int ntables = 0;
			for (; !iter.eof(); iter.next()) {
				Record r = t.input(iter.keyadr());
				String tablename = r.getString(Table.TABLE);
				ecs.submit(new CheckTable(tablename));
				++ntables;
			}
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
			return nbad == 0;
		} catch (Throwable e) {
			details += e + "\n";
			return false;
		} finally {
			executor.shutdown();
			t.complete();
			TheDb.db().close();
			TheDb.set(null);
		}
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
		checkPrintExit("suneido.db");
	}

}
