package suneido.database;

public class Table {
	public String name;
	public int num;
	public int nextfield;
	public int nrecords;
	public int totalsize;
	public Columns columns;
	public Indexes indexes;

	public Table(Record table_rec, Columns columns, Indexes indexes) {
		// TODO
		this.columns = columns;
		this.indexes = indexes;
	}

	public void user_trigger(int tran, Record norec, Record r) {
		// TODO Auto-generated method stub

	}

	public void update() {
		// TODO Auto-generated method stub

	}

}
