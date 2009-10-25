package suneido.database.query;

import static suneido.database.Database.theDB;

import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;

public class InsertQuery extends QueryAction {
	private final Transaction tran;
	final private String table;

	InsertQuery(Transaction tran, Query source, String table) {
		super(source);
		this.tran = tran;
		this.table = table;
	}

	@Override
	public String toString() {
		return "INSERT " + source + " INTO " + table;
	}

	@Override
	public int execute() {
		Query q = source.setup();
		Header hdr = q.header();
		List<String> fields = tran.ck_getTable(table).getFields();
		Row row;
		int n = 0;
		for (; null != (row = q.get(Dir.NEXT)); ++n) {
			Record r = new Record();
			for (String f : fields)
				if (f.equals("-"))
					r.addMin();
				else
					r.add(row.getraw(hdr, f));
			theDB.addRecord(tran, table, r);
		}
		return n;
	}

}
