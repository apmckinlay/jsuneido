package suneido.database.query;

import suneido.SuRecord;

public class InsertRecord extends QueryAction {
	private final SuRecord record;

	public InsertRecord(Query source, SuRecord record) {
		super(source);
		this.record = record == null ? new SuRecord() : record;
	}

	@Override
	public String toString() {
		return "INSERT " + record + " INTO " + source;
	}

	@Override
	public int execute() {
		source.output(record.toDbRecord(source.header()));
		return 1;
	}

}
