package suneido.language.builtin;

import suneido.database.query.Query.Dir;
import suneido.language.BuiltinFunction;

public class QueryLast extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		return TransactionInstance.queryOne(null, args, Dir.PREV, false);
	}

}
