package suneido.database.query;

import suneido.SuException;
import suneido.intfc.database.Transaction;

public class Delete extends QueryAction {
	private final Transaction tran;

	public Delete(Transaction tran, Query source) {
		super(source);
		this.tran = tran;
	}

	@Override
	public String toString() {
		return "DELETE " + source;
	}

	@Override
	public int execute() {
		Query q = source.setup(tran);
		if (!q.updateable())
			throw new SuException("delete: query not updateable");
		Row row;
		int n = 0;
		for (; null != (row = q.get(Dir.NEXT)); ++n)
			tran.removeRecord(q.tblnum(), row.firstData());
		return n;
	}

}
