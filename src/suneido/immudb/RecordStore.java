/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.intfc.database.Record;

class RecordStore implements suneido.intfc.database.RecordStore {
	private final HeapStorage stor = new HeapStorage(16 * 1024);

	@Override
	public int add(Record rec) {
		int adr = stor.alloc(rec.packSize());
		rec.pack(stor.buffer(adr));
		return adr;
	}

	@Override
	public Record get(int adr) {
		return new BufRecord(stor.buffer(adr));
	}
}
