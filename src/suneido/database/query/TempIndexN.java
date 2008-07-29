package suneido.database.query;

import static suneido.Util.*;

import java.util.List;

import suneido.database.Record;

public class TempIndexN extends Query1 {
	private final List<String> order;
	private final boolean unique;

	public TempIndexN(Query source, List<String> order, boolean unique) {
		super(source);
		this.order = order;
		this.unique = unique;
	}

	@Override
	public String toString() {
		return source.toString() + " TEMPINDEXN" + listToParens(order)
				+ (unique ? " unique" : "");
	}

	@Override
	List<List<String>> indexes() {
		return list(unique ? order : concat(order, list("-")));
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
