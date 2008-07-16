package suneido.database.query;

import java.util.List;

import suneido.database.Record;
import suneido.database.query.expr.And;
import suneido.database.query.expr.Expr;

public class Select extends Query1 {
	private final Expr expr;

	public Select(Query source, Expr expr) {
		super(source);
		// expr = expr.fold();
		if (!(expr instanceof And))
			expr = new And().add(expr);
		// if (!source.columns().containsAll(expr.fields()))
		// throw new SuException("select: nonexistent columns: "
		// + listToParens(difference(expr.fields(), source.columns())));
		this.expr = expr;
	}

	@Override
	public String toString() {
		return source + " WHERE " + expr;
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
