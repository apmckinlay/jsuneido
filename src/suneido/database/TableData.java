/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

/**
 * Contains the mutable data for a table: nextfield, nrecords, totalsize. tables
 * and indexes records are the only records that are updated "in place" rather
 * than new versions being added to the end of the database file. Almost
 * immutable but update will change record. The schema information is in
 * {@link Table}
 */
// used by query Table
public class TableData {
	private final Record record;
	final int tblnum;
	final int nextfield;
	public final int nrecords;
	public final int totalsize; // TODO should be long

	TableData(Record record) {
		this.record = record;
		tblnum = record.getInt(Table.TBLNUM);
		nextfield = record.getInt(Table.NEXTFIELD);
		nrecords = record.getInt(Table.NROWS);
		totalsize = record.getInt(Table.TOTALSIZE);
	}

	private TableData(Record record, int num, int nextfield, int nrecords,
			int totalsize) {
		this.record = record;
		this.tblnum = num;
		this.nextfield = nextfield;
		this.nrecords = nrecords;
		this.totalsize = totalsize;
	}

	TableData with(int recSize) {
		return new TableData(record, tblnum, nextfield, nrecords + 1, totalsize
				+ recSize);
	}

	TableData without(int recSize) {
		return new TableData(record, tblnum, nextfield, nrecords - 1, totalsize
				- recSize);
	}

	TableData withField() {
		return new TableData(record, tblnum, nextfield + 1, nrecords, totalsize);
	}

	/** NOTE: this method updates */
	TableData with(int nextfield, int d_nrecords, int d_totalsize) {
		if (nextfield == this.nextfield && d_nrecords == 0 && d_totalsize == 0)
			return this;
		TableData td =
				new TableData(record, tblnum, Math.max(this.nextfield,
						nextfield), nrecords + d_nrecords, totalsize
						+ d_totalsize);
		td.update();
		return td;
	}

	private void update() {
		Table.update(record, nextfield, nrecords, totalsize);
	}

}
