/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

/**
 * Static methods for adding, getting, and removing views.
 */
class Views {
	static final int[] INDEX_COLS = new int[] { 0 };

	static void addView(SchemaTransaction t, String name, String definition) {
		DataRecord r = new RecordBuilder().add(name).add(definition).build();
		t.addRecord(Bootstrap.TN.VIEWS, r);
	}

	/** @return view definition, else null if view not found */
	static String getView(ReadTransaction t, String name) {
		Record rec = getViewRecord(t, name);
		if (rec == null)
			return null;
		return rec.getString(1);
	}

	static boolean dropView(SchemaTransaction t, String name) {
		Record rec = getViewRecord(t, name);
		if (rec == null)
			return false;
		t.removeRecord(Bootstrap.TN.VIEWS, rec);
		return true;
	}

	private static Record getViewRecord(ReadTransaction t, String name) {
		Record key = new RecordBuilder().add(name).arrayRec();
		return t.lookup(Bootstrap.TN.VIEWS, INDEX_COLS, key);
	}

}
