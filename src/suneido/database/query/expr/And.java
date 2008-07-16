package suneido.database.query.expr;

public class And extends Multi {

	@Override
	public String toString() {
		return super.toString(" and ");
	}

}
