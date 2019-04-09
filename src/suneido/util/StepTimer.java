/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

/**
 * Track the duration (in milliseconds) of multiple steps of a process.
 * Log an error if a specified limit is exceeded.
 */
public class StepTimer {
	private final Stopwatch sw = Stopwatch.createStarted();
	private final String desc;
	private long limit;
	private final ArrayList<Long> steps = new ArrayList<>();

	/**
	 * @param desc Used in exception message
	 * @param limit An exception will be thrown if this limit (milliseconds) is exceeded
	 */
	public StepTimer(String desc, long limit) {
		this.desc = desc;
		this.limit = limit;
	}

	public void step() {
		long t = sw.elapsed(TimeUnit.MILLISECONDS);
		steps.add(t);
		if (t > limit) {
			Errlog.error(desc + " exceeded time limit " +
					t + "ms > " + limit + " " + steps());
			limit = Long.MAX_VALUE; // prevent further logging
		}
	}

	public ArrayList<Long> finish() {
		step();
		return steps();
	}

	private ArrayList<Long> steps() {
		for (int i = steps.size() - 1; i >= 1; --i)
			steps.set(i, steps.get(i) - steps.get(i - 1));
		return steps;
	}

//	public static void main(String[] args) throws InterruptedException {
//		StepTimer st = new StepTimer("Testing", 1000);
//		Thread.sleep(100);
//		st.step();
//		Thread.sleep(500);
//		st.step();
//		Thread.sleep(200);
//		System.out.println(st.finish());
//	}
}
