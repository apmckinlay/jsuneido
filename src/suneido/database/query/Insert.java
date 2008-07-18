package suneido.database.query;

import suneido.SuContainer;
import suneido.database.Transaction;

public class Insert extends QueryAction {
	private final SuContainer record;

	public Insert(Query source, SuContainer record) {
		super(source);
		this.record = record;
	}

	@Override
	public String toString() {
		return "INSERT " + record + " INTO " + source;
	}

	@Override
	int execute(Transaction tran) {
		source.setTransaction(tran);
		source.output(record.toDbRecord(source.header()));
		return 1;
	}

}
