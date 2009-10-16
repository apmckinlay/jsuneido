package suneido.database.query;

import static suneido.database.Database.theDB;
import suneido.SuException;
import suneido.database.Transaction;


public class Delete extends QueryAction {

	public Delete(Query source) {
		super(source);
	}

	@Override
	public String toString() {
		return "DELETE " + source;
	}

	@Override
	public int execute(Transaction tran) {
		Query q = source.setup(tran);
		if (!q.updateable())
			throw new SuException("delete: query not updateable");
		Row row;
		int n = 0;
		for (; null != (row = q.get(Dir.NEXT)); ++n)
			theDB.removeRecord(tran, row.getFirstData().off());
		return n;
	}

}
