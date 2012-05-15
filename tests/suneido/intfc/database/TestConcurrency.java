/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import static suneido.util.Verify.verifyEquals;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.ThreadSafe;

import suneido.Suneido;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;
import suneido.database.server.ServerData;
import suneido.database.server.Timestamp;
import suneido.intfc.database.DatabasePackage.Status;

public class TestConcurrency {
	private static final DatabasePackage dbpkg = Suneido.dbpkg;
	private static final Database db = dbpkg.create("concur.db");
	private static final ServerData serverData = new ServerData();
	private static final int NTHREADS = 4;
	private static final int QUEUE_SIZE = 100;
	private static final long DURATION_MS = TimeUnit.SECONDS.toMillis(10);

	public static void main(String[] args) {
		Runnable[] actions = new Runnable[] {
			new NextNum("nextnum"),
			new NextNum("nextnum2"),
			new BigTable("bigtable"),
			new BigTable("bigtable2"),
		};
		BoundedExecutor exec = new BoundedExecutor(QUEUE_SIZE, NTHREADS);
		Random rand = newRandom();

		long t = System.currentTimeMillis();
		Transaction rt = null;
		int nreps = 0;
		while (true) {
			exec.submitTask(actions[rand.nextInt(actions.length)]);
			// actions[random(actions.length)].run();
			if (++nreps % 100 == 0) {
				long elapsed = System.currentTimeMillis() - t;
				if (elapsed > DURATION_MS)
					break;
				if (nreps % 500 == 0)
					System.out.print('.');
				if (nreps % 40000 == 0) {
					System.out.println();
					if (rt != null)
						rt.abort();
					rt = db.readTransaction();
				}
				db.limitOutstandingTransactions();
			}
		}
		System.out.println();
		if (rt != null)
			rt.abort();
		exec.finish();
		t = System.currentTimeMillis() - t;

		for (Runnable r : actions) {
			String s = r.toString();
			if (!s.equals(""))
				System.out.println(s);
		}
		System.out.println(NTHREADS + " threads finished " + nreps + " ops" +
				" in " + readableDuration(t) +
				" = " + (nreps / (t / 1000)) + " per second");

		db.checkTransEmpty();
		db.close();
		verifyEquals(Status.OK, dbpkg.check("concur.db",
				suneido.intfc.database.DatabasePackage.printObserver));
	}

	static Random newRandom() {
		return new Random(84576);
	}

	static final int MINUTE_MS = 60 * 1000;
	static final int SECOND_MS = 1000;
	static String readableDuration(long ms) {
		long div = 1;
		String units = "ms";
		if (ms > MINUTE_MS) {
			div = MINUTE_MS;
			units = "min";
		} else if (ms > SECOND_MS) {
			div = SECOND_MS;
			units = "sec";
		}
		long t = (ms * 10 + div / 2) / div;
		return (t / 10) + "." + (t % 10) + " " + units;
	}

	static class BigTable implements Runnable {
		private static final int N = 10000;
		String tablename;
		Random rand = newRandom();
		AtomicInteger nlookups = new AtomicInteger();
		AtomicInteger nranges = new AtomicInteger();
		AtomicInteger nappends = new AtomicInteger();
		AtomicInteger nappendsfailed = new AtomicInteger();
		AtomicInteger nupdates = new AtomicInteger();
		AtomicInteger nupdatesfailed = new AtomicInteger();
		static String[] strings = new String[] { "hello", "world", "now", "is",
			"the", "time", "foo", "bar", "foobar" };

