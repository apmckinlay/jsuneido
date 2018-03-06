/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import java.util.ArrayList;
import java.util.List;

import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;
import suneido.database.query.Query.Dir;
import suneido.database.query.Summarize.Summary;

public abstract class SummarizeStrategy {
	Summarize q;
	Query source;
	Dir curdir;
	Keyrange sel = new Keyrange();

	public SummarizeStrategy(Summarize q) {
		this.q = q;
		source = q.source;
	}

	List<Summary> funcSums() {
		List<Summary> sums = new ArrayList<>();
		for (String f : q.funcs)
			sums.add(Summary.valueOf(f));
		return sums;
	}

	void initSums(List<Summary> sums) {
		for (Summary s : sums)
			s.init();
	}

	Row makeRow(Record r, List<Summary> sums) {
		RecordBuilder rb = new RecordBuilder();
		rb.addAll(r);
		for (Summary s : sums)
			rb.add(s.result());
		return new Row(Dbpkg.MIN_RECORD, rb.build());
	}

	abstract Row get(Dir dir, boolean rewound);

	abstract void select(List<String> index, Record from, Record to);
}
