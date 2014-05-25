/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.Suneido.dbpkg;
import static suneido.util.Util.startsWith;

import java.util.List;

import suneido.SuException;
import suneido.database.query.Query.Dir;
import suneido.database.query.Summarize.Summary;
import suneido.intfc.database.Record;

public class SummarizeStrategySeq extends SummarizeStrategy {
	List<Summary> sums;
	Row nextrow;
	Row currow;

	SummarizeStrategySeq(Summarize source) {
		super(source);
		sums = funcSums();
	}

	@Override
	Row get(Dir dir, boolean rewound) {
		if (rewound) {
			curdir = dir;
			currow = new Row();
			nextrow = source.get(dir);
			if (nextrow == null)
				return null;
		}

		// if direction changes, have to skip over previous result
		if (dir != curdir) {
			if (nextrow == null)
				source.rewind();
			do
				nextrow = source.get(dir);
				while (nextrow != null && equal());
			curdir = dir;
		}

		if (nextrow == null)
			return null;

		currow = nextrow;
		initSums(sums);
		do {
			if (nextrow == null)
				break ;
			for (int i = 0; i < sums.size(); ++i)
				sums.get(i).add(nextrow.getval(q.getHdr(), q.on.get(i)));
			nextrow = source.get(dir);
		} while (equal());
		// output after reading a group

		Record byRec = currow.project(q.getHdr(), q.by);
		return makeRow(byRec, sums);
	}

	private boolean equal() {
		if (nextrow == null)
			return false;
		for (String f : q.by)
			if (!currow.getval(q.getHdr(), f).equals(nextrow.getval(q.getHdr(), f)))
				return false;
		return true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// because of fixed, this index may not be the same as the source index (via)
		if (startsWith(q.via, index) ||
				(from.equals(dbpkg.minRecord()) && to.equals(dbpkg.maxRecord())))
			source.select(q.via, from, to);
		else
			throw new SuException(
					"Summarize SeqStrategy by " + q.via
					+ " doesn't handle select(" + index + " from " + from
					+ " to " + to + ")");
	}
}
