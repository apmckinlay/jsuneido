// -agentlib:hprof=cpu=samples,interval=1,depth=6,cutoff=.01

package suneido;

import static suneido.database.Database.theDB;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.ThreadSafe;

import suneido.database.*;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;
import suneido.database.server.ServerData;
import suneido.database.server.Timestamp;
import suneido.util.ByteBuf;

public class TestConcurrency {
	private static final ServerData serverData = new ServerData();
	private static final int NTHREADS = 4;
	private static final int DURATION = 1 * 60 * 1000;
	private static final int QUEUE_SIZE = 100;
	private static final Random rand = new Random();
	private static boolean setup = true;

	public static void main(String[] args) {
		Mmfile mmf = new Mmfile("concur.db", Mode.CREATE);
		theDB = new Database(mmf, Mode.CREATE);

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
			}
		}
		exec.finish();
		t = System.currentTimeMillis() - t;

		for (Runnable r : actions) {
			String s = r.toString();
			if (!s.equals(""))
				System.out.println(s);
		}

		System.out.println("finished " + nreps + " reps with " + NTHREADS
				+ " threads in " + readableDuration(t));
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

	synchronized static int random(int n) {
		return rand.nextInt(n);
	}

	public static void sleep(int nanos) {
		try {
			Thread.sleep(0, nanos);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
				Transaction t = theDB.readwriteTran();
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
				theDB.addColumn(tablename, "z");
			} catch (SuException e) {
				if (e.toString().contains("column already exists")
						|| e.toString().contains("conflict"))
					nAddColumnFailed.incrementAndGet();
				else
					throw e;
			}

		}
		private void removeColumn() {
			nRemoveColumns.incrementAndGet();
			try {
				theDB.removeColumn(tablename, "z");
			} catch (SuException e) {
				if (e.toString().contains("nonexistent")
						|| e.toString().contains("conflict"))
					nRemoveColumnFailed.incrementAndGet();
				else
					throw e;
			}

		}
		private void lookup() {
			nlookups.incrementAndGet();
			int n = random(N);
			Transaction t = theDB.readonlyTran();
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
			Transaction t = theDB.readonlyTran();
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
			Transaction t = theDB.readwriteTran();
			try {
				t.addRecord(tablename, record());
			} catch (SuException e) {
				if (! e.toString().contains("conflict"))
					throw e;
			} finally {
				if (t.complete() != null)
					nappendsfailed.incrementAndGet();
			}
		}
		private void update() {
			nupdates.incrementAndGet();
			int n = random(N);
			Transaction t = theDB.readwriteTran();
			try {
				Query q = CompileQuery.parse(t, serverData,
						"update " + tablename + " where b = " + n
						+ " set c = " + random(N));
				((QueryAction) q).execute();
				t.ck_complete();
			} catch (SuException e) {
				nupdatesfailed.incrementAndGet();
				t.abort();
				if (! e.toString().contains("conflict")) {
					throw e;
				}
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
			CheckIndexes.checkIndexes(tablename);
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
		final long node = theDB.dest.alloc(4096, (byte) 1);
		AtomicInteger nreps = new AtomicInteger();
		AtomicInteger nfailed = new AtomicInteger();
		AtomicInteger num = new AtomicInteger();

		@Override
		public void run() {
			Transaction t = theDB.readwriteTran();
			DestTran dest = new DestTran(t, theDB.dest);
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
				if (! e.toString().contains("conflict"))
					throw e;
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
			Transaction t = theDB.readwriteTran();
			t.addRecord(tablename, rec(1));
			t.ck_complete();
		}
		@Override
		public void run() {
			Transaction t = theDB.readwriteTran();
			Query q = CompileQuery.query(t, serverData, tablename);
			try {
				Row r = q.get(Dir.NEXT);
				Record rec = r.getFirstData();
				t.updateRecord(rec.off(), rec);
			} catch (SuException e) {
				if (! e.toString().contains("conflict"))
					throw e;
			}
			if (t.complete() != null)
				nfailed.incrementAndGet();
			nreps.incrementAndGet();
		}
		@Override
		public String toString() {
			return "NextNum " + tablename + (nreps.get() == 0 ? "" : " "
					+ (nfailed.get() * 100 / nreps.get()) + "% failed "
					+ "(" + nfailed + " / " + nreps + ")");

		}
	}
	private static Record rec(int... values) {
		Record r = new Record();
		for (int i : values)
			r.add(i);
		return r;
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