package suneido.database.query;

import static suneido.Util.list;
import static suneido.Util.listToParens;

import java.util.List;

import suneido.database.Record;

public class TempIndex1 extends Query1 {
	private final List<String> order;
	private final boolean unique;

	public TempIndex1(Query source, List<String> order, boolean unique) {
		super(source);
		this.order = order;
		this.unique = unique;
	}

	@Override
	public String toString() {
		return source.toString() + " TEMPINDEX1" + listToParens(order)
				+ (unique ? " unique" : "");
	}

	@Override
	List<List<String>> indexes() {
		return list(order);
	}

	@Override
	Row get(Dir dir) {
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
