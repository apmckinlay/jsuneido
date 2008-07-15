package suneido.database.query;

import java.util.List;

import suneido.database.Record;
import suneido.database.Transaction;

public class History extends Query {
	String tablename;
	int tblnum;
	// Mmfile::iterator iter;
	boolean rewound;
	int id;
	int ic;

	History(String tablename) {
		this.tablename = tablename;
	}

	@Override
	public String toString() {
		return "history(" + tablename + ")";
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

}
