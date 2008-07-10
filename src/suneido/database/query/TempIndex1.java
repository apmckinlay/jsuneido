package suneido.database.query;

import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;

public class TempIndex1 extends Query {

	public TempIndex1(Query query, List<String> tempindex, boolean unique) {
		// TODO Auto-generated constructor stub
	}

	@Override
	void close() {
		// TODO Auto-generated method stub

	}

	@Override
	List<String> columns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	int columnsize() {
		// TODO Auto-generated method stub
		return 0;
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
	List<String> key_index(List<String> needs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<List<String>> keys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	double nrecords() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	int recordsize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	void rewind() {
		// TODO Auto-generated method stub

	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// TODO Auto-generated method stub

	}

	@Override
	void setTransaction(Transaction tran) {
		// TODO Auto-generated method stub

	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
