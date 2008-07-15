package suneido.database.query;

import static suneido.Util.difference;
import static suneido.Util.listToParens;

import java.util.List;

import suneido.SuException;
import suneido.database.Record;

public class Summarize extends Query1 {
	private final List<String> by;
	private final List<String> cols;
	private final List<String> funcs;
	private final List<String> on;
	private List<String> via;
	Strategy strategy = Strategy.NONE;
	enum Strategy {
		NONE(""), COPY("-COPY"), SEQUENTIAL("-SEQ");
		public String name;
		Strategy(String name) {
			this.name = name;
		}
	};

	Summarize(Query source, List<String> by, List<String> cols,
			List<String> funcs, List<String> on) {
		super(source);
		this.by = by;
		this.cols = cols;
		this.funcs = funcs;
		this.on = on;
		if (!source.columns().containsAll(by))
			throw new SuException(
					"summarize: nonexistent columns: "
					+ difference(by, source.columns()));

		if (by.isEmpty() || by_contains_key())
			strategy = Strategy.COPY;
	}

	boolean by_contains_key() {
		// check if project contain candidate key
		for (List<String> k : source.keys())
			if (by.containsAll(k))
				return true;
		return false;
	}

	@Override
	public String toString() {
		String s = source + " SUMMARIZE" + strategy.name + " ";
		if (via != null)
			s += "^" + listToParens(via) + " ";
		if (! by.isEmpty())
			s += listToParens(by) + " ";
		for (int i = 0; i < cols.size(); ++i) {
			if (cols.get(i) != null)
				s += cols.get(i) + " = ";
			s += funcs.get(i);
			if (on.get(i) != null)
				s += " " + on.get(i);
			s += ", ";
		}
		return s.substring(0, s.length() - 2);
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
