package suneido.language.builtin;

import suneido.database.query.Query.Dir;
import suneido.language.SuFunction;

public class Query1 extends SuFunction {

	@Override
	public Object call(Object... args) {
		return TransactionInstance.queryOne(null, args, Dir.NEXT, true);
	}

}
