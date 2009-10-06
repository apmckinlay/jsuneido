package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.SuException;
import suneido.language.*;

/**
 * this is the value of the global "Transaction" i.e. the class
 * instances are {@link TransactionInstance}
 *
 * @author Andrew McKinlay
 */
public class Transaction extends BuiltinClass {

	@Override
	public TransactionInstance newInstance(Object[] args) {
		return new TransactionInstance(args);
	}

	private static final FunctionSpec tranFS =
			new FunctionSpec(array("read", "update", "block"), false, false,
					false);

	@Override
	public Object call(Object... args) {
		TransactionInstance t = newInstance(args);
		args = Args.massage(tranFS, args);
		if (args[2] == Boolean.FALSE)
			return t;
		try {
			Object result = Ops.call(args[2], t);
			t.ck_complete();
			return result;
		} catch (BlockReturnException bre) {
			t.ck_complete();
			throw bre;
		} catch (SuException e) {
			//e.printStackTrace();
			t.abort();
			throw e;
		}
	}

}
