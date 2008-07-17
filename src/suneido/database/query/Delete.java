package suneido.database.query;

import suneido.database.Transaction;


public class Delete extends QueryAction {

	public Delete(Query source) {
		super(source);
	}

	@Override
	public String toString() {
		return "DELETE " + source;
	}

	@Override
	int execute(Transaction tran) {
		// TODO Auto-generated method stub
		return 0;
	}

}
