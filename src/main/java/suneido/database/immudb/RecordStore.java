/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Used to store key records for TempIndex and Project
 * to avoid per-object overhead for large numbers of keys.
 */
public class RecordStore {
	private final HeapStorage stor = new HeapStorage(16 * 1024);

	public int add(Record rec) {
		int adr = stor.alloc(rec.packSize());
		rec.pack(stor.buffer(adr));
		return adr;
	}

	public Record get(int adr) {
		return new BufRecord(stor.buffer(adr));
	}
}
