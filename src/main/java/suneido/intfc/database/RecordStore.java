/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

/**
 * Used to store key records for TempIndex and Project
 * to avoid per-object overhead for large numbers of keys.
 */
public interface RecordStore {

	int add(Record rec);

	Record get(int adr);

}
