package suneido.database.query.expr;

public class Or extends Multi {

	@Override
	public String toString() {
		return super.toString(" or ");
	}

}
