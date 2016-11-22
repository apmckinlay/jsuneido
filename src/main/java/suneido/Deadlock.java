/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import suneido.util.Errlog;
import suneido.util.Util;

public class Deadlock {
	static void check() {
		ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
		long[] ids = mbean.findDeadlockedThreads();
		if (ids == null)
			return;
		Errlog.error("FATAL: deadlock detected");
		for (ThreadInfo info : mbean.getThreadInfo(ids, Integer.MAX_VALUE)) {
			if (info != null)
				Errlog.bare(info.toString().trim());
		}
		System.exit(-1);
	}

	static void cause() {
		Object lock1 = new Object();
		Object lock2 = new Object();
		new Thread(() -> {
			synchronized (lock1) {
				Util.interruptableSleep(100);
				synchronized (lock2) {
				}
			}
		}).start();
		new Thread(() -> {
			synchronized (lock2) {
				Util.interruptableSleep(100);
				synchronized (lock1) {
				}
			}
		}).start();
	}

}
