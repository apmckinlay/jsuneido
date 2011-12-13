/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

/**
 * Static methods for adding, getting, and removing views.
 */
class Views {

	static void addView(UpdateTransaction t, String name, String definition) {
		Record r = new RecordBuilder().add(name).add(definition).build();
		t.addRecord(Bootstrap.TN.VIEWS, r);
	}

	/** @return view definition, else null if view not found */
	static String getView(ReadTransaction t, String name) {
		Record key = new RecordBuilder().add(name).build();
		Record rec = t.lookup(Bootstrap.TN.VIEWS, new int[] { 0 }, key);
		if (rec == null)
			return null;
		return rec.getString(1);
	}

	static void dropView(UpdateTransaction t, String name) {
		// WARNING: assumes views is only indexed on name
		Record rec = new RecordBuilder().add(name).build();
		t.removeRecord(Bootstrap.TN.VIEWS, rec);
	}

}
