/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Confirming that if you try to lock/unlock ReentrantReadWriteLock
 * in different threads you get IllegalMonitorStateException.
 * Same for readLock and writeLock.
 */
public class TestReadWriteLock {
	static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public static void main(String[] args) {
		lock.readLock().lock();
		new Thread(new Runnable() {
			@Override
			public void run() {
				lock.readLock().unlock();
			} }).start();
	}

}
