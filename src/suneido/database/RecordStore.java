/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.util.ArrayList;

import suneido.intfc.database.Record;

import com.google.common.collect.Lists;

public class RecordStore implements suneido.intfc.database.RecordStore {
	private final ArrayList<Record> recs = Lists.newArrayList();

	@Override
	public int add(Record rec) {
		recs.add(rec);
		return recs.size() - 1;
	}

	@Override
	public Record get(int adr) {
		return recs.get(adr);
	}
}
