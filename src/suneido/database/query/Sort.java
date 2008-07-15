package suneido.database.query;

import static suneido.Suneido.verify;
import static suneido.Util.listToParens;

import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;

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
			s += listToParens(segs);
		}
		return s;
	}

	@Override
	List<String> columns() {
		return source.columns();
	}

	@Override
	List<List<String>> keys() {
		return source.keys();
	}

	@Override
	List<List<String>> indexes() {
		return source.indexes();
	}

	@Override
	Header header() {
		return source.header();
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
	}

	@Override
	void rewind() {
		source.rewind();
	}

	@Override
	Row get(Dir dir) {
		return source.get(reverse ? (dir == Dir.NEXT ? Dir.PREV : Dir.NEXT)
				: dir);
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		verify(index.isEmpty());
		// look for index containing requested index as prefix (using fixed)
		List<String> best_index = segs;
		double best_cost = source.optimize(segs, needs, firstneeds, is_cursor,
				false);
		best_prefixed(source.indexes(), segs, needs, is_cursor, best_index,
				best_cost);

		if (!freeze)
			return best_cost;
		if (best_index == segs)
			return source.optimize(segs, needs, firstneeds, is_cursor, true);
		else
			// NOTE: optimize1 to avoid tempindex
			return source.optimize1(best_index, needs, firstneeds, is_cursor,
					true);
	}

	@Override
	Query addindex() {
		indexed = true;
		return super.addindex();
	}

	@Override
	List<String> ordering() {
		return segs;
	}

	@Override
	boolean output(Record r) {
		return source.output(r);
	}

	@Override
	int columnsize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	double nrecords() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	int recordsize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	void setTransaction(Transaction tran) {
		// TODO Auto-generated method stub

	}

}
