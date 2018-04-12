/* Copyright 2018 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util.testing;

/**
 * Simple benchmark framework.
 *
 * Usage:
	 	@Test
		public void foo() {
			benchmark("foo", (long nreps) -> {
				// setup
				while (nreps-- > 0)
					// operation to be timed
			});
		}
 */
public class Benchmark {

	public interface Bench {
		void run(long nreps);
	}

	public static void benchmark(String name, Bench f) {
		// set nobenchmarks in infinitest.args
		org.junit.Assume.assumeTrue(
				System.getProperty("nobenchmarks") == null);
		long nreps = reps_per_sec(f);
		long t1 = System.nanoTime();
		f.run(nreps);
		long dur = System.nanoTime() - t1;
		System.out.println(name + ": " + (dur / nreps) + " ns");
	}

	// estimate how many repetitions per second
	// also serves as "warmup" for JIT
	private static long reps_per_sec(Bench f) {
		for (long nreps = 1; ; nreps *= 2) {
			long t1 = System.nanoTime();
			f.run(nreps);
			long dur = System.nanoTime() - t1;
			if (dur > 10_000_000) // 10 ms in ns
				return nreps * 1_000_000_000L / dur;
		}
	}
}
