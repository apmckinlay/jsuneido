package suneido.database.query;

import static suneido.Util.intersect;
import static suneido.Util.union;

import java.util.List;

import suneido.SuException;
import suneido.database.Record;

public class Product extends Query2 {
	private final boolean first = true;

	Product(Query source1, Query source2) {
		super(source1, source2);
		List<String> dups = intersect(source1.columns(), source2.columns());
		if (!dups.isEmpty())
			throw new SuException("product: common columns not allowed: " + dups);
	}

	@Override
	public String toString() {
		return "(" + source + " TIMES " + source2 + ")";
	}

	@Override
	List<String> columns() {
		return union(source.columns(), source2.columns());
	}

	@Override
	Row get(Dir dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Header header() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<List<String>> indexes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<List<String>> keys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void rewind() {
		// TODO Auto-generated method stub

	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// TODO Auto-generated method stub

	}
}
