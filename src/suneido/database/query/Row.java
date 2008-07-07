package suneido.database.query;

import suneido.database.Record;

public class Row {
	private final Record[] records;

	Row(Record... records) {
		this.records = records;
	}
}
