/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

/**
 * Static methods for adding, getting, and removing views.
 */
class Views {
	static final int[] INDEX_COLS = new int[] { 0 };

	static void addView(ExclusiveTransaction2 t, String name, String definition) {
		Record r = new RecordBuilder().add(name).add(definition).build();
		t.addRecord(Bootstrap.TN.VIEWS, r);
	}

	/** @return view definition, else null if view not found */
	static String getView(ReadTransaction2 t, String name) {
		Record rec = getViewRecord(t, name);
		if (rec == null)
			return null;
		return rec.getString(1);
	}

	static boolean dropView(ExclusiveTransaction2 t, String name) {
		// WARNING: assumes views is only indexed on name
		Record rec = getViewRecord(t, name);
		if (rec == null)
			return false;
		t.removeRecord(Bootstrap.TN.VIEWS, rec);
		return true;
	}

	private static Record getViewRecord(ReadTransaction2 t, String name) {
		Record key = new RecordBuilder().add(name).build();
		return t.lookup(Bootstrap.TN.VIEWS, INDEX_COLS, key);
	}

}
