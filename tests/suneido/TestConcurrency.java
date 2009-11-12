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
	private static final int NREPS = 1000;
	private static final int QUEUE_SIZE = 100;

	public static void main(String[] args) {
		Mmfile mmf = new Mmfile("concur.db", Mode.CREATE);
		theDB = new Database(mmf, Mode.CREATE);
		Random rand = new Random();

		Runnable[] actions = new Runnable[] {
//			new MmfileTest(),
			new NextNum("nextnum"),
//			new NextNum("nextnum2"), // currently failing with two NextNum's
		};
		final int na = actions.length;

		ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
		BoundedExecutor exec2 = new BoundedExecutor(exec, QUEUE_SIZE);

		long t = System.currentTimeMillis();
		for (int i = 0; i < NREPS; ++i) {
			int j = rand.nextInt(na);
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
			return nreps.get() == 0 ? "" :
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
