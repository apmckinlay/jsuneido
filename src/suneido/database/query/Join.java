package suneido.database.query;

import static suneido.Util.intersect;
import static suneido.Util.listToParens;
import static suneido.Util.set_eq;

import java.util.List;

import suneido.SuException;
import suneido.database.Record;

public class Join extends Query2 {
	List<String> joincols;
	private Type type;

	enum Type {
		NONE(""), ONE_ONE("1:1"), ONE_N("1:n"), N_ONE("n:1"), N_N("n:n");
		public String name;
		Type(String name) {
			this.name = name;
		}
	}

	Join(Query source1, Query source2, List<String> by) {
		super(source1, source2);
		joincols = intersect(source.columns(), source2.columns());
		if (joincols.isEmpty())
			throw new SuException("join: common columns required");
		if (by != null && !set_eq(by, joincols))
			throw new SuException("join: by does not match common columns: "
					+ joincols);

		// find out if joincols include keys
		boolean k1 = containsKey(source.keys());
		boolean k2 = containsKey(source2.keys());
		if (k1 && k2)
			type = Type.ONE_ONE;
		else if (k1)
			type = Type.ONE_N;
		else if (k2)
			type = Type.N_ONE;
		else
			type = Type.N_N;
	}

	private boolean containsKey(List<List<String>> keys) {
		for (List<String> k : keys)
			if (joincols.containsAll(k))
				return true;
		return false;
	}

	@Override
	public String toString() {
		return "(" + source + ") " + name() + " " + type.name + " on "
				+ listToParens(joincols)
				+ " (" + source2 + ")";
	}

	protected String name() {
		return "JOIN";
	}

	protected boolean can_swap() {
		return true;
	}

	@Override
	List<String> columns() {
		// TODO Auto-generated method stub
		return null;
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
