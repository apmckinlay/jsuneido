/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import java.util.List;

import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;
import suneido.database.query.Query.Dir;

public class SummarizeStrategyIdx extends SummarizeStrategy {
	List<String> selIndex;

	public SummarizeStrategyIdx(Summarize q) {
		super(q);
	}

	@Override
	Row get(Dir dir, boolean rewound) {
		if (! rewound)
			return null;
		dir = q.funcs.get(0).toLowerCase().equals("min") ? Dir.NEXT : Dir.PREV;
		Row row = source.get(dir);
		if (row == null)
			return null;
		if (selIndex != null) {
			Record key = row.project(q.getHdr(), selIndex);
			if (key.compareTo(sel.org) < 0 || key.compareTo(sel.end) > 0)
				return null;
		}
		RecordBuilder rb = new RecordBuilder();
		rb.add(row.getraw(q.getHdr(), q.on.get(0)));
		Row result = new Row(Dbpkg.MIN_RECORD, rb.build());
		if (q.wholeRecord)
			result = new Row(row, result);
		return result;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		selIndex = index;
		q.source.rewind();
	}

}
