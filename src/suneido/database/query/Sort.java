/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Util.difference;
import static suneido.util.Util.listToCommas;
import static suneido.util.Verify.verify;

import java.util.List;
import java.util.Set;

import suneido.SuException;
import suneido.database.immudb.Record;
import suneido.database.immudb.Transaction;

public class Sort extends Query1 {
	private final boolean reverse;
	private final List<String> segs;
	boolean indexed;

	Sort(Query source, boolean reverse, List<String> segs) {
		super(source);
		this.reverse = reverse;
		this.segs = segs;
		if (!source.columns().containsAll(segs))
			throw new SuException("sort: nonexistent columns: "
					+ difference(segs, source.columns()));
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(source.toString());
		if (!indexed) {
			s.append(" SORT ");
			if (reverse)
				s.append("REVERSE ");
			s.append(listToCommas(segs));
		}
		return s.toString();
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
	}

	@Override
	public Row get(Dir dir) {
		return source.get(reverse ? (dir == Dir.NEXT ? Dir.PREV : Dir.NEXT) : dir);
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		verify(index.isEmpty());
		Best best = best_prefixed(source.indexes(), segs, needs, is_cursor);
		double cost = source.optimize(segs, needs, firstneeds, is_cursor, false);
		if (! freeze)
			return Math.min(cost, best.cost);
		if (cost <= best.cost)
			return source.optimize(segs, needs, firstneeds, is_cursor, true);
		else if (best.cost < IMPOSSIBLE)
			// NOTE: optimize1 to avoid tempindex
			return source.optimize1(best.index, needs, firstneeds, is_cursor, true);
		else
			return IMPOSSIBLE;
	}

	@Override
	Query addindex(Transaction t) {
		indexed = true;
		return super.addindex(t);
	}

	@Override
	public List<String> ordering() {
		return segs;
	}

}
