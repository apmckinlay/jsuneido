package suneido.database;

import static suneido.Suneido.verify;

public class TableData {

	private final Record record;
	public int nextfield;
	public int nrecords;
	public int totalsize;

	public TableData(Record record) {
		this.record = record;
		nextfield = record.getInt(Table.NEXTFIELD);
		nrecords = record.getInt(Table.NROWS);
		totalsize = record.getInt(Table.TOTALSIZE);
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

	public void add(int recSize) {
		++nrecords;
		totalsize += recSize;
		update();
	}

	public void remove(int recSize) {
		--nrecords;
		totalsize -= recSize;
		update();
	}

	public void update(int oldSize, int newSize) {
		totalsize += newSize - oldSize;
		update();
	}

	public void update() {
		verify(record.off() != 0);
		record.truncate(Table.NEXTFIELD);
		record.add(nextfield).add(nrecords).add(totalsize);
	}

}
