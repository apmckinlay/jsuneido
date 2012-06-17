/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import org.junit.Test;

public class NreReadWriteLockTest {
	NreReadWriteLock rwlock = new NreReadWriteLock();

	@Test
	public void lock_unlock_in_different_threads() throws InterruptedException {
		assert rwlock.tryWriteLock();
		Thread thread = new Thread(new WriteUnlock());
		thread.start();
		thread.join();
	}

	@Test
	public void multiple_readers_allowed() {
		assert rwlock.tryWriteLock();
		rwlock.writeUnlock();
		rwlock.readLock();
		assert ! rwlock.tryWriteLock();
		rwlock.readLock();
		assert ! rwlock.tryWriteLock();
		rwlock.readUnlock();
		assert ! rwlock.tryWriteLock();
		rwlock.readUnlock();
		assert rwlock.tryWriteLock();
		rwlock.writeUnlock();
	}

	class WriteUnlock implements Runnable {
		@Override
		public void run() {
			rwlock.writeUnlock();
		}
	}

}
