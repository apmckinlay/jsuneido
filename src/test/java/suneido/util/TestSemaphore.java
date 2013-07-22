/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.concurrent.Semaphore;

public class TestSemaphore {
	static Semaphore sem = new Semaphore(Integer.MAX_VALUE, true);

	public static void main(String[] args) {
		sem.acquireUninterruptibly();
		new Thread(new Runnable() {
			@Override
			public void run() {
				sem.release();
				System.out.println("worked!");
			} }).start();
	}

}
