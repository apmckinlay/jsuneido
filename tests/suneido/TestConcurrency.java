// -agentlib:hprof=cpu=samples,interval=1,depth=6,cutoff=.01

package suneido;

import static suneido.SuException.verifyEquals;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.ThreadSafe;

import suneido.database.*;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;
import suneido.database.server.ServerData;
import suneido.database.server.Timestamp;
import suneido.database.tools.DbCheck;
import suneido.database.tools.DbCheck.Status;
import suneido.util.ByteBuf;

public class TestConcurrency {
	private static final ServerData serverData = new ServerData();
	private static final int NTHREADS = 20;
	private static final int SECONDS = 1000;
	private static final int MINUTES = 60 * SECONDS;
	private static final int DURATION = 15 * MINUTES;//20 * SECONDS;//
	private static final int QUEUE_SIZE = 100;
	private static final Random rand = new Random();
	private static boolean setup = true;

	public static void main(String[] args) {
		Transactions.MAX_SHADOWS_SINCE_ACTIVITY = 200;
		Transactions.MAX_FINALS_SIZE = 90;
		Mmfile mmf = new Mmfile("concur.db", Mode.CREATE);
		TheDb.set(new Database(mmf, Mode.CREATE));

		Runnable[] actions = new Runnable[] {
//			new MmfileTest(),
//			new TransactionTest(),
			new NextNum("nextnum"),
			new NextNum("nextnum2"),
			new BigTable("bigtable"),
			new BigTable("bigtable2"),
		};
		setup = true;
		BoundedExecutor exec = new BoundedExecutor(QUEUE_SIZE, NTHREADS);

		long t = System.currentTimeMillis();
		int nreps = 0;
		while (true) {
			exec.submitTask(actions[random(actions.length)]);
			if (++nreps % 100 == 0) {
				long elapsed = System.currentTimeMillis() - t;
				if (elapsed > DURATION)
					break;
				if (nreps % 500 == 0)
					System.out.print('.');
				if (nreps % 40000 == 0) {
					System.out.println();
					TheDb.db().readonlyTran();
				}
				TheDb.db().limitOutstandingTransactions();
			}
		}
		System.out.println();
		exec.finish();
		t = System.currentTimeMillis() - t;

		for (Runnable r : actions) {
			String s = r.toString();
			if (!s.equals(""))
				System.out.println(s);
		}

		TheDb.db().close();
		TheDb.set(null);
		verifyEquals(Status.OK, DbCheck.check("concur.db"));

		System.out.println("finished " + nreps + " reps with " + NTHREADS
				+ " threads in " + readableDuration(t));
	}

	static final int MINUTE_MS = MINUTES;
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

	synchronized static int random(int n) {
		return rand.nextInt(n);
	}

	public static void assert2(boolean condition) {
		assert setup || condition;
	}

	static class BigTable implements Runnable {
		private static final int N = 10000;
		String tablename;
		Random rand = new Random();
		AtomicInteger nlookups = new AtomicInteger();
		AtomicInteger nranges = new AtomicInteger();
		AtomicInteger nappends = new AtomicInteger();
		AtomicInteger nappendsfailed = new AtomicInteger();
		AtomicInteger nupdates = new AtomicInteger();
		AtomicInteger nupdatesfailed = new AtomicInteger();
		AtomicInteger nAddColumns = new AtomicInteger();
		AtomicInteger nAddColumnFailed = new AtomicInteger();
		AtomicInteger nRemoveColumns = new AtomicInteger();
		AtomicInteger nRemoveColumnFailed = new AtomicInteger();
		static String[] strings = new String[] { "hello", "world", "now", "is",
			"the", "time", "foo", "bar", "foobar" };

