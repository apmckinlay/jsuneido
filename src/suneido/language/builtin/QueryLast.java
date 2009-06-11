package suneido.language.builtin;

import suneido.database.query.Query.Dir;
import suneido.language.SuFunction;

public class QueryLast extends SuFunction {

	@Override
	public Object call(Object... args) {
		return TransactionInstance.queryOne(null, args, Dir.PREV, false);
	}

}
