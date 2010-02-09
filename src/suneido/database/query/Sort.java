package suneido.database.query;

import static suneido.SuException.verify;
import static suneido.util.Util.listToCommas;

import java.util.List;

import suneido.database.Record;

public class Sort extends Query1 {
	private final boolean reverse;
	private final List<String> segs;
	boolean indexed;

	Sort(Query source, boolean reverse, List<String> segs) {
		super(source);
		this.reverse = reverse;
		this.segs = segs;
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
		return source.get(reverse ? (dir == Dir.NEXT ? Dir.PREV : Dir.NEXT)
				: dir);
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
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
	Query addindex() {
		indexed = true;
		return super.addindex();
	}

	@Override
	public List<String> ordering() {
		return segs;
	}

}
