/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Random;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;

/**
 * Test concurrency of loading nodes on demand
 */
public class TestDbHashTrie {
	static final int N = 1000000;
	static final int N_THREADS = 8;
	static final int N_OPS = 10000000;
	static Storage stor = new HeapStorage(100 * 1024);
	static DbHashTrie t = DbHashTrie.empty(stor);
	static TIntIntMap entries = new TIntIntHashMap();
	static long start;

	public static void main(String[] args) {
		createRandom(N);
		proc.run();
		int adr = t.store((e) -> e);
		t = DbHashTrie.from(stor, adr);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println((System.currentTimeMillis() - start) + " ms");
			}
		});
		start = System.currentTimeMillis();
		for (int i = 0; i < N_THREADS; ++i)
			new Thread(proc).start();
	}

	private static void createRandom(int n) {
		Random rand = new Random();
		for (int i = 0; i < n; ++i) {
			Entry e = new IntEntry(nextNonZero(rand), rand.nextInt());
			t = t.with(e);
			entries.put(e.key(), e.value());
		}
	}

	static final DbHashTrie.Translator translate = (Entry e) -> e;

	static final Runnable proc = () -> {
		try {
			Random rand = new Random();
			for (int i = 0; i < N_OPS; ++i) {
				int j = nextNonZero(rand);
				Entry e = t.get(j);
				if (entries.containsKey(j))
					assertThat(e.value(), equalTo(entries.get(j)));
				else
					assertThat(e, equalTo((Entry) null));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	};

	private static int nextNonZero(Random rand) {
		int n;
		do
			n = rand.nextInt(N);
			while (n == 0);
		return n;
	}

}
