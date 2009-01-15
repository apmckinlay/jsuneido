package suneido.database.query;

import suneido.SuRecord;
import suneido.database.Transaction;

public class InsertRecord extends QueryAction {
	private final SuRecord record;

	public InsertRecord(Query source, SuRecord record) {
		super(source);
		this.record = record;
	}

	@Override
	public String toString() {
		return "INSERT " + record + " INTO " + source;
	}

	@Override
	public int execute(Transaction tran) {
		source.setTransaction(tran);
		source.output(record.toDbRecord(source.header()));
		return 1;
	}

}