		synchronized int random(int n) {
			return rand.nextInt(n);
		}
		BigTable(String tablename) {
			this.tablename = tablename;
			Request.execute("create " + tablename
					+ " (a,b,c,d,e,f,g) key(a) index(b,c)");
			for (int i = 0; i < N / 100; ++i) {
				Transaction t = TheDb.db().readwriteTran();
				for (int j = 0; j < 100; ++j)
					t.addRecord(tablename, record());
				t.ck_complete();
			}
		}
		public void run() {
			switch (random(6)) {
			case 0: range(); break;
			case 1: lookup(); break;
			case 2: append(); break;
			case 3: update(); break;
			case 4: addColumn(); break;
			case 5: removeColumn(); break;
			}
		}
		private void addColumn() {
			nAddColumns.incrementAndGet();
			try {
				TheDb.db().addColumn(tablename, "z");
			} catch (SuException e) {
				nAddColumnFailed.incrementAndGet();
				throwUnexpected(e);
			}

		}
		private void removeColumn() {
			nRemoveColumns.incrementAndGet();
			try {
				TheDb.db().removeColumn(tablename, "z");
			} catch (SuException e) {
				nRemoveColumnFailed.incrementAndGet();
				throwUnexpected(e);
			}

		}
		private void lookup() {
			nlookups.incrementAndGet();
			int n = random(N);
			Transaction t = TheDb.db().readonlyTran();
			try {
				Query q = CompileQuery.query(t, serverData,
						tablename + " where b = " + n);
				Row row;
				while (null != (row = q.get(Dir.NEXT)))
					assert n == row.getFirstData().getInt(1);
			} finally {
				t.ck_complete();
			}
		}
		private void range() {
			nranges.incrementAndGet();
			int from = random(N);
			int to = from + random(N - from);
			Transaction t = TheDb.db().readonlyTran();
			try {
				Query q = CompileQuery.query(t, serverData,
						tablename + " where b > " + from + " and b < " + to);
				Row row;
				while (null != (row = q.get(Dir.NEXT))) {
					Record rec = row.getFirstData();
					int n = rec.getInt(1);
					assert from < n && n < to;
				}
			} finally {
				t.ck_complete();
			}
		}
		private void append() {
			nappends.incrementAndGet();
			Transaction t = TheDb.db().readwriteTran();
			try {
				t.addRecord(tablename, record());
			} catch (SuException e) {
				throwUnexpected(e);
			} finally {
				if (t.complete() != null)
					nappendsfailed.incrementAndGet();
			}
		}
		private void update() {
			nupdates.incrementAndGet();
			int n = random(N);
			Transaction t = TheDb.db().readwriteTran();
			try {
				Query q = CompileQuery.parse(t, serverData,
						"update " + tablename + " where b = " + n
						+ " set c = " + random(N));
				((QueryAction) q).execute();
				t.ck_complete();
			} catch (SuException e) {
				nupdatesfailed.incrementAndGet();
				t.abort();
				throwUnexpected(e);
			}
		}
		Record record() {
			Record r = new Record();
			r.add(Timestamp.next());
			for (int i = 0; i < 6; ++i)
				r.add(i % 2 == 0 ? random(N)
						: strings[random(strings.length)]);
			return r;
		}
		@Override
		public String toString() {
			return "BigTable " + tablename
					+ " " + nranges.get() + "r + " + nlookups.get() + " + "
					+ nappends.get() + "-" + nappendsfailed.get() + "a "
					+ nupdates.get() + "-" + nupdatesfailed.get() + "u "
					+ nAddColumns + "-" + nAddColumnFailed + "ac "
					+ nRemoveColumns + "-" + nRemoveColumnFailed + "rc "
					+ "= " + (nranges.get() + nlookups.get() + nappends.get()
							+ nAddColumns.get() + nRemoveColumns.get());
		}
	}

	static class MmfileTest implements Runnable {
		Mmfile mmf = new Mmfile("mmfiletest", Mode.CREATE);
		List<Long> offsets = Collections.synchronizedList(new ArrayList<Long>());
		Random rand = new Random();

		synchronized int random(int n) {
			return rand.nextInt(n);
		}
		@Override
		public void run() {
			int a = 0;
			ByteBuf b;
			long offset;
			if (offsets.size() > 0)
				a = random(7);
			switch (a) {
			case 0:
				offset = mmf.alloc(8 + random(100), (byte) 1);
				b = mmf.adr(offset);
				b.putLong(0, offset);
				offsets.add(offset);
				break;
			default:
				offset = offsets.get(random(offsets.size()));
				b = mmf.adr(offset);
				assert b.getLong(0) == offset;
				break;
			}
		}
		@Override
		public String toString() {
			return "";
		}
	}

	static class TransactionTest implements Runnable {
		final long node = TheDb.db().dest.alloc(4096, (byte) 1);
		AtomicInteger nreps = new AtomicInteger();
		AtomicInteger nfailed = new AtomicInteger();
		AtomicInteger num = new AtomicInteger();

		@Override
		public void run() {
			Transaction t = TheDb.db().readwriteTran();
			DestTran dest = new DestTran(t, TheDb.db().dest);
			try {
				switch (random(3)) {
				case 0:
					check(dest.node(node));
					break;
				case 1:
					check(dest.nodeForWrite(node));
					break;
				case 2:
					ByteBuf buf = dest.nodeForWrite(node);
					buf.putInt(0, num.getAndIncrement());
					t.create_act(123, 456); // otherwise doesn't commit
					break;
				}
			} catch (SuException e) {
				throwUnexpected(e);
			} finally {
				if (t.complete() != null)
					nfailed.incrementAndGet();
				nreps.incrementAndGet();
			}
		}
		private void check(ByteBuf buf) {
			int n = buf.getInt(0);
			for (int i = 0; i < 10; ++i) {
				Thread.yield();
				int nn = buf.getInt(0);
				if (n != nn)
					System.out.println("changed from " + n + " to " + nn + " (" + i + ")");
				//assert n == buf.getInt(0);
			}
		}
		@Override
		public String toString() {
			return nreps.get() == 0 ? "" : "TransactionTest " +
					(nfailed.get() * 100 / nreps.get()) + "% failed "
					+ "(" + nfailed + " / " + nreps + ")";

		}
	}

	/** read/write of single record table with key() */
	static class NextNum implements Runnable {
		final String tablename;
		AtomicInteger nreps = new AtomicInteger();
		AtomicInteger nfailed = new AtomicInteger();
		public NextNum(String tablename) {
			this.tablename = tablename;
			Request.execute("create " + tablename + " (num) key()");
			Transaction t = TheDb.db().readwriteTran();
			t.addRecord(tablename, rec(1));
			t.ck_complete();
		}
		@Override
		public void run() {
			Transaction t = TheDb.db().readwriteTran();
			Query q = CompileQuery.query(t, serverData, tablename);
			try {
				Row r = q.get(Dir.NEXT);
				Record rec = r.getFirstData();
				t.updateRecord(rec.off(), rec);
			} catch (SuException e) {
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
		Record r = new Record();
		for (int i : values)
			r.add(i);
		return r;
	}

	private static void throwUnexpected(SuException e) {
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
