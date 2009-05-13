package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.SuException;
import suneido.language.*;

public class Transaction extends BuiltinClass {

	@Override
	public SuTransaction newInstance(Object[] args) {
		return new SuTransaction(args);
	}

	private static final FunctionSpec tranFS =
			new FunctionSpec(array("read", "update", "block"), false, false,
					false);

	@Override
	public Object call(Object... args) {
		SuTransaction t = newInstance(args);
		args = Args.massage(tranFS, args);
		if (args[2] == Boolean.FALSE)
			return t;
		try {
			Object result = Ops.call(args[2], t);
			t.ck_complete();
			return result;
		} catch (BlockReturnException bre) {
			t.ck_complete();
			return bre.returnValue;
		} catch (SuException e) {
			t.abort();
			throw e;
		}
	}



}
