package suneido.database;

import static suneido.Suneido.verify;

// almost immutable but update will change record
public class TableData {

	private final Record record;
	public final int num;
	public final int nextfield;
	public final int nrecords;
	public final int totalsize;

	public TableData(Record record) {
		this.record = record;
		num = record.getInt(Table.TBLNUM);
		nextfield = record.getInt(Table.NEXTFIELD);
		nrecords = record.getInt(Table.NROWS);
		totalsize = record.getInt(Table.TOTALSIZE);
	}

	private TableData(Record record, int num, int nextfield, int nrecords,
			int totalsize) {
		this.record = record;
		this.num = num;
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
		return new TableData(record, num, nextfield, nrecords + 1,
				totalsize + recSize);
	}

	public TableData without(int recSize) {
		return new TableData(record, num, nextfield, nrecords - 1,
				totalsize - recSize);
	}

	public TableData withReplace(int oldSize, int newSize) {
		return new TableData(record, num, nextfield, nrecords,
				totalsize + newSize - oldSize);
	}

	public TableData withField() {
		return new TableData(record, num, nextfield + 1, nrecords,
 totalsize).update();
	}

	public TableData with(int nextfield, int d_nrecords, int d_totalsize) {
		if (nextfield == this.nextfield && d_nrecords == 0 && d_totalsize == 0)
			return this;
		return new TableData(record, num, nextfield, nrecords + d_nrecords,
				totalsize + d_totalsize).update();
	}

	private TableData update() {
		verify(record.off() != 0);
		record.truncate(Table.NEXTFIELD);
		record.add(nextfield).add(nrecords).add(totalsize);
		return this;
	}

}
