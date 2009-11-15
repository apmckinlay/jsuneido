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
import suneido.util.ByteBuf;

public class TestConcurrency {
	private static final ServerData serverData = new ServerData();
	private static final int NTHREADS = 2;
	private static final int NREPS = 1000000;
	private static final int QUEUE_SIZE = 100;

	static final Random rand = new Random();

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

	public static void main(String[] args) {
		Mmfile mmf = new Mmfile("concur.db", Mode.CREATE);
		theDB = new Database(mmf, Mode.CREATE);

		Runnable[] actions = new Runnable[] {
			new MmfileTest(),
			new TransactionTest(),
			new NextNum("nextnum"),
			new NextNum("nextnum2"),
		};
		final int na = actions.length;

		ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
		BoundedExecutor exec2 = new BoundedExecutor(exec, QUEUE_SIZE);

		long t = System.currentTimeMillis();
		for (int i = 0; i < NREPS; ++i) {
			int j = random(na);
			try {
				exec2.submitTask(actions[j]);
			} catch (InterruptedException e) {
				System.out.println(e);
			}
		}
		exec.shutdown();
		try {
			exec.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.out.println("interrupted");
		}
		t = System.currentTimeMillis() - t;
		t = (t + 500) / 1000;

		for (Runnable r : actions) {
			String s = r.toString();
			if (!s.equals(""))
				System.out.println(r.toString());
		}

		System.out.println("finished " + NREPS + " reps with " + NTHREADS + " threads in " + t + " sec");
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
			return nreps.get() == 0 ? "" : "NextNum " +
					(nfailed.get() * 100 / nreps.get()) + "% failed "
					+ "(" + nfailed + " / " + nreps + ")";

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
	    private final Executor exec;
	    private final Semaphore semaphore;

	    public BoundedExecutor(Executor exec, int bound) {
	        this.exec = exec;
	        this.semaphore = new Semaphore(bound);
	    }

	    public void submitTask(final Runnable command)
	            throws InterruptedException {
	        semaphore.acquire();
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
	}
}
