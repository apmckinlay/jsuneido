/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

interface Locking {

	void readLock(int adr);

	void writeLock(int adr);

	static Locking noLocking = new Locking() {
		@Override
		public void readLock(int adr) {
		}
		@Override
		public void writeLock(int adr) {
		}
	};

}
