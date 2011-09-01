package suneido.database.query;

import static suneido.util.Util.difference;
import static suneido.util.Util.listToCommas;
import static suneido.util.Verify.verify;

import java.util.List;
import java.util.Set;

import suneido.SuException;
import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

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
		String s = source.toString();
		if (!indexed) {
			s += " SORT ";
			if (reverse)
				s += "REVERSE ";
			s += listToCommas(segs);
		}
		return s;
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
		// look for index containing requested index as prefix (using fixed)
		Best best = new Best();
		best.index = segs;
		best.cost = source.optimize(segs, needs, firstneeds, is_cursor, false);
		best_prefixed(source.indexes(), segs, needs, is_cursor, best);

		if (!freeze)
			return best.cost;
		if (best.index.equals(segs))
			return source.optimize(segs, needs, firstneeds, is_cursor, true);
		else
			// NOTE: optimize1 to avoid tempindex
			return source.optimize1(best.index, needs, firstneeds, is_cursor, true);
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
