/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Static methods for adding, getting, and removing views.
 */
public class Views {

	public static void addView(UpdateTransaction t, String name, String definition) {
		Record r = new RecordBuilder().add(name).add(definition).build();
		t.addRecord(Bootstrap.TN.VIEWS, r);
	}

	/** @return view definition, else null if view not found */
	public static String getView(ReadTransaction t, String name) {
		Record key = new RecordBuilder().add(name).build();
		Record rec = t.lookup(Bootstrap.TN.VIEWS, "0", key);
		if (rec == null)
			return null;
		return rec.getString(1);
	}

	public static void dropView(UpdateTransaction t, String name) {
		// WARNING: assumes views is only indexed on name
		Record rec = new RecordBuilder().add(name).build();
		t.removeRecord(Bootstrap.TN.VIEWS, rec);
	}

}
