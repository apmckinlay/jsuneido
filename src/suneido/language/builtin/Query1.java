package suneido.language.builtin;

import static suneido.database.server.Command.theDbms;
import suneido.database.query.Query.Dir;
import suneido.database.server.DbmsTran;
import suneido.language.SuFunction;

public class Query1 extends SuFunction {

	@Override
	public Object call(Object... args) {
		DbmsTran t = theDbms.transaction(false);
		try {
			return SuTransaction.queryOne(t, args, Dir.NEXT, true);
		} finally {
			t.complete();
		}
	}

}
