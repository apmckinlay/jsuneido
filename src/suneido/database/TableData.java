package suneido.database;

import static suneido.SuException.verify;

/**
 * Contains the changing data for a table: nextfield, nrecords, totalsize.
 * tables and indexes records are the only records that are updated "in place"
 * rather than new versions being added to the end of the database file.
 * Almost immutable but update will change record.
 *
 * @author Andrew McKinlay
 */
public class TableData {

	public final Record record;
	public final int tblnum;
	public final int nextfield;
	public final int nrecords;
	public final int totalsize;

	public TableData(Record record) {
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

	public int nrecords() {
		return nrecords;
	}

	public int totalsize() {
		return totalsize;
	}

	public boolean isEmpty() {
		return nrecords == 0;
	}

	public TableData with(int recSize) {
		return new TableData(record, tblnum, nextfield, nrecords + 1,
				totalsize + recSize);
	}

	public TableData without(int recSize) {
		return new TableData(record, tblnum, nextfield, nrecords - 1,
				totalsize - recSize);
	}

	public TableData withReplace(int oldSize, int newSize) {
		return new TableData(record, tblnum, nextfield, nrecords,
				totalsize + newSize - oldSize);
	}

	public TableData withField() {
		return new TableData(record, tblnum, nextfield + 1, nrecords, totalsize);
	}

	/** NOTE: this method updates */
	public TableData with(int nextfield, int d_nrecords, int d_totalsize) {
		if (nextfield == this.nextfield && d_nrecords == 0 && d_totalsize == 0)
			return this;
		TableData td = new TableData(record, tblnum, nextfield, nrecords + d_nrecords,
				totalsize + d_totalsize);
		td.update();
		return td;
	}

	private void update() {
		verify(record.off() != 0);
		record.truncate(Table.NEXTFIELD);
		record.add(nextfield).add(nrecords).add(totalsize);
	}

}
