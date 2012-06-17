/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.concurrent.Semaphore;

/**
 * Non-reentrant read-write lock.
 * i.e. Unlock can be done in a different thread than lock.
 * (unlike ReentrantReadWriteLock)
 */
public class NreReadWriteLock {
	private static final int MAXPERMITS = Integer.MAX_VALUE;
	private final Semaphore sem = new Semaphore(MAXPERMITS, true);

	public void readLock() {
		sem.acquireUninterruptibly(1);
	}

	public void readUnlock() {
		sem.release(1);
	}

	public boolean tryWriteLock() {
		return sem.tryAcquire(MAXPERMITS);
	}

	public void writeUnlock() {
		sem.release(MAXPERMITS);
	}

}
