package suneido.database.query;

import java.util.ArrayList;
import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;

public abstract class Query {
	QueryCache cache = new QueryCache();
	boolean willneed_tempindex;
	List<String> tempindex;

	enum Dir {
		NEXT, PREV
	};

	static int update(Transaction tran, Query qq, List<String> c, List<Expr> e) {
		return 0; // TODO
	}

	abstract void setTransaction(Transaction tran);

	// iteration
	abstract Header header();
	abstract List<List<String>> indexes();
	List<String> ordering() { // overridden by QSort
		return new ArrayList<String>();
	}
	abstract void select(List<String> index, Record from, Record to);
	void select(List<String> index, Record key) {
	}
	abstract void rewind();
	abstract Row get(Dir dir);
	List<Fixed> fixed() {
		return new ArrayList<Fixed>();
	}

	// updating
	boolean updateable() {
		return false;
	}
	boolean output(Record record) {
		return false;
	}

	abstract void close();

	@Override
	public abstract String toString();

	abstract List<String> columns();

	abstract List<List<String>> keys();

	Query transform() {
		return this;
	}
	double optimize(List<String> index, List<String> needs, List<String> firstneeds, boolean is_cursor, boolean freeze) {
		return 0; // TODO
	}
	double optimize1(List<String> index, List<String> needs, List<String> firstneeds, boolean is_cursor, boolean freeze) {
		return 0; // TODO
	}
	abstract double optimize2(List<String> index, List<String> needs, List<String> firstneeds, boolean is_cursor, boolean freeze);
	abstract List<String> key_index(List<String> needs);
	// estimated result sizes
	abstract double nrecords();
	abstract int recordsize();
	abstract int columnsize();

	// used to insert TempIndex nodes
	Query addindex() { // redefined by Query1 and Query2
		return null; // TODO
	}
}
