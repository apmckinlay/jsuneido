package suneido.database.query;

import static suneido.Suneido.dbpkg;

import java.util.List;

import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

public class InsertQuery extends QueryAction {
	private final Transaction tran;
	private final String table;

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
		Query q = source.setup(tran);
		Header hdr = q.header();
		List<String> fields = tran.ck_getTable(table).getFields();
		Row row;
		int n = 0;
		for (; null != (row = q.get(Dir.NEXT)); ++n) {
			Record r = dbpkg.record();
			for (String f : fields)
				if (f.equals("-"))
					r.addMin();
				else
					r.add(row.getraw(hdr, f));
			tran.addRecord(table, r);
		}
		return n;
	}

}