		synchronized int random(int n) {
			return rand.nextInt(n);
		}
		BigTable(String tablename) {
			this.tablename = tablename;
			Request.execute(db, "create " + tablename
					+ " (a,b,c,d,e,f,g) key(a) index(b,c)");
			for (int i = 0; i < N / 100; ++i) {
				Transaction t = db.updateTransaction();
				for (int j = 0; j < 100; ++j)
					t.addRecord(tablename, record());
				t.ck_complete();
			}
		}
		@Override
		public void run() {
			switch (random(4)) {
			case 0: range(); break;
			case 1: lookup(); break;
			case 2: append(); break;
			case 3: update(); break;
			}
		}
		private void lookup() {
			nlookups.incrementAndGet();
			int n = random(N);
			Transaction t = db.readTransaction();
			try {
				Query q = CompileQuery.query(t, serverData,
						tablename + " where b = " + n);
				Row row;
				while (null != (row = q.get(Dir.NEXT)))
					assert n == row.firstData().getInt(1);
			} finally {
				t.ck_complete();
			}
		}
		private void range() {
			nranges.incrementAndGet();
			int from = random(N);
			int to = from + random(N - from);
			Transaction t = db.readTransaction();
			try {
				Query q = CompileQuery.query(t, serverData,
						tablename + " where b > " + from + " and b < " + to);
				Row row;
				while (null != (row = q.get(Dir.NEXT))) {
					Record rec = row.firstData();
					int n = rec.getInt(1);
					assert from < n && n < to;
				}
			} finally {
				t.ck_complete();
			}
		}
		private void append() {
			nappends.incrementAndGet();
			Transaction t = db.updateTransaction();
			try {
				t.addRecord(tablename, record());
			} catch (RuntimeException e) {
				throwUnexpected(e);
			} finally {
				if (t.complete() != null)
					nappendsfailed.incrementAndGet();
			}
		}
		private void update() {
			nupdates.incrementAndGet();
			int n = random(N);
			Transaction t = db.updateTransaction();
			try {
				Query q = CompileQuery.parse(t, serverData,
						"update " + tablename + " where b = " + n
						+ " set c = " + random(N));
				((QueryAction) q).execute();
				t.ck_complete();
			} catch (OutOfMemoryError e) {
				System.out.println(e);
				System.exit(-1);
			} catch (RuntimeException e) {
//System.out.println("\n" + e);
				nupdatesfailed.incrementAndGet();
/*BUG*/				t.abort();
				throwUnexpected(e);
			}
		}
		Record record() {
			RecordBuilder r = dbpkg.recordBuilder();
			r.add(Timestamp.next());
			for (int i = 0; i < 6; ++i)
				r.add(i % 2 == 0 ? random(N)
						: strings[random(strings.length)]);
			return r.build();
		}
		@Override
		public String toString() {
			return "BigTable " + tablename
					+ " " + nranges.get() + "r + " + nlookups.get() + " + "
					+ nappends.get() + "-" + nappendsfailed.get() + "a "
					+ nupdates.get() + "-" + nupdatesfailed.get() + "u "
					+ "= " + (nranges.get() + nlookups.get() + nappends.get());
		}
	}

	/** read/write of single record table with key() */
	static class NextNum implements Runnable {
		final String tablename;
		AtomicInteger nreps = new AtomicInteger();
		AtomicInteger nfailed = new AtomicInteger();
		public NextNum(String tablename) {
			this.tablename = tablename;
			Request.execute(db, "create " + tablename + " (num) key()");
			Transaction t = db.updateTransaction();
			t.addRecord(tablename, rec(1));
			t.ck_complete();
		}
		@Override
		public void run() {
			Transaction t = db.updateTransaction();
			Query q = CompileQuery.query(t, serverData, tablename);
			try {
				Row r = q.get(Dir.NEXT);
				Record rec = r.firstData();
				t.updateRecord(rec.address(), rec);
			} catch (RuntimeException e) {
				throwUnexpected(e);
			}
			if (t.complete() != null)
				nfailed.incrementAndGet();
			nreps.incrementAndGet();
		}
		@Override
		public String toString() {
			return "NextNum " + tablename + (nreps.get() == 0 ? "" : " "
					+ (nfailed.get() * 100 / nreps.get()) + "% conflicted "
					+ "(" + nfailed + " / " + nreps + ")");

		}
	}
	private static Record rec(int... values) {
		RecordBuilder r = dbpkg.recordBuilder();
		for (int i : values)
			r.add(i);
		return r.build();
	}

	private static void throwUnexpected(RuntimeException e) {
		if (! e.toString().contains("conflict")
				&& ! e.toString().contains("aborted")
				&& ! e.toString().contains("ended")
				&& ! e.toString().contains("exist"))
			throw e;
	}

	@ThreadSafe
	public static class BoundedExecutor {
	    private final ExecutorService exec;
	    private final Semaphore semaphore;

	    public BoundedExecutor(int bound, int nthreads) {
	        this.exec = Executors.newFixedThreadPool(nthreads);
	        this.semaphore = new Semaphore(bound);
	    }

	    public void submitTask(final Runnable command) {
	    	try {
		        semaphore.acquire();
			} catch (InterruptedException e) {
				System.out.println(e);
			}
	        try {
	            exec.execute(new Runnable() {
	                @Override
					public void run() {
	                    try {
	                        command.run();
	                    } finally {
	                        semaphore.release();
	                    }
	                }
	            });
	        } catch (RejectedExecutionException e) {
	            semaphore.release();
	        }
	   }

	   void finish() {
			exec.shutdown();
			try {
				exec.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				System.out.println("interrupted");
			}
	   }
	}
}
